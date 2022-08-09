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

                    BucketConfiguration config = BucketConfiguration.builder()
                            .addLimit(Bandwidth.simple(3, Duration.ofSeconds(60)))
                            .build();
                    BucketProxy bucket = proxyManager.builder().build(666L, config);
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
