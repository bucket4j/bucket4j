/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2025 Vladimir Bukhtoyarov
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package io.github.bucket4j.couchbase;

import com.couchbase.client.core.error.CasMismatchException;
import com.couchbase.client.core.error.DocumentExistsException;
import com.couchbase.client.core.error.DocumentNotFoundException;
import com.couchbase.client.java.AsyncCollection;
import com.couchbase.client.java.AsyncUtils;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.codec.RawBinaryTranscoder;
import com.couchbase.client.java.kv.GetResult;
import com.couchbase.client.java.kv.GetOptions;
import com.couchbase.client.java.kv.InsertOptions;
import com.couchbase.client.java.kv.MutationResult;
import com.couchbase.client.java.kv.RemoveOptions;
import com.couchbase.client.java.kv.ReplaceOptions;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.expiration.NoneExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AbstractCompareAndSwapBasedProxyManager;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AsyncCompareAndSwapOperation;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.CompareAndSwapOperation;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.serialization.Mapper;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static com.couchbase.client.java.kv.GetOptions.getOptions;
import static com.couchbase.client.java.kv.InsertOptions.insertOptions;
import static com.couchbase.client.java.kv.RemoveOptions.removeOptions;
import static com.couchbase.client.java.kv.ReplaceOptions.replaceOptions;

/**
 * Compare-and-swap-based proxy manager for Couchbase that uses KV operations from the Couchbase Java SDK.
 * Both blocking and asynchronous execution modes share the same CAS and TTL semantics.
 *
 * @param <K> type of primary key
 */
public class CouchbaseCompareAndSwapBasedProxyManager<K> extends AbstractCompareAndSwapBasedProxyManager<K> {

    private static final RawBinaryTranscoder RAW_BINARY_TRANSCODER = RawBinaryTranscoder.INSTANCE;

    private final Collection collection;
    private final AsyncCollection asyncCollection;
    private final Mapper<K> keyMapper;
    private final ExpirationAfterWriteStrategy expirationStrategy;

    protected CouchbaseCompareAndSwapBasedProxyManager(Bucket4jCouchbase.CouchbaseCompareAndSwapBasedProxyManagerBuilder<K> builder) {
        super(builder.getClientSideConfig());
        this.collection = builder.getCollection();
        this.asyncCollection = builder.getAsyncCollection();
        this.keyMapper = builder.getKeyMapper();
        this.expirationStrategy = builder.getClientSideConfig()
            .getExpirationAfterWriteStrategy()
            .orElseGet(ExpirationAfterWriteStrategy::none);
    }

    @Override
    protected CompareAndSwapOperation beginCompareAndSwapOperation(K key) {
        return new CompareAndSwapOperation() {
            private final String documentId = keyMapper.toString(key);
            private long currentCas;

            @Override
            public Optional<byte[]> getStateData(Optional<Long> timeoutNanos) {
                try {
                    GetResult persistedState = executeGet(documentId, timeoutNanos);
                    currentCas = persistedState.cas();
                    return Optional.of(persistedState.contentAsBytes());
                } catch (DocumentNotFoundException e) {
                    currentCas = 0L;
                    return Optional.empty();
                }
            }

            @Override
            public boolean compareAndSwap(byte[] originalData, byte[] newData, RemoteBucketState newState, Optional<Long> timeoutNanos) {
                if (originalData == null) {
                    try {
                        executeInsert(documentId, newData, newState, timeoutNanos);
                        return true;
                    } catch (DocumentExistsException e) {
                        return false;
                    }
                }

                try {
                    MutationResult result = executeReplace(documentId, newData, newState, currentCas, timeoutNanos);
                    currentCas = result.cas();
                    return true;
                } catch (DocumentNotFoundException | CasMismatchException e) {
                    return false;
                }
            }
        };
    }

    @Override
    protected AsyncCompareAndSwapOperation beginAsyncCompareAndSwapOperation(K key) {
        return new AsyncCompareAndSwapOperation() {
            private final String documentId = keyMapper.toString(key);
            private volatile long currentCas;

            @Override
            public CompletableFuture<Optional<byte[]>> getStateData(Optional<Long> timeoutNanos) {
                return asyncCollection.get(documentId, buildGetOptions(timeoutNanos))
                    .thenApply(persistedState -> {
                        currentCas = persistedState.cas();
                        return Optional.of(persistedState.contentAsBytes());
                    })
                    .handle((state, error) -> {
                        if (error == null) {
                            return state;
                        }
                        Throwable cause = unwrap(error);
                        if (cause instanceof DocumentNotFoundException) {
                            currentCas = 0L;
                            return Optional.<byte[]>empty();
                        }
                        throw rethrow(cause);
                    });
            }

            @Override
            public CompletableFuture<Boolean> compareAndSwap(byte[] originalData, byte[] newData, RemoteBucketState newState, Optional<Long> timeoutNanos) {
                if (originalData == null) {
                    return asyncCollection.insert(documentId, newData, buildInsertOptions(newState, timeoutNanos))
                        .thenApply(result -> true)
                        .handle((inserted, error) -> {
                            if (error == null) {
                                return inserted;
                            }
                            Throwable cause = unwrap(error);
                            if (cause instanceof DocumentExistsException) {
                                return false;
                            }
                            throw rethrow(cause);
                        });
                }

                return asyncCollection.replace(documentId, newData, buildReplaceOptions(newState, currentCas, timeoutNanos))
                    .thenApply(result -> {
                        currentCas = result.cas();
                        return true;
                    })
                    .handle((replaced, error) -> {
                        if (error == null) {
                            return replaced;
                        }
                        Throwable cause = unwrap(error);
                        if (cause instanceof DocumentNotFoundException || cause instanceof CasMismatchException) {
                            return false;
                        }
                        throw rethrow(cause);
                    });
            }
        };
    }

