/*
 *
 *   Copyright 2015-2017 Vladimir Bukhtoyarov
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.github.bucket4j.grid.coherence;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.processor.AbstractProcessor;
import io.github.bucket4j.grid.CommandResult;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.jcache.JCacheEntryProcessor;
import io.github.bucket4j.serialization.DeserializationAdapter;
import io.github.bucket4j.serialization.SerializationAdapter;
import io.github.bucket4j.serialization.SerializationHandle;

import java.io.IOException;
import java.io.Serializable;


public class CoherenceEntryProcessorAdapter<K extends Serializable, T extends Serializable> extends AbstractProcessor<K, GridBucketState, CommandResult<?>> {

    private static final long serialVersionUID = 1L;

    private final JCacheEntryProcessor<K, T> entryProcessor;

    public CoherenceEntryProcessorAdapter(JCacheEntryProcessor<K, T> entryProcessor) {
        this.entryProcessor = entryProcessor;
    }

    public static SerializationHandle<CoherenceEntryProcessorAdapter> SERIALIZATION_HANDLE = new SerializationHandle<CoherenceEntryProcessorAdapter>() {

        @Override
        public <I> CoherenceEntryProcessorAdapter deserialize(DeserializationAdapter<I> adapter, I input) throws IOException {
            JCacheEntryProcessor entryProcessor = (JCacheEntryProcessor) adapter.readObject(input);
            return new CoherenceEntryProcessorAdapter(entryProcessor);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, CoherenceEntryProcessorAdapter serializableObject) throws IOException {
            adapter.writeObject(output, serializableObject.entryProcessor);
        }

        @Override
        public int getTypeId() {
            return 22;
        }

        @Override
        public Class<CoherenceEntryProcessorAdapter> getSerializedType() {
            return CoherenceEntryProcessorAdapter.class;
        }
    };

    @Override
    public CommandResult<?> process(InvocableMap.Entry<K, GridBucketState> entry) {
        CoherenceMutableEntryAdapter<K> entryAdapter = new CoherenceMutableEntryAdapter<>(entry);
        return entryProcessor.process(entryAdapter);
    }

    public JCacheEntryProcessor<K, T> getEntryProcessor() {
        return entryProcessor;
    }

}
