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

package io.github.bucket4j;

import java.io.Serializable;
import java.time.Duration;

/**
 * <h3>Anatomy of bandwidth:</h3>
 * The bandwidth is key building block for bucket.
 * The bandwidth consists from {@link #capacity} and {@link Refill refill}. Where:
 * <ul>
 *     <li><b>Capacity</b> - defines the maximum count of tokens which can be hold by bucket.</li>
 *     <li><b>Refill</b> - defines the speed in which tokens are regenerated in bucket.</li>
 * </ul>
 *
 * <h3>Classic and simple bandwidth definitions:</h3>
 * The bandwidth can be initialized in the two way:
 * <ul>
 *     <li>{@link #simple(long, Duration) Simple} - most popular way, which does not require from you to fully understand the token-bucket algorithm.
 *     Use this way when you just want to specify easy limitation <tt>N</tt> tokens per <tt>M</tt> time window.
 *     See <a href="https://github.com/vladimir-bukhtoyarov/bucket4j/blob/1.3/doc-pages/basic-usage.md#example-1---limiting-the-rate-of-heavy-work">this example</a> of usage.
 *     </li>
 *     <li>{@link #classic(long, Refill)} Classic} - hard way to specify limitation,
 *     use it when you want to utilize the whole power of token-bucket. See <a href="https://github.com/vladimir-bukhtoyarov/bucket4j/blob/1.3/doc-pages/basic-usage.md#example-3---limiting-the-rate-of-access-to-rest-api">this example</a> of usage.
 *     </li>
 * </ul>
 *
 * <h3>Multiple bandwidths:</h3>
 * Most likely you will use only one bandwidth per bucket,
 * but in general it is possible to specify more than one bandwidth per bucket,
 * and bucket will handle all bandwidth in strongly atomic way.
 * Strongly atomic means that token will be consumed from all bandwidth or from nothing,
 * in other words any token can not be partially consumed.
 * <br> Example of multiple bandwidth:
 * <pre>{@code // Adds bandwidth that restricts to consume not often 1000 tokens per 1 minute and not often than 100 tokens per second
 * Bucket bucket = Bucket4j.builder().
 *      .addLimit(Bandwidth.create(1000, Duration.ofMinutes(1)));
 *      .addLimit(Bandwidth.create(100, Duration.ofSeconds(1)));
 *      .build()
 * }</pre>
 */
public class Bandwidth implements Serializable {

    private static final long serialVersionUID = 42L;

    private static final long GREEDY_REFIL_INTERVAL = -1;

    final long capacity;
    long initialTokens;

    final long refillPeriodNanos;
    final long refillTokens;
    long refillRefreshIntervalNanos = GREEDY_REFIL_INTERVAL;

    private Bandwidth(long capacity, Refill refill) {
        if (capacity <= 0) {
            throw BucketExceptions.nonPositiveCapacity(capacity);
        }
        if (refill == null) {
            throw BucketExceptions.nullBandwidthRefill();
        }
        this.capacity = capacity;
        this.initialTokens = capacity;
        this.refillPeriodNanos = refill.getPeriodNanos();
        this.refillTokens = refill.getTokens();
    }

    /**
     * TODO javadocs
     *
     * @param initialTokens
     * @return
     */
    public Bandwidth withInitialTokens(long initialTokens) {
        if (initialTokens < 0) {
            throw BucketExceptions.nonPositiveInitialTokens(initialTokens);
        }
        this.initialTokens = initialTokens;
        return this;
    }

    /**
     * TODO fix this javadocs
     *
     * @param refillInterval
     * @return
     */
    /**
     * Creates refill which regenerates the tokens in greedy manner.
     *      *
     *      This factory method is called "smooth" because of refill created by this method will add tokens to bucket as soon as possible.
     *      *      * For example smooth refill "10 tokens per 1 second" will add 1 token per each 100 millisecond,
     *      *      * in other words refill will not wait 1 second to regenerate whole bunch of 10 tokens:
     *
     *      * <pre>
     *      * <code>Refill.smooth(600, Duration.ofMinutes(1));</code>
     *      * <code>Refill.smooth(10, Duration.ofSeconds(1));</code>
     *      * <code>Refill.smooth(1, Duration.ofMillis(100));</code>
     *      * </pre>
     *      * The three refills above absolutely equals.
     *      *
     *      * @param tokens
     *      * @param period
     *      *
     *      * @return
     */
    public Bandwidth withFixedRefillInterval(Duration refillInterval) {
        if (refillInterval == null) {
            // TODO
        }
        if (refillInterval.isNegative()) {
            // TODO
        }
        this.refillRefreshIntervalNanos = refillInterval.toNanos();
        return this;
    }

    /**
     * Specifies simple limitation <tt>capacity</tt> tokens per <tt>period</tt> time window.
     *
     * @param capacity
     * @param period
     * @return
     */
    public static Bandwidth simple(long capacity, Duration period) {
        Refill refill = Refill.of(capacity, period);
        return classic(capacity, refill);
    }

    /**
     * Specifies limitation in <a href="https://github.com/vladimir-bukhtoyarov/bucket4j/blob/1.3/doc-pages/token-bucket-brief-overview.md#token-bucket-algorithm">classic interpretation</a> of token-bucket algorithm.
     *
     * @param capacity
     * @param refill
     * @return
     */
    public static Bandwidth classic(long capacity, Refill refill) {
        return new Bandwidth(capacity, refill);
    }

    public long getCapacity() {
        return capacity;
    }

    public long getInitialTokens() {
        return initialTokens;
    }

    public long getRefillPeriodNanos() {
        return refillPeriodNanos;
    }

    public long getRefillTokens() {
        return refillTokens;
    }

    public long getRefillRefreshIntervalNanos() {
        return refillRefreshIntervalNanos;
    }

    public boolean isGreedy() {
        return refillRefreshIntervalNanos == GREEDY_REFIL_INTERVAL;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Bandwidth{");
        sb.append("capacity=").append(capacity);
        sb.append(", initialTokens=").append(initialTokens);
        sb.append(", refillPeriodNanos=").append(refillPeriodNanos);
        sb.append(", refillTokens=").append(refillTokens);
        sb.append(", refillRefreshIntervalNanos=").append(refillRefreshIntervalNanos);
        sb.append('}');
        return sb.toString();
    }

}