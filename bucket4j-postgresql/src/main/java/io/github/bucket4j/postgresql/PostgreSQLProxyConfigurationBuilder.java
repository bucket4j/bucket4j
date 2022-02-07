package io.github.bucket4j.postgresql;

import io.github.bucket4j.BucketExceptions;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;

import javax.sql.DataSource;

/**
 * @author Maxim Bartkov
 * The class to build {@link PostgreSQLProxyConfiguration}
 */
public final class PostgreSQLProxyConfigurationBuilder {
    private ClientSideConfig clientSideConfig;
    private BucketTableSettings tableSettings;
    private LockBasedTransactionType lockBasedTransactionType;

    private PostgreSQLProxyConfigurationBuilder() {
    }

    public static PostgreSQLProxyConfigurationBuilder builder() {
        return new PostgreSQLProxyConfigurationBuilder();
    }

    /**
     * @param clientSideConfig {@link ClientSideConfig} client-side configuration for proxy-manager.
     *                                                 By default, under the hood uses {@link ClientSideConfig#getDefault}
     * @return {@link PostgreSQLProxyConfigurationBuilder}
     */
    public PostgreSQLProxyConfigurationBuilder addClientSideConfig(ClientSideConfig clientSideConfig) {
        this.clientSideConfig = clientSideConfig;
        return this;
    }

    /**
     * @param tableSettings {@link BucketTableSettings} define a configuration of the table to use as a Buckets store.
     *                                                 By default, under the hood uses {@link BucketTableSettings#getDefault}
     * @return {@link PostgreSQLProxyConfigurationBuilder}
     */
    public PostgreSQLProxyConfigurationBuilder addTableSettings(BucketTableSettings tableSettings) {
        this.tableSettings = tableSettings;
        return this;
    }

    /**
     * @param lockBasedTransactionType a strategy of transaction for the table.
     *                                 By default, under the hood uses {@link LockBasedTransactionType#ADVISORY}
     * @return {@link PostgreSQLProxyConfigurationBuilder}
     */
    public PostgreSQLProxyConfigurationBuilder addLockBasedTransactionType(LockBasedTransactionType lockBasedTransactionType) {
        this.lockBasedTransactionType = lockBasedTransactionType;
        return this;
    }

    /**
     * The method takes a {@link DataSource} as a required parameter
     * @param dataSource - a database configuration
     * @return {@link PostgreSQLProxyConfiguration}
     */
    public PostgreSQLProxyConfiguration build(DataSource dataSource) {
        if(dataSource == null) {
            throw new BucketExceptions.BucketExecutionException("DataSource cannot be null");
        }
        if(lockBasedTransactionType == null) {
            this.lockBasedTransactionType = LockBasedTransactionType.ADVISORY;
        }
        if(tableSettings == null) {
            this.tableSettings = BucketTableSettings.getDefault();
        }
        if(clientSideConfig == null) {
            this.clientSideConfig = ClientSideConfig.getDefault();
        }
        return new PostgreSQLProxyConfiguration(dataSource, clientSideConfig, tableSettings, lockBasedTransactionType);
    }
}
