/*
 *
 * Copyright 2015-2019 Vladimir Bukhtoyarov
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package example.distributed.generic.redisson.rmap_cas;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import org.redisson.Redisson;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.testcontainers.containers.GenericContainer;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

public class RMAP_Redisson_Example {

    public static void main(String[] args) throws InterruptedException {
        GenericContainer container = startRedisContainer();
        RedissonClient redisson = createRedissonClient(container);

        RMap<Long, byte[]> buckets = redisson.getMap("buckets");

        RMapBasedRedissonBackend backend = new RMapBasedRedissonBackend(buckets, ClientSideConfig.getDefault());

        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(10, Duration.ofSeconds(1)))
                .build();

        Bucket bucket = backend.builder().buildProxy(42L, configuration);

        CountDownLatch startLatch = new CountDownLatch(4);
        CountDownLatch stopLatch = new CountDownLatch(4);
        AtomicLong consumed = new AtomicLong();
        for (int i = 0; i < 4; i++) {
            new Thread(() -> {
                startLatch.countDown();
                try {
                    startLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    while (bucket.tryConsume(1)) {
                        consumed.incrementAndGet();
                        System.out.println("consumed from Thread " + Thread.currentThread().getName());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                stopLatch.countDown();
            }).start();
        }
        stopLatch.await();
        System.out.println("Was consumed " + consumed.get() + " tokens ");

        for (int i = 0; i < 5; i++) {
            bucket.asBlocking().consume(10);
            System.out.println("Was consumed 10 token in blocking mode " + new Date());
        }
    }

    private static RedissonClient createRedissonClient(GenericContainer container) {
        String redisAddress = container.getContainerIpAddress();
        Integer redisPort = container.getMappedPort(6379);
        String redisUrl = "redis://" + redisAddress + ":" + redisPort;

        Config config = new Config();
        config.useSingleServer()
                .setAddress(redisUrl);

        return Redisson.create(config);
    }

    private static GenericContainer startRedisContainer() {
        GenericContainer genericContainer = new GenericContainer("redis:4.0.11")
                .withExposedPorts(6379);
        genericContainer.start();
        return genericContainer;
    }

}
