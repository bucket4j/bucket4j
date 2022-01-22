package io.github.bucket4j.state;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.internal.AtomicRateLimiter;
import io.github.resilience4j.ratelimiter.internal.SemaphoreBasedRateLimiter;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.time.Duration;
import java.util.function.Supplier;

@State(Scope.Benchmark)
public class Resilience4jState {

    private final RateLimiterConfig rateLimiterConfig = RateLimiterConfig.custom()
            .limitForPeriod(Integer.MAX_VALUE / 2)
            .timeoutDuration(Duration.ofNanos(Long.MAX_VALUE / 2))
            .build();

    private final Supplier<String> stringSupplier = () -> {
        return "";
    };

    private final  RateLimiter semaphoreBasedRateLimiter = new SemaphoreBasedRateLimiter("semaphoreBased", rateLimiterConfig);
    private final  AtomicRateLimiter atomicRateLimiter = new AtomicRateLimiter("atomicBased", rateLimiterConfig);

    public final Supplier<String> semaphoreGuardedSupplier = RateLimiter.decorateSupplier(semaphoreBasedRateLimiter, stringSupplier);
    public final Supplier<String> atomicGuardedSupplier = RateLimiter.decorateSupplier(atomicRateLimiter, stringSupplier);
}
