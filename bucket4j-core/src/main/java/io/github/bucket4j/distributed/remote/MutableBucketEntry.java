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

package io.github.bucket4j.distributed.remote;

import java.util.Objects;

import io.github.bucket4j.distributed.versioning.Version;

import static io.github.bucket4j.distributed.serialization.InternalSerializationHelper.deserializeState;
import static io.github.bucket4j.distributed.serialization.InternalSerializationHelper.serializeState;

public class MutableBucketEntry {

    private RemoteBucketState state;
    private boolean stateModified;

    public MutableBucketEntry(RemoteBucketState state) {
        this.state = state;
    }

    public MutableBucketEntry(byte[] originalStateBytes) {
        this.state = originalStateBytes == null? null : deserializeState(originalStateBytes);
    }

    public boolean exists() {
        return state != null;
    }

    public boolean isStateModified() {
        return stateModified;
    }

    public void set(RemoteBucketState state) {
        this.state = Objects.requireNonNull(state);
        this.stateModified = true;
    }

    public RemoteBucketState get() {
        if (state == null) {
            throw new IllegalStateException("'exists' must be called before 'get'");
        }
        return state;
    }

    public byte[] getStateBytes(Version backwardCompatibilityVersion) {
        return serializeState(get(), backwardCompatibilityVersion);
    }

}
