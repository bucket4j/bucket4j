/*
 *
 * Copyright 2015-2018 Vladimir Bukhtoyarov
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package io.github.bucket4j.grid.hazelcast;

import com.hazelcast.map.EntryBackupProcessor;
import com.hazelcast.map.EntryProcessor;
import io.github.bucket4j.grid.jcache.JCacheEntryProcessor;
import io.github.bucket4j.remote.CommandResult;
import io.github.bucket4j.remote.RemoteBucketState;

import java.io.Serializable;
import java.util.Map;


class HazelcastEntryProcessorAdapter<K extends Serializable, T extends Serializable> implements EntryProcessor<K, RemoteBucketState> {

    private static final long serialVersionUID = 1L;

    private final JCacheEntryProcessor<K, T> entryProcessor;
    private EntryBackupProcessor<K, RemoteBucketState> backupProcessor;

    public HazelcastEntryProcessorAdapter(JCacheEntryProcessor<K, T> entryProcessor) {
        this.entryProcessor = entryProcessor;
    }

    @Override
    public Object process(Map.Entry<K, RemoteBucketState> entry) {
        HazelcastMutableEntryAdapter<K> entryAdapter = new HazelcastMutableEntryAdapter<>(entry);
        CommandResult<T> result = entryProcessor.process(entryAdapter);
        if (entryAdapter.isModified()) {
            RemoteBucketState state = entry.getValue();
            backupProcessor = new SimpleBackupProcessor<>(state);
        }
        return result;
    }

    @Override
    public EntryBackupProcessor<K, RemoteBucketState> getBackupProcessor() {
        return backupProcessor;
    }

}
