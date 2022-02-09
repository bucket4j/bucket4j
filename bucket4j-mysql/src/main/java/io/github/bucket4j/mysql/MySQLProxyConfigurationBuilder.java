package io.github.bucket4j.mysql;

import io.github.bucket4j.BucketExceptions;
import io.github.bucket4j.distributed.jdbc.BucketTableSettings;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;

import javax.sql.DataSource;

/**
 * @author Maxim Bartkov
 * The class to build {@link MySQLProxyConfiguration}
 */
public final class MySQLProxyConfigurationBuilder {
    private ClientSideConfig clientSideConfig;
    private BucketTableSettings tableSettings;

    private MySQLProxyConfigurationBuilder() {
    }

    public static MySQLProxyConfigurationBuilder builder() {
        return new MySQLProxyConfigurationBuilder();
    }

    /**
     * @param clientSideConfig {@link ClientSideConfig} client-side configuration for proxy-manager.
     *                                                 By default, under the hood uses {@link ClientSideConfig#getDefault}
     * @return {@link MySQLProxyConfigurationBuilder}
     */
    public MySQLProxyConfigurationBuilder addClientSideConfig(ClientSideConfig clientSideConfig) {
        this.clientSideConfig = clientSideConfig;
        return this;
    }

    /**
     * @param tableSettings {@link BucketTableSettings} define a configuration of the table to use as a Buckets store.
     *                                                 By default, under the hood uses {@link BucketTableSettings#getDefault}
     * @return {@link MySQLProxyConfigurationBuilder}
     */
    public MySQLProxyConfigurationBuilder addTableSettings(BucketTableSettings tableSettings) {
        this.tableSettings = tableSettings;
        return this;
    }

    /**
     * The method takes a {@link DataSource} as a required parameter
     * @param dataSource - a database configuration
     * @return {@link MySQLProxyConfiguration}
     */
    public MySQLProxyConfiguration build(DataSource dataSource) {
        if(dataSource == null) {
            throw new BucketExceptions.BucketExecutionException("DataSource cannot be null");
        }
        if(tableSettings == null) {
            this.tableSettings = BucketTableSettings.getDefault();
        }
        if(clientSideConfig == null) {
            this.clientSideConfig = ClientSideConfig.getDefault();
        }
        return new MySQLProxyConfiguration(dataSource, clientSideConfig, tableSettings);
    }
}
