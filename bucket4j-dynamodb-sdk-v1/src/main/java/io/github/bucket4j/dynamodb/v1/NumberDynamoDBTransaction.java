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

import java.util.Objects;

/**
 * {@link BaseDynamoDBTransaction} implementation that maps {@link Number}
 * to DynamoDB's {@code N} type.
 */
final class NumberDynamoDBTransaction extends BaseDynamoDBTransaction {
    private final Number key;

    public NumberDynamoDBTransaction(AmazonDynamoDB db, String table, Number key) {
        super(db, table);
        this.key = Objects.requireNonNull(key, "key is null");
    }

    @Override
    protected AttributeValue getKeyAttributeValue() {
        return new AttributeValue().withN(key.toString());
    }
}
