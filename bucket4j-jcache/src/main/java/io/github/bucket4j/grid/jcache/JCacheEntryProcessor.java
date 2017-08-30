
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

package io.github.bucket4j.grid.jcache;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.BucketState;
import io.github.bucket4j.Nothing;
import io.github.bucket4j.grid.CommandResult;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.GridCommand;

import javax.cache.processor.EntryProcessor;
import javax.cache.processor.MutableEntry;
import java.io.Serializable;


public class JCacheEntryProcessor<K extends Serializable, T extends Serializable> implements Serializable, EntryProcessor<K, GridBucketState, CommandResult<T>> {

    private final GridCommand<T> targetCommand;
    private final BucketConfiguration configuration;

    public static <K extends Serializable> JCacheEntryProcessor<K, Nothing> initStateProcessor(BucketConfiguration configuration) {
        return new JCacheEntryProcessor<>(null, configuration);
    }

    public static <K extends Serializable, T extends Serializable> JCacheEntryProcessor<K, T> executeProcessor(GridCommand<T> targetCommand) {
        return new JCacheEntryProcessor<>(targetCommand, null);
    }

    public static <K extends Serializable, T extends Serializable> JCacheEntryProcessor<K, T> initStateAndExecuteProcessor(GridCommand<T> targetCommand, BucketConfiguration configuration) {
        return new JCacheEntryProcessor<>(targetCommand, configuration);
    }

    private JCacheEntryProcessor(GridCommand<T> targetCommand, BucketConfiguration configuration) {
        this.targetCommand = targetCommand;
        this.configuration = configuration;
    }

    public GridCommand<T> getTargetCommand() {
        return targetCommand;
    }

    @Override
    public CommandResult<T> process(MutableEntry<K, GridBucketState> mutableEntry, Object... arguments) {
        long currentTimeNanos = currentTimeNanos();
        GridBucketState gridBucketState;
        if (mutableEntry.exists()) {
            gridBucketState = mutableEntry.getValue();
        } else {
            if (configuration == null) {
                return CommandResult.bucketNotFound();
            }
            BucketState bucketState = BucketState.createInitialState(configuration, currentTimeNanos);
            gridBucketState = new GridBucketState(configuration, bucketState);
            mutableEntry.setValue(gridBucketState);
        }

        T result = targetCommand.execute(gridBucketState, currentTimeNanos);
        if (targetCommand.isBucketStateModified()) {
            mutableEntry.setValue(gridBucketState);
        }
        return CommandResult.success(result);
    }

    private long currentTimeNanos() {
        return System.currentTimeMillis() * 1_000_000;
    }

}
