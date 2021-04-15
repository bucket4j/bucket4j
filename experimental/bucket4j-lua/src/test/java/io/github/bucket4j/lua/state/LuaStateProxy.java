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

package io.github.bucket4j.lua.state;

import io.github.bucket4j.*;
import io.github.bucket4j.local.LocalBucket;
import io.github.bucket4j.lua.Bucket4jScript;
import org.classdump.luna.Table;

public class LuaStateProxy implements BucketState {

    private final Bucket4jScript script = new Bucket4jScript();
    private final TimeMeter timeMeter;
    private final Table state;

    public LuaStateProxy(LocalBucket bucket) {
        BucketConfiguration configuration = bucket.getConfiguration();
        this.timeMeter = bucket.getTimeMeter();
        this.state = script.createState(configuration, timeMeter.currentTimeNanos());
    }

    @Override
    public long getAvailableTokens(Bandwidth[] bandwidths) {
        return 0;
    }

    @Override
    public void consume(Bandwidth[] bandwidths, long toConsume) {

    }

    @Override
    public long calculateDelayNanosAfterWillBePossibleToConsume(Bandwidth[] bandwidths, long tokensToConsume, long currentTimeNanos) {
        return 0;
    }

    @Override
    public long calculateFullRefillingTime(Bandwidth[] bandwidths, long currentTimeNanos) {
        return 0;
    }

    @Override
    public void refillAllBandwidth(Bandwidth[] limits, long currentTimeNanos) {

    }

    @Override
    public void addTokens(Bandwidth[] bandwidths, long tokensToAdd) {

    }

    @Override
    public void forceAddTokens(Bandwidth[] bandwidths, long tokensToAdd) {

    }

    @Override
    public long getCurrentSize(int bandwidth) {
        return 0;
    }

    @Override
    public long getRoundingError(int bandwidth) {
        return 0;
    }

    @Override
    public MathType getMathType() {
        return MathType.IEEE_754;
    }

    @Override
    public BucketState copy() {
        throw new UnsupportedOperationException("is not needed for testing");
    }

    @Override
    public BucketState replaceConfiguration(BucketConfiguration previousConfiguration, BucketConfiguration newConfiguration, TokensInheritanceStrategy tokensInheritanceStrategy, long currentTimeNanos) {
        return null;
    }

    @Override
    public void copyStateFrom(BucketState sourceState) {
        throw new UnsupportedOperationException("is not needed for testing");
    }

}
