package io.github.bucket4j.distributed.proxy;

import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AbstractCompareAndSwapBasedProxyManager;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AsyncCompareAndSwapOperation;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.CompareAndSwapOperation;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AbstractProxyManagerBuilder to verify maxRetries and retryStrategy configuration
 */
public class AbstractProxyManagerBuilderTest {

    @Test
    public void testMaxRetriesConfiguration() {
        TestProxyManagerBuilder builder = new TestProxyManagerBuilder();
        
        // Configure maxRetries
        builder.maxRetries(5);
        
        // Verify it's set in the builder
        assertEquals(Optional.of(5), builder.getMaxRetries());
        
        // Verify it's passed to ClientSideConfig
        ClientSideConfig config = builder.getClientSideConfig();
        assertTrue(config.getMaxRetries().isPresent());
        assertEquals(5, config.getMaxRetries().get());
    }

    @Test
    public void testRetryStrategyConfiguration() {
        TestProxyManagerBuilder builder = new TestProxyManagerBuilder();
        
        // Configure retryStrategy
        RetryStrategy strategy = metadata -> metadata.getAttemptNumber() < 10;
        builder.retryStrategy(strategy);
        
        // Verify it's set in the builder
        assertTrue(builder.getRetryStrategy().isPresent());
        assertEquals(strategy, builder.getRetryStrategy().get());
        
        // Verify it's passed to ClientSideConfig
        ClientSideConfig config = builder.getClientSideConfig();
        assertTrue(config.getRetryStrategy().isPresent());
        assertEquals(strategy, config.getRetryStrategy().get());
    }

    @Test
    public void testMaxRetriesNotSetByDefault() {
        TestProxyManagerBuilder builder = new TestProxyManagerBuilder();
        
        // Verify maxRetries is not set by default
        assertFalse(builder.getMaxRetries().isPresent());
        
        ClientSideConfig config = builder.getClientSideConfig();
        assertFalse(config.getMaxRetries().isPresent());
    }

    @Test
    public void testRetryStrategyNotSetByDefault() {
        TestProxyManagerBuilder builder = new TestProxyManagerBuilder();
        
        // Verify retryStrategy is not set by default
        assertFalse(builder.getRetryStrategy().isPresent());
        
        ClientSideConfig config = builder.getClientSideConfig();
        assertFalse(config.getRetryStrategy().isPresent());
    }

    @Test
    public void testMaxRetriesValidation() {
        TestProxyManagerBuilder builder = new TestProxyManagerBuilder();
        
        // Test that non-positive maxRetries throws exception
        assertThrows(IllegalArgumentException.class, () -> builder.maxRetries(0));
        assertThrows(IllegalArgumentException.class, () -> builder.maxRetries(-1));
    }

    @Test
    public void testRetryStrategyNullValidation() {
        TestProxyManagerBuilder builder = new TestProxyManagerBuilder();
        
        // Test that null retryStrategy throws exception
        assertThrows(NullPointerException.class, () -> builder.retryStrategy(null));
    }

    @Test
    public void testBothMaxRetriesAndRetryStrategyCanBeSet() {
        TestProxyManagerBuilder builder = new TestProxyManagerBuilder();
        
        // Configure both
        RetryStrategy strategy = metadata -> metadata.getAttemptNumber() < 10;
        builder.maxRetries(5).retryStrategy(strategy);
        
        // Verify both are set
        assertEquals(Optional.of(5), builder.getMaxRetries());
        assertTrue(builder.getRetryStrategy().isPresent());
        
        ClientSideConfig config = builder.getClientSideConfig();
        assertEquals(Optional.of(5), config.getMaxRetries());
        assertTrue(config.getRetryStrategy().isPresent());
    }

    /**
     * Test implementation of AbstractProxyManagerBuilder for testing purposes
     */
    private static class TestProxyManagerBuilder extends AbstractProxyManagerBuilder<String, TestProxyManager, TestProxyManagerBuilder> {
        @Override
        public TestProxyManager build() {
            return new TestProxyManager(getClientSideConfig());
        }

        @Override
        public boolean isExpireAfterWriteSupported() {
            return false;
        }
    }

    /**
     * Test implementation of ProxyManager for testing purposes
     */
    private static class TestProxyManager extends AbstractCompareAndSwapBasedProxyManager<String> {
        public TestProxyManager(ClientSideConfig clientSideConfig) {
            super(clientSideConfig);
        }

        @Override
        protected CompareAndSwapOperation beginCompareAndSwapOperation(String key) {
            return null;
        }

        @Override
        protected AsyncCompareAndSwapOperation beginAsyncCompareAndSwapOperation(String key) {
            return null;
        }

        @Override
        public void removeProxy(String key) {
        }

        @Override
        public CompletableFuture<Void> removeAsync(String key) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public boolean isAsyncModeSupported() {
            return false;
        }
    }
}