    @Override
    protected CompletableFuture<Void> removeAsync(K key) {
        String documentId = keyMapper.toString(key);
        return asyncCollection.remove(documentId, buildRemoveOptions(Optional.empty()))
            .thenApply(result -> null)
            .handle((ignored, error) -> {
                if (error == null) {
                    return null;
                }
                Throwable cause = unwrap(error);
                if (cause instanceof DocumentNotFoundException) {
                    return null;
                }
                throw rethrow(cause);
            });
    }

    @Override
    public void removeProxy(K key) {
        try {
            executeRemove(keyMapper.toString(key), Optional.empty());
        } catch (DocumentNotFoundException e) {
            // ignore
        }
    }

    @Override
    public boolean isAsyncModeSupported() {
        return true;
    }

    @Override
    public boolean isExpireAfterWriteSupported() {
        return true;
    }

    private GetResult executeGet(String documentId, Optional<Long> timeoutNanos) {
        if (collection != null) {
            return collection.get(documentId, buildGetOptions(timeoutNanos));
        }
        return AsyncUtils.block(asyncCollection.get(documentId, buildGetOptions(timeoutNanos)));
    }

    private MutationResult executeInsert(String documentId, byte[] newData, RemoteBucketState newState, Optional<Long> timeoutNanos) {
        if (collection != null) {
            return collection.insert(documentId, newData, buildInsertOptions(newState, timeoutNanos));
        }
        return AsyncUtils.block(asyncCollection.insert(documentId, newData, buildInsertOptions(newState, timeoutNanos)));
    }

    private MutationResult executeReplace(String documentId, byte[] newData, RemoteBucketState newState, long cas, Optional<Long> timeoutNanos) {
        if (collection != null) {
            return collection.replace(documentId, newData, buildReplaceOptions(newState, cas, timeoutNanos));
        }
        return AsyncUtils.block(asyncCollection.replace(documentId, newData, buildReplaceOptions(newState, cas, timeoutNanos)));
    }

    private MutationResult executeRemove(String documentId, Optional<Long> timeoutNanos) {
        if (collection != null) {
            return collection.remove(documentId, buildRemoveOptions(timeoutNanos));
        }
        return AsyncUtils.block(asyncCollection.remove(documentId, buildRemoveOptions(timeoutNanos)));
    }

    private GetOptions buildGetOptions(Optional<Long> timeoutNanos) {
        GetOptions options = getOptions().transcoder(RAW_BINARY_TRANSCODER);
        timeoutNanos.map(Duration::ofNanos).ifPresent(options::timeout);
        return options;
    }

    private InsertOptions buildInsertOptions(RemoteBucketState newState, Optional<Long> timeoutNanos) {
        InsertOptions options = insertOptions().transcoder(RAW_BINARY_TRANSCODER);
        timeoutNanos.map(Duration::ofNanos).ifPresent(options::timeout);
        expiryFor(newState).ifPresent(options::expiry);
        return options;
    }

    private ReplaceOptions buildReplaceOptions(RemoteBucketState newState, long cas, Optional<Long> timeoutNanos) {
        ReplaceOptions options = replaceOptions()
            .transcoder(RAW_BINARY_TRANSCODER)
            .cas(cas);
        timeoutNanos.map(Duration::ofNanos).ifPresent(options::timeout);
        expiryFor(newState).ifPresent(options::expiry);
        return options;
    }

    private RemoveOptions buildRemoveOptions(Optional<Long> timeoutNanos) {
        RemoveOptions options = removeOptions();
        timeoutNanos.map(Duration::ofNanos).ifPresent(options::timeout);
        return options;
    }

    private Optional<Duration> expiryFor(RemoteBucketState newState) {
        if (expirationStrategy.getClass() == NoneExpirationAfterWriteStrategy.class) {
            return Optional.empty();
        }

        long ttlMillis = expirationStrategy.calculateTimeToLiveMillis(newState, currentTimeNanos());
        if (ttlMillis < 1_000) {
            return Optional.of(Duration.ofSeconds(1));
        }
        return Optional.of(Duration.ofMillis(ttlMillis));
    }

    private Throwable unwrap(Throwable error) {
        if (error instanceof CompletionException completionException && completionException.getCause() != null) {
            return completionException.getCause();
        }
        return error;
    }

    private RuntimeException rethrow(Throwable error) {
        if (error instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new CompletionException(error);
    }
}
