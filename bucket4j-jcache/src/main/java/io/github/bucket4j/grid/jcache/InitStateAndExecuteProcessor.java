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
import io.github.bucket4j.grid.CommandResult;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.GridCommand;

import javax.cache.processor.MutableEntry;
import java.io.Serializable;

public class InitStateAndExecuteProcessor<K extends Serializable, T extends Serializable> implements JCacheEntryProcessor<K, T> {

    private static final long serialVersionUID = 1L;

    private GridCommand<T> targetCommand;
    private BucketConfiguration configuration;

    public InitStateAndExecuteProcessor(GridCommand<T> targetCommand, BucketConfiguration configuration) {
        this.configuration = configuration;
        this.targetCommand = targetCommand;
    }

    @Override
    public CommandResult<T> process(MutableEntry<K, GridBucketState> mutableEntry, Object... arguments) {
        boolean newStateCreated = false;
        long currentTimeNanos = currentTimeNanos();
        GridBucketState gridBucketState;
        if (mutableEntry.exists()) {
            gridBucketState = mutableEntry.getValue();
        } else {
            BucketState bucketState = BucketState.createInitialState(configuration, currentTimeNanos);
            gridBucketState = new GridBucketState(configuration, bucketState);
            newStateCreated = true;
        }

        T result = targetCommand.execute(gridBucketState, currentTimeNanos);
        if (newStateCreated || targetCommand.isBucketStateModified()) {
            mutableEntry.setValue(gridBucketState);
        }
        return CommandResult.success(result);
    }

}
