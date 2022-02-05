package io.github.bucket4j.postgresql;

import io.github.bucket4j.distributed.proxy.ClientSideConfig;

import javax.sql.DataSource;

public class PostgreSQLProxyConfiguration {

    private final DataSource dataSource;
    private final ClientSideConfig clientSideConfig;
    private final BucketTableSettings tableSettings;
    private final LockBasedTransactionType lockBasedTransactionType;

    public PostgreSQLProxyConfiguration(DataSource dataSource) {
        this(dataSource, BucketTableSettings.defaultSettings());
    }

    public PostgreSQLProxyConfiguration(DataSource dataSource, BucketTableSettings tableSettings) {
        this(dataSource, ClientSideConfig.getDefault(), tableSettings, LockBasedTransactionType.ADVISORY);
    }

    public PostgreSQLProxyConfiguration(DataSource dataSource, ClientSideConfig clientSideConfig) {
        this(dataSource, clientSideConfig, BucketTableSettings.defaultSettings(), LockBasedTransactionType.ADVISORY);
    }

    public PostgreSQLProxyConfiguration(DataSource dataSource, LockBasedTransactionType lockBasedTransactionType) {
        this(dataSource, BucketTableSettings.defaultSettings(), lockBasedTransactionType);
    }

    public PostgreSQLProxyConfiguration(DataSource dataSource, BucketTableSettings tableSettings, LockBasedTransactionType lockBasedTransactionType) {
        this(dataSource, ClientSideConfig.getDefault(), tableSettings, lockBasedTransactionType);
    }

    public PostgreSQLProxyConfiguration(DataSource dataSource, ClientSideConfig clientSideConfig, LockBasedTransactionType lockBasedTransactionType) {
        this(dataSource, clientSideConfig, BucketTableSettings.defaultSettings(), lockBasedTransactionType);
    }

    public PostgreSQLProxyConfiguration(DataSource dataSource, ClientSideConfig clientSideConfig, BucketTableSettings tableSettings, LockBasedTransactionType lockBasedTransactionType) {
        this.dataSource = dataSource;
        this.clientSideConfig = clientSideConfig;
        this.tableSettings = tableSettings;
        this.lockBasedTransactionType = lockBasedTransactionType;
    }

    public LockBasedTransactionType getLockBasedTransactionType() {
        return lockBasedTransactionType;
    }

    public String getIdName() {
        return tableSettings.getIdName();
    }

    public String getStateName() {
        return tableSettings.getStateName();
    }

    public String getTableName() {
        return tableSettings.getTableName();
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public ClientSideConfig getClientSideConfig() {
        return clientSideConfig;
    }
}
