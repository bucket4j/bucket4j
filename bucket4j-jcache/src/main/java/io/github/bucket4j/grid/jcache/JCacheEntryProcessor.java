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
import io.github.bucket4j.Nothing;
import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.remote.CommandResult;
import io.github.bucket4j.remote.RemoteBucketState;
import io.github.bucket4j.remote.RemoteCommand;

import javax.cache.processor.EntryProcessor;
import java.io.Serializable;


public abstract class JCacheEntryProcessor<K extends Serializable, T extends Serializable> implements Serializable, EntryProcessor<K, RemoteBucketState, CommandResult<T>> {

    private final Long clientSideTimeNanos;

    protected JCacheEntryProcessor(Long clientSideTimeNanos) {
        this.clientSideTimeNanos = clientSideTimeNanos;
    }

    public static <K extends Serializable> JCacheEntryProcessor<K, Nothing> initStateProcessor(BucketConfiguration configuration, Long clientSideTimeNanos) {
        return new InitStateProcessor<>(configuration, clientSideTimeNanos);
    }

    public static <K extends Serializable, T extends Serializable> JCacheEntryProcessor<K, T> executeProcessor(RemoteCommand<T> targetCommand, Long clientSideTimeNanos) {
        return new ExecuteProcessor<>(targetCommand, clientSideTimeNanos);
    }

    public static <K extends Serializable, T extends Serializable> JCacheEntryProcessor<K, T> initStateAndExecuteProcessor(RemoteCommand<T> targetCommand, BucketConfiguration configuration, Long clientSideTimeNanos) {
        return new InitStateAndExecuteProcessor<>(targetCommand, configuration, clientSideTimeNanos);
    }

    public long currentTimeNanos() {
        if (clientSideTimeNanos != null) {
            return clientSideTimeNanos;
        } else {
            return System.currentTimeMillis() * 1_000_000;
        }
    }

}
