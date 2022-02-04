package io.github.bucket4j.postgresql;

import io.github.bucket4j.distributed.proxy.ClientSideConfig;

import javax.sql.DataSource;

public class PostgreSQLProxyConfiguration {

    private final String idName;

    private final String stateName;

    private final String tableName;

    private final DataSource dataSource;

    private final ClientSideConfig clientSideConfig;

    public PostgreSQLProxyConfiguration(DataSource dataSource) {
        this(dataSource, "bucket");
    }

    public PostgreSQLProxyConfiguration(DataSource dataSource, String tableName) {
        this(dataSource, ClientSideConfig.getDefault(), tableName);
    }

    public PostgreSQLProxyConfiguration(DataSource dataSource, ClientSideConfig clientSideConfig, String tableName) {
        this(dataSource, clientSideConfig, tableName, "id", "state");
    }

    public PostgreSQLProxyConfiguration(DataSource dataSource, ClientSideConfig clientSideConfig, String tableName, String idName, String stateName) {
        this.dataSource = dataSource;
        this.clientSideConfig = clientSideConfig;
        this.tableName = tableName;
        this.idName = idName;
        this.stateName = stateName;
    }

    public String getIdName() {
        return idName;
    }

    public String getStateName() {
        return stateName;
    }

    public String getTableName() {
        return tableName;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public ClientSideConfig getClientSideConfig() {
        return clientSideConfig;
    }
}
