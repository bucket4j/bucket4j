package io.github.bucket4j.distributed.proxy;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.BucketExceptions.BucketExecutionException;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AbstractCompareAndSwapBasedProxyManager;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AsyncCompareAndSwapOperation;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.CompareAndSwapOperation;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class MaxRetriesTest {

    @Test
    public void testMaxRetriesConfiguration() {
        // Test that maxRetries can be configured
        ClientSideConfig config = ClientSideConfig.getDefault().withMaxRetries(5);
        assertTrue(config.getMaxRetries().isPresent());
        assertEquals(5, config.getMaxRetries().get());
    }

    @Test
    public void testMaxRetriesNotSetByDefault() {
        // Test that maxRetries is not set by default (backward compatibility)
        ClientSideConfig config = ClientSideConfig.getDefault();
        assertFalse(config.getMaxRetries().isPresent());
    }

    @Test
    public void testMaxRetriesValidation() {
        // Test that non-positive maxRetries throws exception
        assertThrows(IllegalArgumentException.class, () -> {
            ClientSideConfig.getDefault().withMaxRetries(0);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            ClientSideConfig.getDefault().withMaxRetries(-1);
        });
    }

    @Test
    public void testSyncMaxRetriesExceeded() {
        // Create a mock that always fails CAS
        AtomicInteger attemptCount = new AtomicInteger(0);
        ClientSideConfig config = ClientSideConfig.getDefault().withMaxRetries(3);
        
        FailingCasProxyManager<String> proxyManager = new FailingCasProxyManager<>(config, attemptCount);
        
        BucketConfiguration bucketConfig = BucketConfiguration.builder()
            .addLimit(Bandwidth.simple(10, Duration.ofSeconds(1)))
            .build();
        
        BucketProxy bucket = proxyManager.builder().build("test-key", bucketConfig);
        
        // Should throw BucketExecutionException after 3 attempts
        BucketExecutionException exception = assertThrows(BucketExecutionException.class, () -> {
            bucket.tryConsume(1);
        });
        
        assertTrue(exception.getMessage().contains("CAS operation failed after 3 retry attempts"));
        assertEquals(3, attemptCount.get());
    }

    @Test
    public void testSyncUnlimitedRetriesWhenNotConfigured() {
        // When maxRetries is not configured, should retry many times
        AtomicInteger attemptCount = new AtomicInteger(0);
        ClientSideConfig config = ClientSideConfig.getDefault(); // No maxRetries
        
        SucceedingAfterNAttemptsProxyManager<String> proxyManager = 
            new SucceedingAfterNAttemptsProxyManager<>(config, attemptCount, 100);
        
        BucketConfiguration bucketConfig = BucketConfiguration.builder()
            .addLimit(Bandwidth.simple(10, Duration.ofSeconds(1)))
            .build();
        
        BucketProxy bucket = proxyManager.builder().build("test-key", bucketConfig);
        
        // Should succeed after 100 attempts
        assertTrue(bucket.tryConsume(1));
        assertEquals(100, attemptCount.get());
    }

    @Test
    public void testAsyncMaxRetriesExceeded() throws Exception {
        // Create a mock that always fails CAS for async operations
        AtomicInteger attemptCount = new AtomicInteger(0);
        ClientSideConfig config = ClientSideConfig.getDefault().withMaxRetries(5);
        
        FailingCasProxyManager<String> proxyManager = new FailingCasProxyManager<>(config, attemptCount);
        
        BucketConfiguration bucketConfig = BucketConfiguration.builder()
            .addLimit(Bandwidth.simple(10, Duration.ofSeconds(1)))
            .build();
        
        var asyncBucket = proxyManager.asAsync().builder().build("test-key", bucketConfig);
        
        // Should throw BucketExecutionException after 5 attempts
        ExecutionException exception = assertThrows(ExecutionException.class, () -> {
            asyncBucket.tryConsume(1).get();
        });
        
        assertTrue(exception.getCause() instanceof BucketExecutionException);
        assertTrue(exception.getCause().getMessage().contains("CAS operation failed after 5 retry attempts"));
        assertEquals(5, attemptCount.get());
    }

    /**
     * Mock proxy manager that always fails CAS operations
     */
    private static class FailingCasProxyManager<K> extends AbstractCompareAndSwapBasedProxyManager<K> {
        private final Map<K, byte[]> stateMap = new HashMap<>();
        private final AtomicInteger attemptCount;

        public FailingCasProxyManager(ClientSideConfig config, AtomicInteger attemptCount) {
            super(config);
            this.attemptCount = attemptCount;
        }

        @Override
        protected CompletableFuture<Void> removeAsync(K key) {
            stateMap.remove(key);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected CompareAndSwapOperation beginCompareAndSwapOperation(K key) {
            return new CompareAndSwapOperation() {
                @Override
                public Optional<byte[]> getStateData(Optional<Long> timeoutNanos) {
                    return Optional.ofNullable(stateMap.get(key));
                }
                @Override
                public boolean compareAndSwap(byte[] originalData, byte[] newData, RemoteBucketState newState, Optional<Long> timeoutNanos) {
                    attemptCount.incrementAndGet();
                    return false; // Always fail CAS
                }
            };
        }

        @Override
        protected AsyncCompareAndSwapOperation beginAsyncCompareAndSwapOperation(K key) {
            byte[] backup = stateMap.get(key);
            return new AsyncCompareAndSwapOperation() {
                @Override
                public CompletableFuture<Optional<byte[]>> getStateData(Optional<Long> timeoutNanos) {
                    return CompletableFuture.completedFuture(Optional.ofNullable(backup));
                }
                @Override
                public CompletableFuture<Boolean> compareAndSwap(byte[] originalData, byte[] newData, RemoteBucketState newState, Optional<Long> timeoutNanos) {
                    attemptCount.incrementAndGet();
                    return CompletableFuture.completedFuture(false); // Always fail CAS
                }
            };
        }

        @Override
        public void removeProxy(K key) {
            stateMap.remove(key);
        }

        @Override
        public boolean isAsyncModeSupported() {
            return true;
        }
    }

    /**
     * Mock proxy manager that succeeds CAS after N attempts
     */
    private static class SucceedingAfterNAttemptsProxyManager<K> extends AbstractCompareAndSwapBasedProxyManager<K> {
        private final Map<K, byte[]> stateMap = new HashMap<>();
        private final AtomicInteger attemptCount;
        private final int succeedAfter;

        public SucceedingAfterNAttemptsProxyManager(ClientSideConfig config, AtomicInteger attemptCount, int succeedAfter) {
            super(config);
            this.attemptCount = attemptCount;
            this.succeedAfter = succeedAfter;
        }

        @Override
        protected CompletableFuture<Void> removeAsync(K key) {
            stateMap.remove(key);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected CompareAndSwapOperation beginCompareAndSwapOperation(K key) {
            return new CompareAndSwapOperation() {
                @Override
                public Optional<byte[]> getStateData(Optional<Long> timeoutNanos) {
                    return Optional.ofNullable(stateMap.get(key));
                }
                @Override
                public boolean compareAndSwap(byte[] originalData, byte[] newData, RemoteBucketState newState, Optional<Long> timeoutNanos) {
                    int attempt = attemptCount.incrementAndGet();
                    if (attempt >= succeedAfter) {
                        stateMap.put(key, newData);
                        return true;
                    }
                    return false; // Fail until we reach succeedAfter
                }
            };
        }

        @Override
        protected AsyncCompareAndSwapOperation beginAsyncCompareAndSwapOperation(K key) {
            byte[] backup = stateMap.get(key);
            return new AsyncCompareAndSwapOperation() {
                @Override
                public CompletableFuture<Optional<byte[]>> getStateData(Optional<Long> timeoutNanos) {
                    return CompletableFuture.completedFuture(Optional.ofNullable(backup));
                }
                @Override
                public CompletableFuture<Boolean> compareAndSwap(byte[] originalData, byte[] newData, RemoteBucketState newState, Optional<Long> timeoutNanos) {
                    int attempt = attemptCount.incrementAndGet();
                    if (attempt >= succeedAfter) {
                        stateMap.put(key, newData);
                        return CompletableFuture.completedFuture(true);
                    }
                    return CompletableFuture.completedFuture(false);
                }
            };
        }

        @Override
        public void removeProxy(K key) {
            stateMap.remove(key);
        }

        @Override
        public boolean isAsyncModeSupported() {
            return true;
        }
    }
}

