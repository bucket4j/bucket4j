package com.github.bandwidthlimiter.state;

import com.github.bandwidthlimiter.Buckets;
import com.github.bandwidthlimiter.bucket.Bucket;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class LocalMillisecondPrecisionState {

    public final Bucket bucket = Buckets
            .withMillisTimePrecision().withLimitedBandwidth(Long.MAX_VALUE / 2, Long.MAX_VALUE / 2)
            .build();

}
