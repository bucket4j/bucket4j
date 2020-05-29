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

import io.github.bucket4j.distributed.remote.MutableBucketEntry;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.serialization.InternalSerializationHelper;

import java.util.Map;

import static io.github.bucket4j.serialization.InternalSerializationHelper.deserializeState;

class HazelcastMutableEntryAdapter<K> implements MutableBucketEntry {

    private final Map.Entry<K, byte[]> entry;
    private boolean modified;

    public HazelcastMutableEntryAdapter(Map.Entry<K, byte[]> entry) {
        this.entry = entry;
    }

    @Override
    public boolean exists() {
        return entry.getValue() != null;
    }

    @Override
    public void set(RemoteBucketState value) {
        byte[] stateBytes = InternalSerializationHelper.serializeState(value);
        entry.setValue(stateBytes);
        this.modified = true;
    }

    @Override
    public RemoteBucketState get() {
        byte[] stateBytes = entry.getValue();
        return deserializeState(stateBytes);
    }

    public boolean isModified() {
        return modified;
    }

}
