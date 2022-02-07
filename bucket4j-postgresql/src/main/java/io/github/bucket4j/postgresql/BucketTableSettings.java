package io.github.bucket4j.postgresql;

import java.util.Objects;

/**
 * @author Maxim Bartkov
 * The class to define a configuration of the table to use as a Buckets store
 */
public class BucketTableSettings {

    private final String idName;

    private final String stateName;

    private final String tableName;

    private BucketTableSettings(String tableName, String idName, String stateName) {
        this.tableName = Objects.requireNonNull(tableName, "TableName is null");;
        this.idName = Objects.requireNonNull(idName, "idName is null");
        this.stateName = Objects.requireNonNull(stateName, "StateName is null");
    }

    /**
     * The method will define a custom configuration of the table for the work with the PostgreSQL extension
     * @param tableName - name of table to use as a Buckets store
     * @param idName - name of id (PRIMARY KEY - BIGINT)
     * @param stateName - name of state (BYTEA)
     * @return {@link BucketTableSettings}
     */
    public static BucketTableSettings customSettings(String tableName, String idName, String stateName){
        return new BucketTableSettings(tableName, idName, stateName);
    }

    /**
     * The method will define the default configuration of the table for the work with the PostgreSQL extension
     * @return {@link BucketTableSettings} with the default setting, which includes "bucket" as the table name, "id" as the id column, "state" as the state column
     */
    public static BucketTableSettings getDefault(){
        return new BucketTableSettings("bucket", "id", "state");
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
}
