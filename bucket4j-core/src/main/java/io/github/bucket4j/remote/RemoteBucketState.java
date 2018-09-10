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

package io.github.bucket4j.remote;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.BucketState;

import java.io.Serializable;

public class RemoteBucketState implements Serializable {

    private static final long serialVersionUID = 42;

    private BucketConfiguration configuration;
    private BucketState state;

    public RemoteBucketState(BucketConfiguration configuration, BucketState state) {
        this.configuration = configuration;
        this.state = state;
    }

    public RemoteBucketState deepCopy() {
        return new RemoteBucketState(configuration, state.copy());
    }

    public void refillAllBandwidth(long currentTimeNanos) {
        state.refillAllBandwidth(configuration.getBandwidths(), currentTimeNanos);
    }

    public long getAvailableTokens() {
        return state.getAvailableTokens(configuration.getBandwidths());
    }

    public void consume(long tokensToConsume) {
        state.consume(configuration.getBandwidths(), tokensToConsume);
    }

    public long calculateDelayNanosAfterWillBePossibleToConsume(long tokensToConsume, long currentTimeNanos) {
        return state.calculateDelayNanosAfterWillBePossibleToConsume(configuration.getBandwidths(), tokensToConsume, currentTimeNanos);
    }

    public void addTokens(long tokensToAdd) {
        state.addTokens(configuration.getBandwidths(), tokensToAdd);
    }

    public BucketState copyBucketState() {
        return state.copy();
    }

    public BucketConfiguration replaceConfigurationOrReturnPrevious(BucketConfiguration newConfiguration) {
        if (!configuration.isCompatible(newConfiguration)) {
            return configuration;
        }
        configuration = newConfiguration;
        return null;
    }

    public BucketConfiguration getConfiguration() {
        return configuration;
    }

    public BucketState getState() {
        return state;
    }

}
