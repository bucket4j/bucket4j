package io.github.bucket4j

import io.github.bucket4j.TimeMeter.SYSTEM_MILLISECONDS
import io.github.bucket4j.distributed.AsyncBucketProxy
import io.github.bucket4j.mock.BucketType
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.BehaviorSpec
import java.time.Duration

class ToStringSpecification : BehaviorSpec({

    Given("a bucket configuration") {
        val configuration = BucketConfiguration.builder()
            .addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(1))
            .build()

        Then("toString() does not fail for any bucket type/backend") {
            shouldNotThrowAny {
                for (type in BucketType.values()) {
                    for (meter in listOf(SYSTEM_MILLISECONDS, SYSTEM_MILLISECONDS)) {
                        val bucket: Bucket = type.createBucket(configuration, meter)
                        println(bucket.toString())

                        val asyncBucket: AsyncBucketProxy = type.createAsyncBucket(configuration, meter)
                        println(asyncBucket.toString())
                    }
                }
            }
        }
    }
})
