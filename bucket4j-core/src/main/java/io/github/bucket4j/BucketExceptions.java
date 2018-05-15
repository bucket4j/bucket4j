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

import java.text.MessageFormat;
import java.time.Duration;

public final class BucketExceptions {

    // ------------------- construction time exceptions --------------------------------
    public static IllegalArgumentException nonPositiveCapacity(long capacity) {
        String pattern = "{0} is wrong value for capacity, because capacity should be positive";
        String msg = MessageFormat.format(pattern, capacity);
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException nonPositiveInitialTokens(long initialTokens) {
        String pattern = "{0} is wrong value for initial capacity, because initial tokens count should be positive";
        String msg = MessageFormat.format(pattern, initialTokens);
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException nullBandwidth() {
        String msg = "Bandwidth can not be null";
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException nullBandwidthRefill() {
        String msg = "Bandwidth refill can not be null";
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException nullTimeMeter() {
        String msg = "Time meter can not be null";
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException nullSynchronizationStrategy() {
        String msg = "Synchronization strategy can not be null";
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException nullListener() {
        String msg = "listener can not be null";
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException nullRefillPeriod() {
        String msg = "Refill period can not be null";
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException nullFixedRefillInterval() {
        String msg = "Fixed refill interval can not be null";
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException nullScheduler() {
        String msg = "Scheduler can not be null";
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException nullConfiguration() {
        String msg = "Configuration can not be null";
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException nullConfigurationSupplier() {
        String msg = "Configuration supplier can not be null";
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException nonPositivePeriod(long period) {
        String pattern = "{0} is wrong value for period of bandwidth, because period should be positive";
        String msg = MessageFormat.format(pattern, period);
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException nonPositiveFixedRefillInterval(Duration fixedRefillInterval) {
        String pattern = "{0} is wrong value for fixed refill interval, because period should be positive";
        String msg = MessageFormat.format(pattern, fixedRefillInterval);
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException nonPositivePeriodTokens(long tokens) {
        String pattern = "{0} is wrong value for period tokens, because tokens should be positive";
        String msg = MessageFormat.format(pattern, tokens);
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException restrictionsNotSpecified() {
        String msg = "At list one limited bandwidth should be specified";
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException tooHighRefillRate(long periodNanos, long tokens) {
        double actualRate = (double) tokens / (double) periodNanos;
        String pattern = "{0} token/nanosecond is not permitted refill rate" +
                ", because highest supported rate is 1 token/nanosecond";
        String msg = MessageFormat.format(pattern, actualRate);
        return new IllegalArgumentException(msg);
    }

    // ------------------- end of construction time exceptions --------------------------------

    // ------------------- usage time exceptions  ---------------------------------------------
    public static IllegalArgumentException nonPositiveNanosToWait(long waitIfBusyNanos) {
        String pattern = "Waiting value should be positive, {0} is wrong waiting period";
        String msg = MessageFormat.format(pattern, waitIfBusyNanos);
        return new IllegalArgumentException(msg);
    }

    public static IllegalArgumentException nonPositiveTokensToConsume(long tokens) {
        String pattern = "Unable to consume {0} tokens, due to number of tokens to consume should be positive";
        String msg = MessageFormat.format(pattern, tokens);
        return new IllegalArgumentException(msg);
    }

    private BucketExceptions() {
        // private constructor for utility class
    }

}
