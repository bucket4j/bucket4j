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

package example.distributed.generic.select_for_update.postgresql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;

public class Select_For_Update_PostgreSQL_Example {

    public static void main(String[] args) throws SQLException {
        PostgreSQLContainer container = startPostgreSQLContainer();
        final DataSource dataSource = createJdbcDataSource(container);
        PostgreSQLBackend backend = new PostgreSQLBackend(dataSource);

        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(10, Duration.ofSeconds(1)))
                .build();

        Bucket bucket = backend.builder().buildProxy(42L, configuration);

        CountDownLatch countDownLatch = new CountDownLatch(4);
        for (int i = 0; i < 4; i++) {
            new Thread(() -> {
                countDownLatch.countDown();
                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    while (bucket.tryConsume(1)) {
                        System.out.println("consumed from Thread " + Thread.currentThread().getName());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
        System.out.println("By");
    }

    @NotNull
    private static DataSource createJdbcDataSource(PostgreSQLContainer container) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(container.getJdbcUrl());
        hikariConfig.setUsername(container.getUsername());
        hikariConfig.setPassword(container.getPassword());
        hikariConfig.setDriverClassName(container.getDriverClassName());
        hikariConfig.setMaximumPoolSize(100);
        return new HikariDataSource(hikariConfig);
    }

    @NotNull
    private static PostgreSQLContainer startPostgreSQLContainer() {
        PostgreSQLContainer container = new PostgreSQLContainer();
        container.start();
        return container;
    }

}
