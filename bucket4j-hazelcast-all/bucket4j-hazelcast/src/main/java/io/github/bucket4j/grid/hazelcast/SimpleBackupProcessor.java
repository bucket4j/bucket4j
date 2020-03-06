/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2020 Vladimir Bukhtoyarov
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

package io.github.bucket4j.grid.hazelcast;

import com.hazelcast.map.EntryProcessor;
import io.github.bucket4j.grid.CommandResult;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.serialization.DeserializationAdapter;
import io.github.bucket4j.serialization.SerializationAdapter;
import io.github.bucket4j.serialization.SerializationHandle;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;


public class SimpleBackupProcessor<K extends Serializable> implements EntryProcessor<K, GridBucketState, Object> {

    private static final long serialVersionUID = 1L;

    private final GridBucketState state;

    public SimpleBackupProcessor(GridBucketState state) {
        this.state = state;
    }

    @Override
    public Object process(Map.Entry<K, GridBucketState> entry) {
        entry.setValue(state);
        return null; // return value from backup processor is ignored, see https://github.com/hazelcast/hazelcast/pull/14995
    }

    public static SerializationHandle<SimpleBackupProcessor> SERIALIZATION_HANDLE = new SerializationHandle<SimpleBackupProcessor>() {

        @Override
        public <I> SimpleBackupProcessor deserialize(DeserializationAdapter<I> adapter, I input) throws IOException {
            GridBucketState state = adapter.readObject(input, GridBucketState.class);
            return new SimpleBackupProcessor(state);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, SimpleBackupProcessor processor) throws IOException {
            adapter.writeObject(output, processor.state);
        }

        @Override
        public int getTypeId() {
            return 20;
        }

        @Override
        public Class<SimpleBackupProcessor> getSerializedType() {
            return SimpleBackupProcessor.class;
        }
    };

}
