/*
 * Copyright 2015 Vladimir Bukhtoyarov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.github.bandwidthlimiter;

import com.github.bandwidthlimiter.bucket.BucketConfiguration;
import com.github.bandwidthlimiter.bucket.BucketState;
import com.github.bandwidthlimiter.bucket.local.UnsafeBucket;

public class SynchronizedBucket extends UnsafeBucket {

    private final BucketState state;

    public SynchronizedBucket(BucketConfiguration configuration) {
        super(configuration);
        this.state = BucketState.createInitialState(configuration);
    }

    @Override
    synchronized protected long consumeAsMuchAsPossibleImpl(long limit) {
        return super.consumeAsMuchAsPossibleImpl(limit);
    }

    @Override
    synchronized protected boolean tryConsumeImpl(long tokensToConsume) {
        return super.tryConsumeImpl(tokensToConsume);
    }

    @Override
    synchronized protected boolean consumeOrAwaitImpl(long tokensToConsume, long waitIfBusyTimeLimit) throws InterruptedException {
        return super.consumeOrAwaitImpl(tokensToConsume, waitIfBusyTimeLimit);
    }

    @Override
    synchronized public BucketState createSnapshot() {
        return super.createSnapshot();
    }

}