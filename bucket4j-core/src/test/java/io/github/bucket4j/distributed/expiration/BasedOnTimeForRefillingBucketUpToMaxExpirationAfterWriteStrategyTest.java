package io.github.bucket4j.distributed.expiration;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.BucketState;
import io.github.bucket4j.MathType;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.remote.RemoteStat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BasedOnTimeForRefillingBucketUpToMaxExpirationAfterWriteStrategyTest {

    private RemoteBucketState newState(boolean intervally, long refillPeriodNanos) {
        Duration period = Duration.ofNanos(refillPeriodNanos);
        Bandwidth bandwidth = intervally
                ? Bandwidth.builder().capacity(1).refillIntervally(1, period).build()
                : Bandwidth.builder().capacity(1).refillGreedy(1, period).build();
        BucketConfiguration configuration = BucketConfiguration.builder().addLimit(bandwidth).build();
        return new RemoteBucketState(BucketState.createInitialState(configuration, MathType.INTEGER_64_BITS, 0), new RemoteStat(0), null);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void doesNotExpireBeforeTheConsumedTokenIsRefilled(boolean intervally) {
        RemoteBucketState state = newState(intervally, 1_999_999);
        state.consume(1);
        ExpirationAfterWriteStrategy strategy = ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ZERO);

        long ttlMillis = strategy.calculateTimeToLiveMillis(state, 0);

        assertEquals(2, ttlMillis);
        state.refillAllBandwidth(1_000_000);
        assertEquals(0, state.getAvailableTokens());
        state.refillAllBandwidth(ttlMillis * 1_000_000);
        assertEquals(1, state.getAvailableTokens());
    }

    @ParameterizedTest
    @CsvSource({
            "1, 1",
            "999999, 1",
            "1000000, 1",
            "1000001, 2",
            "2000000, 2",
            "9223372036854775807, 9223372036855"
    })
    void roundsRefillTimeUpToWholeMilliseconds(long refillPeriodNanos, long expectedTtlMillis) {
        RemoteBucketState state = newState(false, refillPeriodNanos);
        state.consume(1);
        ExpirationAfterWriteStrategy strategy = ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ZERO);

        assertEquals(expectedTtlMillis, strategy.calculateTimeToLiveMillis(state, 0));
    }

    @Test
    void preservesTheKeepAfterRefillDuration() {
        RemoteBucketState state = newState(false, 1_999_999);
        state.consume(1);
        ExpirationAfterWriteStrategy strategy = ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofSeconds(5));

        assertEquals(5_002, strategy.calculateTimeToLiveMillis(state, 0));
    }

    @Test
    void preservesTheMinimumTtlForAFullBucket() {
        RemoteBucketState state = newState(false, 1_999_999);

        assertEquals(1, ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ZERO).calculateTimeToLiveMillis(state, 0));
        assertEquals(5_000, ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofSeconds(5)).calculateTimeToLiveMillis(state, 0));
    }
}
