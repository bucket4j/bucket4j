package io.github.bucket4j;

import io.github.bucket4j.distributed.AsyncBucket;
import io.github.bucket4j.mock.BucketType;
import spock.lang.Specification;

import java.time.Duration;

import static io.github.bucket4j.TimeMeter.SYSTEM_MILLISECONDS;

class ToStringSpecification extends Specification {

    def "check that toString does not fail"() {
        when:
            for (BucketType type : BucketType.values()) {
                for (TimeMeter meter : [SYSTEM_MILLISECONDS, SYSTEM_MILLISECONDS]) {
                    BucketConfiguration configuration = BucketConfiguration.builder()
                            .addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(1))
                            .build()
                    Bucket bucket = type.createBucket(configuration, meter)
                    println bucket.toString()
                    if (type.isAsyncModeSupported()) {
                        AsyncBucket asyncBucket = type.createAsyncBucket(configuration, meter)
                        println asyncBucket.toString()
                    }
                }
            }
        then:
            noExceptionThrown()
    }

}
