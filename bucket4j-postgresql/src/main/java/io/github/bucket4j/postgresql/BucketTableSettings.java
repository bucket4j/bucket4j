package io.github.bucket4j.postgresql;

import java.util.Objects;


public class BucketTableSettings {

    private final String idName;

    private final String stateName;

    private final String tableName;

    private BucketTableSettings(String tableName, String idName, String stateName) {
        this.tableName = Objects.requireNonNull(tableName, "TableName is null");;
        this.idName = Objects.requireNonNull(idName, "idName is null");
        this.stateName = Objects.requireNonNull(stateName, "StateName is null");
    }

    public static BucketTableSettings customSettings(String tableName, String idName, String stateName){
        return new BucketTableSettings(tableName, idName, stateName);
    }

    public static BucketTableSettings defaultSettings(){
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
