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

package example.distributed.generic.postgresql.advisory_lock;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

public class Advisory_Lock_PostgreSQL_Example {

    public static void main(String[] args) throws SQLException, InterruptedException {
        PostgreSQLContainer container = startPostgreSQLContainer();
        final DataSource dataSource = createJdbcDataSource(container);
        AdvisoryLockBasedPostgreSQLProxyManager proxyManager = new AdvisoryLockBasedPostgreSQLProxyManager(dataSource, ClientSideConfig.getDefault());

        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(10, Duration.ofSeconds(1)))
                .build();

        Bucket bucket = proxyManager.builder().build(42L, configuration);

        CountDownLatch startLatch = new CountDownLatch(4);
        CountDownLatch stopLatch = new CountDownLatch(4);
        AtomicLong consumed = new AtomicLong();
        long now = System.nanoTime();
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
        long finish = System.nanoTime();
        System.out.println("time:" + (finish - now));
        System.out.println("Was consumed " + consumed.get() + " tokens");

        long nowv2 = System.nanoTime();
        for (int i = 0; i < 5; i++) {
            bucket.asBlocking().consume(10);
            System.out.println("Was consumed 10 token in blocking mode " + new Date());
        }
        long finishv2 = System.nanoTime();
        System.out.println("timev2:" + (finishv2 - nowv2));
    }

    private static DataSource createJdbcDataSource(PostgreSQLContainer container) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(container.getJdbcUrl());
        hikariConfig.setUsername(container.getUsername());
        hikariConfig.setPassword(container.getPassword());
        hikariConfig.setDriverClassName(container.getDriverClassName());
        hikariConfig.setMaximumPoolSize(100);
        return new HikariDataSource(hikariConfig);
    }

    private static PostgreSQLContainer startPostgreSQLContainer() {
        PostgreSQLContainer container = new PostgreSQLContainer();
        container.start();
        return container;
    }

}
