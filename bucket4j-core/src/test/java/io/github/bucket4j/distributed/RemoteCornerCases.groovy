package io.github.bucket4j.distributed


import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.mock.AsyncProxyManagerMock
import io.github.bucket4j.mock.ProxyManagerMock
import spock.lang.Specification

import java.time.Duration
import java.util.concurrent.CompletableFuture

import static io.github.bucket4j.TimeMeter.SYSTEM_MILLISECONDS

class RemoteCornerCases extends Specification {

    def "should complete future exceptionally if proxyManager failed"() {
        setup:
            AsyncProxyManagerMock proxyManagerMock = new AsyncProxyManagerMock(SYSTEM_MILLISECONDS)
            BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit({it.capacity(1).refillGreedy(1, Duration.ofNanos(1))})
                .build()

            AsyncBucketProxy bucket = proxyManagerMock.builder()
                .build("66", {CompletableFuture.completedFuture(configuration)})
        when:
            proxyManagerMock.setException(new RuntimeException())
            CompletableFuture<Boolean> future = bucket.tryConsume(1)
        then:
            future.isCompletedExceptionally()
    }

}
