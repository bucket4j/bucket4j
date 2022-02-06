package io.github.bucket4j.postgresql;

import io.github.bucket4j.BucketExceptions;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;

import javax.sql.DataSource;

public final class PostgreSQLProxyConfigurationBuilder {
    private ClientSideConfig clientSideConfig;
    private BucketTableSettings tableSettings;
    private LockBasedTransactionType lockBasedTransactionType;

    private PostgreSQLProxyConfigurationBuilder() {
    }

    public static PostgreSQLProxyConfigurationBuilder builder() {
        return new PostgreSQLProxyConfigurationBuilder();
    }

    public PostgreSQLProxyConfigurationBuilder addClientSideConfig(ClientSideConfig clientSideConfig) {
        this.clientSideConfig = clientSideConfig;
        return this;
    }

    public PostgreSQLProxyConfigurationBuilder addTableSettings(BucketTableSettings tableSettings) {
        this.tableSettings = tableSettings;
        return this;
    }

    public PostgreSQLProxyConfigurationBuilder addLockBasedTransactionType(LockBasedTransactionType lockBasedTransactionType) {
        this.lockBasedTransactionType = lockBasedTransactionType;
        return this;
    }

    public PostgreSQLProxyConfiguration build(DataSource dataSource) {
        if(dataSource == null) {
            throw new BucketExceptions.BucketExecutionException("DataSource cannot be null");
        }
        if(lockBasedTransactionType == null) {
            this.lockBasedTransactionType = LockBasedTransactionType.ADVISORY;
        }
        if(tableSettings == null) {
            this.tableSettings = BucketTableSettings.defaultSettings();
        }
        if(clientSideConfig == null) {
            this.clientSideConfig = ClientSideConfig.getDefault();
        }
        return new PostgreSQLProxyConfiguration(dataSource, clientSideConfig, tableSettings, lockBasedTransactionType);
    }
}
