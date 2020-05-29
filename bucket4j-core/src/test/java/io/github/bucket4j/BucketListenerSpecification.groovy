
package io.github.bucket4j

import io.github.bucket4j.mock.BucketType
import io.github.bucket4j.mock.TimeMeterMock
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration

class BucketListenerSpecification extends Specification {


    @Unroll
    def "#type bucket created by toListenable should share tokens with source bucket"(BucketType type) {
        setup:
            TimeMeterMock clock = new TimeMeterMock()
            SimpleBucketListener listener = new SimpleBucketListener()

            BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(10, Duration.ofSeconds(1)))
                .build()
            Bucket sourceBucket = type.createBucket(configuration, clock);
            Bucket listenableBucket = sourceBucket.toListenable(listener)

        when:
            sourceBucket.tryConsume(9)
        then:
            sourceBucket.getAvailableTokens() == 1
            listenableBucket.getAvailableTokens() == 1

        when:
            listenableBucket.tryConsume(1)
        then:
            sourceBucket.getAvailableTokens() == 0
            listenableBucket.getAvailableTokens() == 0

        expect:
            !sourceBucket.tryConsume(1)
            !listenableBucket.tryConsume(1)

        where:
            type << BucketType.values()
    }

}
