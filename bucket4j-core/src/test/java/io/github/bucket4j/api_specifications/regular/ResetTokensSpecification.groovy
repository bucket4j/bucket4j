package io.github.bucket4j.api_specifications.regular

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.distributed.AsyncBucketProxy
import io.github.bucket4j.mock.BucketType
import io.github.bucket4j.mock.TimeMeterMock
import spock.lang.Specification

import java.time.Duration

class ResetTokensSpecification extends Specification {

    def "resetBucket spec"() {
        expect:
            BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(100, Duration.ofNanos(100))
                .withInitialTokens(10))
                .build()
            for (BucketType type : BucketType.values()) {
                for (boolean sync : [true, false]) {
                    for (boolean verbose : [true, false]) {
                        println("type=$type sync=$sync verbose=$verbose")
                        TimeMeterMock timeMeter = new TimeMeterMock(0)
                        if (sync) {
                            Bucket bucket = type.createBucket(configuration, timeMeter)
                            bucket.getAvailableTokens()
                            if (!verbose) {
                                bucket.reset()
                            } else {
                                bucket.asVerbose().reset()
                            }
                            assert bucket.getAvailableTokens() == 100
                        } else {
                            AsyncBucketProxy bucket = type.createAsyncBucket(configuration, timeMeter)
                            bucket.getAvailableTokens()
                            if (!verbose) {
                                bucket.reset().get()
                            } else {
                                bucket.asVerbose().reset().get()
                            }
                            assert bucket.getAvailableTokens().get() == 100
                        }
                    }
                }
            }
    }

}
