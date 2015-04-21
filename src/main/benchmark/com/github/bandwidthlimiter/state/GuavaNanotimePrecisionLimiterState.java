package com.github.bandwidthlimiter.state;

import com.google.common.util.concurrent.RateLimiter;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class GuavaNanotimePrecisionLimiterState {

    public final RateLimiter guavaRateLimiter = RateLimiter.create(Long.MAX_VALUE/2);

}
