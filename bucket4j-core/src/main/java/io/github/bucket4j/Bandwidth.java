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
 *
 * <h3>Greediness of refill</h3>
 * By default any bandwidth does refill in greedy manner, because bandwidth tries to add the tokens to bucket as soon as possible.
 * For example bandwidth with refill "10 tokens per 1 second" will add 1 token per each 100 millisecond,
 * in other words refill will not wait 1 second to regenerate whole bunch of 10 tokens.
 *
 * The three bandwidths bellow do refill of tokens with same speed:
 *  <pre>
 *  <code>Bandwidth.simple(600, Duration.ofMinutes(1));</code>
 *  <code>Bandwidth.simple(10, Duration.ofSeconds(1));</code>
 *  <code>Bandwidth.simple(1, Duration.ofMillis(100));</code>
 *  </pre>
 *
 * <p>
 * If greediness is undesired, then you can specify the fixed interval refill via {@link #withFixedRefillInterval(Duration) withFixedRefillInterval}.
 * When fixed refill interval was specified then greediness is turned-off.
 * For example the bandwidth bellow will refill 10 tokens per 1 second instead of 1 token per 100 milliseconds:
 * <pre>
 *  <code>Bandwidth.simple(600, Duration.ofMinutes(1)).withFixedRefillInterval(Duration.ofSecond(1));</code>
 * </pre>
 */
public class Bandwidth implements Serializable {

    private static final long serialVersionUID = 42L;

    static final long GREEDY_REFILL_INTERVAL = 0;

    final long capacity;
    final long initialTokens;
    final long refillPeriodNanos;
    final long refillTokens;
    final long refillRefreshIntervalNanos;

    private Bandwidth(long capacity, long refillPeriodNanos, long refillTokens, long initialTokens, long refillRefreshIntervalNanos) {
        this.capacity = capacity;
        this.initialTokens = initialTokens;
        this.refillPeriodNanos = refillPeriodNanos;
        this.refillTokens = refillTokens;
        this.refillRefreshIntervalNanos = refillRefreshIntervalNanos;
    }

    /**
     * Specifies simple limitation <tt>capacity</tt> tokens per <tt>period</tt> time window.
     *
     * @param capacity
     * @param period
     *
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
     *
     * @return
     */
    public static Bandwidth classic(long capacity, Refill refill) {
        if (capacity <= 0) {
            throw BucketExceptions.nonPositiveCapacity(capacity);
        }
        if (refill == null) {
            throw BucketExceptions.nullBandwidthRefill();
        }
        return new Bandwidth(capacity, refill.periodNanos, refill.tokens, capacity, GREEDY_REFILL_INTERVAL);
    }

    /**
     * By default new created bandwidth has amount of tokens that equals its capacity.
     * This method allows to replace amount of initial tokens.
     *
     * @param initialTokens
     *
     * @return the copy of this bandwidth with new value of initial tokens.
     */
    public Bandwidth withInitialTokens(long initialTokens) {
        if (initialTokens < 0) {
            throw BucketExceptions.nonPositiveInitialTokens(initialTokens);
        }
        return new Bandwidth(capacity, refillPeriodNanos, refillTokens, initialTokens, refillRefreshIntervalNanos);
    }


    /**
     * Creates the copy of current bandwidth for which the greediness of refill is switched-off.
     * 
     * @param fixedRefillInterval specifies the interval between refill of tokens
     * 
     * @return new instance of {@link Bandwidth} configured by {@code fixedRefillInterval}
     */
    public Bandwidth withFixedRefillInterval(Duration fixedRefillInterval) {
        if (fixedRefillInterval == null) {
            throw BucketExceptions.nullFixedRefillInterval();
        }
        if (fixedRefillInterval.isNegative() || fixedRefillInterval.isZero()) {
            throw BucketExceptions.nonPositiveFixedRefillInterval(fixedRefillInterval);
        }
        return new Bandwidth(capacity, refillPeriodNanos, refillTokens, initialTokens, fixedRefillInterval.toNanos());
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