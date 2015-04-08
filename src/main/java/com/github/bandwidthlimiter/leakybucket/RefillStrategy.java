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
package com.github.bandwidthlimiter.leakybucket;

/**
 * Encapsulation of a refilling strategy for a leaky bucket.
 */
public interface RefillStrategy {

    void setupInitialState(LeakyBucketConfiguration configuration, LeakyBucketState state, long currentTime);

    void refill(LeakyBucketConfiguration configuration, LeakyBucketState state, long currentTime);
    
    long timeRequiredToRefill(LeakyBucketConfiguration configuration, int bandwidthIndex, long numTokens);

    int sizeOfState(LeakyBucketConfiguration configuration);

}