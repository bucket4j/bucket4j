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
package io.github.bucket4j.distributed.jdbc;

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
        this.tableName = Objects.requireNonNull(tableName, "TableName is null");
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
