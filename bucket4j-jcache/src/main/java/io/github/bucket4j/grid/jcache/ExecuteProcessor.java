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

import io.github.bucket4j.remote.CommandResult;
import io.github.bucket4j.remote.RemoteBucketState;
import io.github.bucket4j.remote.RemoteCommand;

import javax.cache.processor.MutableEntry;
import java.io.Serializable;

public class ExecuteProcessor<K extends Serializable, T extends Serializable> implements JCacheEntryProcessor<K, T> {

    private static final long serialVersionUID = 1;

    private RemoteCommand<T> targetCommand;

    public ExecuteProcessor(RemoteCommand<T> targetCommand) {
        this.targetCommand = targetCommand;
    }

    @Override
    public CommandResult<T> process(MutableEntry<K, RemoteBucketState> mutableEntry, Object... arguments) {
        if (!mutableEntry.exists()) {
            return CommandResult.bucketNotFound();
        }

        RemoteBucketState remoteBucketState = mutableEntry.getValue();

        T result = targetCommand.execute(remoteBucketState);
        if (targetCommand.isBucketStateModified()) {
            mutableEntry.setValue(remoteBucketState);
        }
        return CommandResult.success(result);
    }

}
