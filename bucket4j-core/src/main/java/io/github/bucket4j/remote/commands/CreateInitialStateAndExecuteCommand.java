/*
 *
 * Copyright 2015-2019 Vladimir Bukhtoyarov
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

package io.github.bucket4j.remote.commands;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.BucketState;
import io.github.bucket4j.remote.CommandResult;
import io.github.bucket4j.remote.MutableBucketEntry;
import io.github.bucket4j.remote.RemoteBucketState;
import io.github.bucket4j.remote.RemoteCommand;

import java.io.Serializable;

public class CreateInitialStateAndExecuteCommand<T extends Serializable> implements RemoteCommand<T> {

    private static final long serialVersionUID = 1;

    private final RemoteCommand<T> targetCommand;
    private BucketConfiguration configuration;

    public CreateInitialStateAndExecuteCommand(BucketConfiguration configuration, RemoteCommand<T> targetCommand) {
        this.configuration = configuration;
        this.targetCommand = targetCommand;
    }

    @Override
    public CommandResult<T> execute(MutableBucketEntry mutableEntry, long currentTimeNanos) {
        RemoteBucketState state;
        if (mutableEntry.exists()) {
            state = mutableEntry.get();
        } else {
            BucketState bucketState = BucketState.createInitialState(configuration, currentTimeNanos);
            state = new RemoteBucketState(configuration, bucketState);
        }

        BucketEntryWrapper entryWrapper = new BucketEntryWrapper(state);
        CommandResult<T> result = targetCommand.execute(entryWrapper, currentTimeNanos);
        mutableEntry.set(entryWrapper.get());
        return result;
    }

    public BucketConfiguration getConfiguration() {
        return configuration;
    }

    public RemoteCommand<T> getTargetCommand() {
        return targetCommand;
    }


    private static class BucketEntryWrapper implements MutableBucketEntry {

        private RemoteBucketState state;

        public BucketEntryWrapper(RemoteBucketState state) {
            this.state = state;
        }

        @Override
        public boolean exists() {
            return true;
        }

        @Override
        public void set(RemoteBucketState state) {
            this.state = state;
        }

        @Override
        public RemoteBucketState get() {
            return state;
        }
    }

}
