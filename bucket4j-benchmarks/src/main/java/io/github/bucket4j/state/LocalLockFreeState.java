package io.github.bucket4j.state;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.time.Duration;

@State(Scope.Benchmark)
public class LocalLockFreeState {

    public final Bucket unlimitedBucket = Bucket4j.builder()
            .addLimit(
                    Bandwidth.simple(Long.MAX_VALUE / 2, Duration.ofNanos(Long.MAX_VALUE / 2))
            ).build();

    public final Bucket _10_milion_rps_Bucket = Bucket4j.builder()
            .addLimit(Bandwidth.simple(10_000_000, Duration.ofSeconds(1)).withInitialTokens(0))
            .build();


}
