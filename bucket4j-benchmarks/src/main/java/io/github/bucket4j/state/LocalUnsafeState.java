package io.github.bucket4j.state;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.local.SynchronizationStrategy;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.time.Duration;

@State(Scope.Benchmark)
public class LocalUnsafeState {

    public final Bucket bucket = Bucket4j.builder()
            .withMillisecondPrecision()
            .addLimit(
                    Bandwidth.simple(Long.MAX_VALUE / 2, Duration.ofNanos(Long.MAX_VALUE / 2))
            )
            .withSynchronizationStrategy(SynchronizationStrategy.NONE)
            .build();


    public final Bucket bucketWithoutRefill = Bucket4j.builder()
            .withMillisecondPrecision()
            .withCustomTimePrecision(new TimeMeter() {
                @Override
                public long currentTimeNanos() {
                    return 0;
                }

                @Override
                public boolean isWallClockBased() {
                    return false;
                }
            })
            .addLimit(
                    Bandwidth.simple(Long.MAX_VALUE / 2, Duration.ofNanos(Long.MAX_VALUE / 2))
            )
            .withSynchronizationStrategy(SynchronizationStrategy.NONE)
            .build();


}
