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

package io.github.bucket4j.remote.commands;


import io.github.bucket4j.remote.RemoteBucketState;
import io.github.bucket4j.remote.RemoteCommand;

public class GetAvailableTokensCommand implements RemoteCommand<Long> {

    private static final long serialVersionUID = 42;

    private Long clientTimeNanos;

    public GetAvailableTokensCommand(Long clientTimeNanos) {
        this.clientTimeNanos = clientTimeNanos;
    }

    @Override
    public Long execute(RemoteBucketState state) {
        state.refillAllBandwidth(currentTimeNanos());
        return state.getAvailableTokens();
    }

    @Override
    public boolean isBucketStateModified() {
        return false;
    }

    @Override
    public Long getClientTimeNanos() {
        return clientTimeNanos;
    }

}
