/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2021 Vladimir Bukhtoyarov
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
package io.github.bucket4j.dynamodb.v1;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AbstractCompareAndSwapBasedProxyManager;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.CompareAndSwapOperation;

import java.util.Objects;

/**
 * @param <K> type of key. {@code K} is unbound while DynamoDB supports only {@code String (S)},
 *            {@code Number (N)} or {@code Binary (B)} types for primary keys.
 * @see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.CoreComponents.html#HowItWorks.CoreComponents.PrimaryKey">
 * DynamoDB documentation for primary key.</a>
 */
abstract class BaseDynamoDBProxyManager<K> extends AbstractCompareAndSwapBasedProxyManager<K> {
    protected final AmazonDynamoDB db;
    protected final String table;

    protected BaseDynamoDBProxyManager(AmazonDynamoDB db, String table, ClientSideConfig config) {
        super(Objects.requireNonNull(config, "config is null"));
        this.db = Objects.requireNonNull(db, "DynamoDB is null");
        this.table = Objects.requireNonNull(table, "table name is null");
    }

    // NOTE: override exists only to expose method to tests.
    @Override
    protected abstract CompareAndSwapOperation beginCompareAndSwapOperation(K key);

}
