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

package io.github.bucket4j.distributed.remote.commands;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.MutableBucketEntry;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.remote.RemoteCommand;


public class ReplaceConfigurationOrReturnPreviousCommand implements RemoteCommand<BucketConfiguration> {

    private static final long serialVersionUID = 42;

    private BucketConfiguration newConfiguration;

    public ReplaceConfigurationOrReturnPreviousCommand(BucketConfiguration newConfiguration) {
        this.newConfiguration = newConfiguration;
    }

    @Override
    public CommandResult<BucketConfiguration> execute(MutableBucketEntry mutableEntry, long currentTimeNanos) {
        if (!mutableEntry.exists()) {
            return CommandResult.bucketNotFound();
        }

        RemoteBucketState state = mutableEntry.get();
        state.refillAllBandwidth(currentTimeNanos);
        BucketConfiguration previousConfiguration = state.replaceConfigurationOrReturnPrevious(newConfiguration);
        if (previousConfiguration != null) {
            return CommandResult.success(previousConfiguration);
        }
        mutableEntry.set(state);
        return CommandResult.empty();
    }

    public BucketConfiguration getNewConfiguration() {
        return newConfiguration;
    }

}
