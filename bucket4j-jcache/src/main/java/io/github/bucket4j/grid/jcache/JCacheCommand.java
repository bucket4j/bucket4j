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

import io.github.bucket4j.grid.CommandResult;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.GridCommand;

import javax.cache.processor.EntryProcessor;
import javax.cache.processor.MutableEntry;
import java.io.Serializable;


public class JCacheCommand<K, T extends Serializable> implements Serializable, EntryProcessor<K, GridBucketState, CommandResult>, Serializable {

    @Override
    public CommandResult<T> process(MutableEntry<K, GridBucketState> mutableEntry, Object... arguments) {
        if (!mutableEntry.exists()) {
            return CommandResult.bucketNotFound();
        }

        GridCommand<T> targetCommand = (GridCommand<T>) arguments[0];
        GridBucketState state = mutableEntry.getValue();
        T result = targetCommand.execute(state);
        if (targetCommand.isBucketStateModified()) {
            mutableEntry.setValue(state);
        }
        return CommandResult.success(result);
    }

}
