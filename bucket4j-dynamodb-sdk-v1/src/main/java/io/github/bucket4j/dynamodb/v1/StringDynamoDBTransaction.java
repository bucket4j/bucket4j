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

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * {@link BaseDynamoDBTransaction} that maps {@link String} to DynamoDB's
 * {@code S} type.
 * <p>
 * <b>NOTE</b>: {@code key} is limited to 2048 bytes (in {@link StandardCharsets#UTF_8})
 * to conform DynamoDB primary key requirements.
 */
final class StringDynamoDBTransaction extends BaseDynamoDBTransaction {
    // see String notes for primary key at
    // https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes
    private static final int MAX_SUPPORTED_LENGTH = 2048;
    private final String key;

    public StringDynamoDBTransaction(AmazonDynamoDB db, String table, String key) {
        super(db, table);

        Objects.requireNonNull(key, "key is null");
        if (key.isEmpty()) {
            throw new IllegalArgumentException("key is empty");
        }

        int length = key.getBytes(StandardCharsets.UTF_8).length;
        if (length > MAX_SUPPORTED_LENGTH) {
            throw new IllegalArgumentException(
                    "key " + key + " has length of " + length + " bytes " +
                    "while max allowed is " + MAX_SUPPORTED_LENGTH + " bytes"
            );
        }

        this.key = key;
    }

    @Override
    protected AttributeValue getKeyAttributeValue() {
        return new AttributeValue().withS(key);
    }
}
