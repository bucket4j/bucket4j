package io.github.bucket4j.state;

import com.google.common.util.concurrent.RateLimiter;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class GuavaLimiterState {

    public final RateLimiter guavaRateLimiter = RateLimiter.create(Long.MAX_VALUE / 2.0);

    public final RateLimiter _10_milion_rps_RateLimiter = RateLimiter.create(10_000_000);

}
