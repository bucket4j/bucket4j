package io.github.bucket4j.distributed

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.mock.ProxyManagerMock
import spock.lang.Specification

import java.time.Duration
import java.util.concurrent.CompletableFuture

import static io.github.bucket4j.TimeMeter.SYSTEM_MILLISECONDS

class RemoteCornerCases extends Specification {

    def "should complete future exceptionally if proxyManager failed"() {
        setup:
            ProxyManagerMock proxyManagerMock = new ProxyManagerMock(SYSTEM_MILLISECONDS)
            BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(1, Duration.ofNanos(1)))
                .build()

            AsyncBucketProxy bucket = proxyManagerMock.asAsync().builder()
                .build("66", configuration)
        when:
            proxyManagerMock.setException(new RuntimeException())
            CompletableFuture<Boolean> future = bucket.tryConsume(1)
        then:
            future.isCompletedExceptionally()
    }

}
