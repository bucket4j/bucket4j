/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2022 Vladimir Bukhtoyarov
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package com.bucket4j.backward_compatibility;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.TokensInheritanceStrategy;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.jdbc.BucketTableSettings;
import io.github.bucket4j.distributed.jdbc.SQLProxyConfiguration;
import io.github.bucket4j.distributed.jdbc.SQLProxyConfigurationBuilder;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.postgresql.PostgreSQLadvisoryLockBasedProxyManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Properties;

public class PostgreSQLScenarios {

    public static class ImplicitConfigurationReplacementCases {
        public static class CreateBucketWithoutImplicitConfigReplacement {
            public static void main(String[] args) throws IOException {
                try (HikariDataSource dataSource = createJdbcDataSource()) {
                    PostgreSQLadvisoryLockBasedProxyManager proxyManager = createProxyManager(dataSource);
                    proxyManager.removeProxy(666L);

                    BucketConfiguration config = BucketConfiguration.builder()
                            .addLimit(Bandwidth.simple(3, Duration.ofSeconds(60)))
                            .build();
                    BucketProxy bucket = proxyManager.builder().build(666L, config);
                    bucket.getAvailableTokens();
                }
            }
        }

        public static class CreateBucketWithImplicitConfigReplacement {
            public static void main(String[] args) throws IOException {
                try (HikariDataSource dataSource = createJdbcDataSource()) {
                    PostgreSQLadvisoryLockBasedProxyManager proxyManager = createProxyManager(dataSource);
                    proxyManager.removeProxy(666L);

                    BucketConfiguration config = BucketConfiguration.builder()
                            .addLimit(Bandwidth.simple(3, Duration.ofSeconds(60)))
                            .build();
                    BucketProxy bucket = proxyManager.builder()
                            .withImplicitConfigurationReplacement(1, TokensInheritanceStrategy.AS_IS)
                            .build(666L, config);
                    bucket.getAvailableTokens();
                }
            }
        }
    }

    private static HikariDataSource createJdbcDataSource() throws IOException {
        File file = new File("./backward-compatibility-tests/common/target/postgresql.properties");
        if (!file.exists()) {
            throw new IllegalStateException("StartPostgreSQLContainer must be ran before");
        }

        Properties properties = new Properties();

        try (FileInputStream fis = new FileInputStream(file)) {
            properties.load(fis);
        }

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(properties.getProperty("jdbc.url"));
        hikariConfig.setUsername(properties.getProperty("username"));
        hikariConfig.setPassword(properties.getProperty("password"));
        hikariConfig.setDriverClassName("org.postgresql.Driver");
        hikariConfig.setMaximumPoolSize(1);
        return new HikariDataSource(hikariConfig);
    }

    private static PostgreSQLadvisoryLockBasedProxyManager createProxyManager(HikariDataSource dataSource) {
        BucketTableSettings tableSettings = BucketTableSettings.getDefault();
        SQLProxyConfiguration configuration = SQLProxyConfigurationBuilder.builder()
                .withClientSideConfig(ClientSideConfig.getDefault())
                .withTableSettings(tableSettings)
                .build(dataSource);
        return new PostgreSQLadvisoryLockBasedProxyManager(configuration);
    }

}
