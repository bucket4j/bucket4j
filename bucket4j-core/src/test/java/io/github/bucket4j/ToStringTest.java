package io.github.bucket4j;

import io.github.bucket4j.distributed.AsyncBucketProxy;
import io.github.bucket4j.mock.BucketType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static io.github.bucket4j.TimeMeter.SYSTEM_MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThatNoException;

@DisplayName("ToString Specification")
class ToStringTest {

    @Test
    void checkThatToStringDoesNotFail() throws Exception {
        assertThatNoException().isThrownBy(() -> {
            for (BucketType type : BucketType.values()) {
                for (TimeMeter meter : new TimeMeter[]{SYSTEM_MILLISECONDS, SYSTEM_MILLISECONDS}) {
                    BucketConfiguration configuration = BucketConfiguration.builder()
                        .addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(1))
                        .build();
                    Bucket bucket = type.createBucket(configuration, meter);
                    System.out.println(bucket);

                    AsyncBucketProxy asyncBucket = type.createAsyncBucket(configuration, meter);
                    System.out.println(asyncBucket);
                }
            }
        });
    }

}
