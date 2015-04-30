package com.github.bandwidthlimiter.state;

import com.github.bandwidthlimiter.Limiters;
import com.github.bandwidthlimiter.bucket.Bucket;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class LocalThreadSafeNanotimePrecisionState {

    public final Bucket bucket = Limiters
            .withNanoTimePrecision().withLimitedBandwidth(Long.MAX_VALUE / 2, Long.MAX_VALUE / 2)
            .build();

}
