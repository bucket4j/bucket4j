/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2020 Vladimir Bukhtoyarov
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */

package io.github.bucket4j.local;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.BucketListener;
import io.github.bucket4j.MathType;
import io.github.bucket4j.TimeMeter;

/**
 * Defines the strategy of synchronization which need to be applied to prevent data-races in multithreading usage scenario.
 *
 */
public interface ConcurrencyStrategy {

    /**
     * Lock-free algorithm based on CAS(compare and swap) of immutable objects.
     *
     * <p>Advantages: This strategy is tolerant to high contention usage scenario, threads do not block each other.
     * <br>Disadvantages: The sequence "read-clone-update-save" needs to allocate one object per each invocation of consumption method.
     * <br>Usage recommendations: when you are not sure what kind of strategy is better for you.
     *
     * <p> The {@link LocalBucketBuilder#build()} without parameters uses this strategy.
     */
    ConcurrencyStrategy LOCK_FREE = new ConcurrencyStrategy() {
        @Override
        public LocalBucket createBucket(BucketConfiguration configuration, MathType mathType, TimeMeter timeMeter, BucketListener listener) {
            return new LockFreeBucket(configuration, MathType.INTEGER_64_BITS, timeMeter, listener);
        }
    };

    /**
     * This concurrency strategy based on <code>Reentrant lock</code>.
     *
     * <p>Advantages: Never allocates memory.
     * <br>Disadvantages: Thread which acquired the lock(and superseded from CPU by OS scheduler) can block another threads for significant time.
     * <br>Usage recommendations: when your primary goal is avoiding of memory allocation, and you do not care about contention.
     */
    ConcurrencyStrategy REENTRANT_LOCK_PROTECTED = new ConcurrencyStrategy() {
        @Override
        public LocalBucket createBucket(BucketConfiguration configuration, MathType mathType, TimeMeter timeMeter, BucketListener listener) {
            return new ReentrantLockProtectedBucket(configuration, MathType.INTEGER_64_BITS, timeMeter, listener);
        }
    };

    /**
     * This concurrency strategy based on java <code>synchronized</code> keyword.
     *
     * <p>Advantages: Never allocates memory.
     * <br>Disadvantages: Thread which acquired the lock(and superseded from CPU by OS scheduler) can block another threads for significant time.
     * <br>Usage recommendations: when your primary goal is avoiding of memory allocation, and you do not care about contention.
     */
    ConcurrencyStrategy SYNCHRONIZED = new ConcurrencyStrategy() {
        @Override
        public LocalBucket createBucket(BucketConfiguration configuration, MathType mathType, TimeMeter timeMeter, BucketListener listener) {
            return null;
        }
    };

    /**
     * This is fake strategy which does not perform synchronization at all.
     * It is usable when there are no multithreading access to same bucket,
     * or synchronization already provided by third-party library or yourself.
     *
     *
     * <p>Advantages: Never allocates memory and never acquires any locks, in other words you pay nothing for synchronization.
     * <br>Disadvantages: If your code or third-party library code has errors then bucket state will be corrupted.
     * <br>Usage recommendations: iff you have guarantees that bucket will never be used from multiple threads,
     * for example in cases where your third-party library(like akka or rx-java) prevents concurrent access and provide guarantees of visibility,
     * or when you are so senior guy that can manage synchronization by yourself.
     */
    ConcurrencyStrategy UNSAFE = new ConcurrencyStrategy() {
        @Override
        public LocalBucket createBucket(BucketConfiguration configuration, MathType mathType, TimeMeter timeMeter, BucketListener listener) {
            return new ThreadUnsafeBucket(configuration, MathType.INTEGER_64_BITS, timeMeter, listener);
        }
    };

    LocalBucket createBucket(BucketConfiguration configuration, MathType mathType, TimeMeter timeMeter, BucketListener listener);

}
