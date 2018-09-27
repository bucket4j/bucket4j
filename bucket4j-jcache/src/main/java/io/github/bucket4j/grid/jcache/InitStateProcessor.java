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

package io.github.bucket4j.grid.jcache;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.BucketState;
import io.github.bucket4j.Nothing;
import io.github.bucket4j.remote.CommandResult;
import io.github.bucket4j.remote.RemoteBucketState;

import javax.cache.processor.MutableEntry;
import java.io.Serializable;

public class InitStateProcessor<K extends Serializable> implements JCacheEntryProcessor<K, Nothing> {

    private static final long serialVersionUID = 1;

    private BucketConfiguration configuration;
    private final Long clientTimeNanos;

    public InitStateProcessor(BucketConfiguration configuration, Long clientTimeNanos) {
        this.configuration = configuration;
        this.clientTimeNanos = clientTimeNanos;
    }

    @Override
    public CommandResult<Nothing> process(MutableEntry<K, RemoteBucketState> mutableEntry, Object... arguments) {
        if (mutableEntry.exists()) {
            return CommandResult.success(null);
        }
        long currentTimeNanos = currentTimeNanos();
        BucketState bucketState = BucketState.createInitialState(configuration, currentTimeNanos);
        RemoteBucketState remoteBucketState = new RemoteBucketState(configuration, bucketState);
        mutableEntry.setValue(remoteBucketState);
        return CommandResult.success(null);
    }

    private long currentTimeNanos() {
        return clientTimeNanos != null? clientTimeNanos : System.currentTimeMillis() * 1_000_000;
    }

}
