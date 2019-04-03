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
import io.github.bucket4j.grid.jcache.JCacheEntryProcessor;
import io.github.bucket4j.remote.CommandResult;
import io.github.bucket4j.remote.RemoteBucketState;

import java.io.Serializable;


class CoherenceEntryProcessorAdapter<K extends Serializable, T extends Serializable> extends AbstractProcessor<K, RemoteBucketState, CommandResult<?>> {

    private static final long serialVersionUID = 1L;

    private final JCacheEntryProcessor<K, T> entryProcessor;

    public CoherenceEntryProcessorAdapter(JCacheEntryProcessor<K, T> entryProcessor) {
        this.entryProcessor = entryProcessor;
    }

    @Override
    public CommandResult<?> process(InvocableMap.Entry<K, RemoteBucketState> entry) {
        CoherenceMutableEntryAdapter<K> entryAdapter = new CoherenceMutableEntryAdapter<>(entry);
        return entryProcessor.process(entryAdapter);
    }

}
