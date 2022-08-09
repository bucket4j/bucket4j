package com.bucket4j.backward_compatibility;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.bucket4j.distributed.jdbc.BucketTableSettings;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.Properties;

public class StartPostgreSQLContainer {

    public static void main(String[] args) throws IOException, SQLException, InterruptedException {
        PostgreSQLContainer container = new PostgreSQLContainer();
        container.start();

        System.out.println("JDBC URL = " + container.getJdbcUrl());
        DataSource dataSource = createJdbcDataSource(container);

        BucketTableSettings tableSettings = BucketTableSettings.getDefault();
        final String INIT_TABLE_SCRIPT = "CREATE TABLE IF NOT EXISTS {0}({1} BIGINT PRIMARY KEY, {2} BYTEA)";
        try (Connection connection = dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                String query = MessageFormat.format(INIT_TABLE_SCRIPT, tableSettings.getTableName(), tableSettings.getIdName(), tableSettings.getStateName());
                statement.execute(query);
            }
        }

        Properties properties = new Properties();
        properties.setProperty("jdbc.url", container.getJdbcUrl());
        properties.setProperty("username", container.getUsername());
        properties.setProperty("password", container.getPassword());

        File file = new File("./backward-compatibility-tests/common/target/postgresql.properties");
        try (FileOutputStream fos = new FileOutputStream(file, false)) {
            properties.store(fos, "");
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            file.delete();
        }));

        Thread.currentThread().join();
    }

    private static DataSource createJdbcDataSource(PostgreSQLContainer container) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(container.getJdbcUrl());
        hikariConfig.setUsername(container.getUsername());
        hikariConfig.setPassword(container.getPassword());
        hikariConfig.setDriverClassName(container.getDriverClassName());
        hikariConfig.setMaximumPoolSize(1);
        return new HikariDataSource(hikariConfig);
    }

}
