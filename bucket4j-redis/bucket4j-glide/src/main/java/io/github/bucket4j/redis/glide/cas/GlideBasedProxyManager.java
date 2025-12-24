package io.github.bucket4j.redis.glide.cas;

import glide.api.BaseClient;
import glide.api.models.GlideString;
import glide.api.models.Script;
import glide.api.models.commands.ScriptOptionsGlideString;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AbstractCompareAndSwapBasedProxyManager;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AsyncCompareAndSwapOperation;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.CompareAndSwapOperation;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.serialization.Mapper;
import io.github.bucket4j.redis.consts.LuaScripts;
import io.github.bucket4j.redis.glide.Bucket4jGlide;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static glide.api.models.GlideString.gs;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

public class GlideBasedProxyManager<K> extends AbstractCompareAndSwapBasedProxyManager<K> {
    private final BaseClient client;
    private final Mapper<K> keyMapper;
    private final ExpirationAfterWriteStrategy expirationStrategy;

    public GlideBasedProxyManager(Bucket4jGlide.GlideBasedProxyManagerBuilder<K> builder) {
        super(builder.getClientSideConfig());
        this.client = builder.getClient();
        this.keyMapper = builder.getKeyMapper();
        this.expirationStrategy = builder.getExpirationAfterWrite().orElse(ExpirationAfterWriteStrategy.none());
    }

    @Override
    protected CompareAndSwapOperation beginCompareAndSwapOperation(K key) {
        return new CompareAndSwapOperation() {
            @Override
            public Optional<byte[]> getStateData(Optional<Long> timeoutNanos) {
                return Optional.ofNullable(getWithTimeout(timeoutNanos, client.get(toGlideString(key))))
                        .map(GlideString::getBytes);
            }

            @Override
            public boolean compareAndSwap(byte[] originalData, byte[] newData, RemoteBucketState newState, Optional<Long> timeoutNanos) {
                Object result = getWithTimeout(timeoutNanos, compareAndSwapFuture(toGlideString(key), originalData, newData, newState));
                return result != null && !result.equals(0L);
            }
        };
    }

    @Override
    protected AsyncCompareAndSwapOperation beginAsyncCompareAndSwapOperation(K key) {
        return new AsyncCompareAndSwapOperation() {
            @Override
            public CompletableFuture<Optional<byte[]>> getStateData(Optional<Long> timeoutNanos) {
                return getFutureWithTimeout(timeoutNanos, client.get(GlideString.of(toGlideString(key))))
                        .thenApply(result -> Optional.ofNullable(result).map(GlideString::getBytes));
            }

            @Override
            public CompletableFuture<Boolean> compareAndSwap(byte[] originalData, byte[] newData, RemoteBucketState newState, Optional<Long> timeoutNanos) {
                return getFutureWithTimeout(timeoutNanos, compareAndSwapFuture(toGlideString(key), originalData, newData, newState))
                        .thenApply(result -> result != null && !result.equals(0L));
            }
        };
    }

    private CompletableFuture<Object> compareAndSwapFuture(GlideString stringKey, byte[] originalData, byte[] newData, RemoteBucketState newState) {
        long ttlMillis = expirationStrategy.calculateTimeToLiveMillis(newState, currentTimeNanos());
        if (ttlMillis > 0) {
            if (originalData == null) {
                ScriptOptionsGlideString options = ScriptOptionsGlideString.builder()
                        .key(stringKey)
                        .arg(gs(newData))
                        .arg(gs(encodeLong(ttlMillis)))
                        .build();
                return eval(LuaScripts.SCRIPT_SET_NX_PX, options);
            } else {
                ScriptOptionsGlideString options = ScriptOptionsGlideString.builder()
                        .key(stringKey)
                        .arg(gs(originalData))
                        .arg(gs(newData))
                        .arg(gs(encodeLong(ttlMillis)))
                        .build();
                return eval(LuaScripts.SCRIPT_COMPARE_AND_SWAP_PX, options);
            }
        } else {
            if (originalData == null) {
                ScriptOptionsGlideString options = ScriptOptionsGlideString.builder()
                        .key(stringKey)
                        .arg(gs(newData))
                        .build();
                return eval(LuaScripts.SCRIPT_SET_NX, options);
            } else {
                ScriptOptionsGlideString options = ScriptOptionsGlideString.builder()
                        .key(stringKey)
                        .arg(gs(originalData))
                        .arg(gs(newData))
                        .build();
                return eval(LuaScripts.SCRIPT_COMPARE_AND_SWAP, options);
            }
        }
    }

    private CompletableFuture<Object> eval(String script, ScriptOptionsGlideString options) {
        Script luaScript = new Script(script, false);
        return client.invokeScript(luaScript, options).whenComplete((value, exception) -> {
            try {
                luaScript.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private <T> CompletableFuture<T> getFutureWithTimeout(Optional<Long> timeoutNanos, CompletableFuture<T> future) {
        return timeoutNanos.map(nanos -> future.orTimeout(nanos, NANOSECONDS)).orElse(future);
    }

    private <T> T getWithTimeout(Optional<Long> timeoutNanos, CompletableFuture<T> future) {
        try {
            if (timeoutNanos.isEmpty()) {
                return future.get();
            }
            return future.get(timeoutNanos.get(), NANOSECONDS);
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected CompletableFuture<Void> removeAsync(K key) {
        return client.del(new GlideString[]{toGlideString(key)}).thenApply(l -> null);
    }

    @Override
    public void removeProxy(K key) {
        getWithTimeout(Optional.empty(), client.del(new GlideString[]{toGlideString(key)}));
    }

    @Override
    public boolean isAsyncModeSupported() {
        return true;
    }

    private byte[] encodeLong(Long value) {
        return ("" + value).getBytes(StandardCharsets.UTF_8);
    }

    private GlideString toGlideString(K key) {
        return GlideString.of(keyMapper.toBytes(key));
    }

    @Override
    public boolean isExpireAfterWriteSupported() {
        return true;
    }
}
