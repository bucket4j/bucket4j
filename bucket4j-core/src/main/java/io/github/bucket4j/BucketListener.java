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

package io.github.bucket4j;

/**
 * Interface for listening bucket related events. The typical use-cases of this interface are logging and monitoring.
 * The bucket can be decorated by listener via {@link Bucket#toListenable(BucketListener)} method.
 *
 * <h3>Question: How many listeners is need to create in case of application uses many buckets?</h3>
 * <b>Answer:</b>  it depends:
 * <ul>
 *     <li>If you want to have aggregated statistics for all buckets then create single listener per application and reuse this listener for all buckets.</li>
 *     <li>If you want to measure statistics independently per each bucket then use listener per bucket model.</li>
 * </ul>
 *
 * <h3>Question: where is methods of listener are invoking in case of distributed usage?</h3>
 * <b>Answer:</b> listener always invoked on client side, it is means that each client JVM will have own totally independent  for same bucket.
 *
 * <h3>Question: Why does bucket invoke the listener on client side instead of server side in case of distributed scenario?
 * What I need to do if I need in aggregated stat across the whole cluster?</h3>
 * <b>Answer:</b> Because of planned expansion to non-JVM back-ends such as Redis, MySQL, PostgreSQL.
 * It is not possible to serialize and invoke listener on this non-java back-ends, so it was decided to invoke listener on client side,
 * in order to avoid inconsistency between different back-ends in the future.
 * You can do post-aggregation of monitoring statistics via features built-into your monitoring database or via mediator(like StatsD) between your application and monitoring database.
 *
 * @see SimpleBucketListener
 */
public interface BucketListener {

    /**
     * This method is called whenever {@code tokens} is consumed.
     *
     * @param tokens amount of tokens that consumed
     */
    void onConsumed(long tokens);

    /**
     * This method is called whenever consumption request for {@code tokens} is rejected.
     *
     * @param tokens amount of tokens that rejected
     */
    void onRejected(long tokens);

    /**
     * This method is called each time when thread was parked for wait of tokens refill
     * in result of interaction with {@link BlockingBucket}
     *
     * @param nanos amount of nanoseconds for which thread was parked
     */
    void onParked(long nanos);

    /**
     * This method is called each time when thread was interrupted during the wait of tokens refill
     * in result of interaction with {@link BlockingBucket}
     *
     * @param e InterruptedException
     */
    void onInterrupted(InterruptedException e);

    /**
     * This method is called each time when delayed task was submit to {@link java.util.concurrent.ScheduledExecutorService} because of wait for tokens refill
     * in result of interaction with {@link AsyncScheduledBucket}
     *
     * @param nanos amount of nanoseconds for which thread was parked
     */
    void onDelayed(long nanos);

    /**
     * The default listener that do nothing.
     */
    BucketListener NOPE = new BucketListener() {
        @Override
        public void onConsumed(long tokens) {
            // do nothing
        }

        @Override
        public void onRejected(long tokens) {
            // do nothing
        }

        @Override
        public void onDelayed(long nanos) {
            // do nothing
        }

        @Override
        public void onParked(long nanos) {
            // do nothing
        }

        @Override
        public void onInterrupted(InterruptedException e) {
            // do nothing
        }
    };

}
