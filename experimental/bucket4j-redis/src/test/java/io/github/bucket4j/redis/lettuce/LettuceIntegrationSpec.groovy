package io.github.bucket4j.redis.lettuce

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Refill
import io.github.bucket4j.distributed.proxy.RecoveryStrategy
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.testcontainers.containers.GenericContainer
import spock.lang.Specification

import java.time.Duration
import java.util.concurrent.CountDownLatch

class LettuceIntegrationSpec extends Specification {

    final static public GenericContainer redis = new GenericContainer("redis:5-alpine")
            .withExposedPorts(6379)

    LettuceRedisBackend<String> backend

    def setupSpec() {
        redis.start()
    }

    def setup() {
        def connectionFactory = new LettuceConnectionFactory(new RedisStandaloneConfiguration(redis.containerIpAddress, redis.getMappedPort(6379)))
        connectionFactory.afterPropertiesSet()
        backend = new LettuceRedisBackend<>(connectionFactory)
    }

    def teardownSpec() {
        redis.stop()
    }

    def 'test all the things'() {
        given:
            def threadCount = 4
            def invocations = 2500
            def bucket = backend.builder()
            // essentially making sure that the bucket won't refill during the test, affecting token count
                    .addLimit(Bandwidth.classic(threadCount * invocations, Refill.intervally(1, Duration.ofMinutes(100))))
                    .build("some_key_boi", RecoveryStrategy.RECONSTRUCT)
            def latch = new CountDownLatch(threadCount)
        expect:
            bucket.getAvailableTokens() == threadCount * invocations
        when:
            def threads = (1..threadCount).collect { id ->
                println "starting thread #$id"
                Thread.start {
                    latch.countDown()
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    // consume one token per invocation
                    (1..invocations).each { bucket.consume(1) }
                    println "finishing thread #$id"
                }
            }
            threads.forEach { it.join() }
        then:
            bucket.getAvailableTokens() == 0
    }

}
