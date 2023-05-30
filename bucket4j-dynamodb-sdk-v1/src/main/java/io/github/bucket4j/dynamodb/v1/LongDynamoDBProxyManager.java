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
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AsyncCompareAndSwapOperation;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.CompareAndSwapOperation;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * {@link BaseDynamoDBProxyManager} implementation that uses {@link Long} as key.
 */
final class LongDynamoDBProxyManager extends BaseDynamoDBProxyManager<Long> {
    LongDynamoDBProxyManager(AmazonDynamoDB db, String table, ClientSideConfig config) {
        super(db, table, config);
    }

    @Override
    protected CompareAndSwapOperation beginCompareAndSwapOperation(Long key) {
        return new NumberDynamoDBTransaction(db, table, key);
    }

    @Override
    protected AsyncCompareAndSwapOperation beginAsyncCompareAndSwapOperation(Long key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeProxy(Long key) {
        Map<String, AttributeValue> attrs = new HashMap<>();
        attrs.put(Constants.Attrs.DEFAULT_KEY_NAME, new AttributeValue().withN(key.toString()));

        db.deleteItem(table, attrs);
    }

    @Override
    public boolean isAsyncModeSupported() {
        return false;
    }

    @Override
    protected CompletableFuture<?> removeAsync(Long key) {
        throw new UnsupportedOperationException();
    }
}
