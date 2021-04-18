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
import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.CompareAndSwapOperation;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

abstract class BaseDynamoDBTransaction implements CompareAndSwapOperation {
    private final AmazonDynamoDB db;
    private final String table;
    // NOTE: hardcode for now. Allow customization?
    private final String keyAttr = Constants.Attrs.DEFAULT_KEY_NAME;
    private final String stateAttr = Constants.Attrs.DEFAULT_STATE_NAME;

    protected BaseDynamoDBTransaction(AmazonDynamoDB db, String table) {
        this.db = Objects.requireNonNull(db, "DynamoDB is null");
        this.table = Objects.requireNonNull(table, "table name is null");
    }

    @Override
    public final Optional<byte[]> getStateData() {
        Map<String, AttributeValue> attrs = new HashMap<>();
        attrs.put(keyAttr, getKeyAttributeValue());

        GetItemRequest request = new GetItemRequest()
                .withTableName(table)
                .withKey(attrs)
                .withConsistentRead(true);

        Map<String, AttributeValue> result = db.getItem(request).getItem();
        // per docs if item is not found, response will not contain item object
        // at all. Based on this, check for emptiness seems redundant.
        //
        // docs: https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_GetItem.html
        // > If there is no matching item, GetItem does not return any data and there
        // > will be no Item element in the response.
        if (result == null || !result.containsKey(stateAttr)) {
            return Optional.empty();
        }

        AttributeValue state = result.get(stateAttr);
        if (state.getB() == null) {
            throw new IllegalStateException(
                    "state (attribute: " + stateAttr + ") value is corrupted for key " +
                    getKeyAttributeValue() +
                    ". It is present but value type is different from Binary (B) type. " +
                    "Current state value is " + state
            );
        }

        return Optional.of(ItemUtils.toSimpleValue(state));
    }

    @Override
    public final boolean compareAndSwap(byte[] originalData, byte[] newData) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put(keyAttr, getKeyAttributeValue());
        item.put(stateAttr, new AttributeValue().withB(ByteBuffer.wrap(newData)));

        Map<String, Object> attrs = Collections.singletonMap(":expected", originalData);

        // to be safe as there are reserved words in DynamoDB
        Map<String, String> names = Collections.singletonMap("#st", stateAttr);

        PutItemRequest request = new PutItemRequest()
                .withTableName(table)
                .withItem(item)
                .withConditionExpression("attribute_not_exists(#st) OR #st = :expected")
                .withExpressionAttributeNames(names)
                .withExpressionAttributeValues(ItemUtils.fromSimpleMap(attrs));

        try {
            db.putItem(request);
            return true;
        } catch (ConditionalCheckFailedException e) {
            return false;
        }
    }
    /**
     * @return key wrapped in {@link AttributeValue}. Type of {@link AttributeValue}
     * depends on implementation.
     */
    protected abstract AttributeValue getKeyAttributeValue();
}
