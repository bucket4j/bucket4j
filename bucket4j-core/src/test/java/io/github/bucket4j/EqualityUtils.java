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

import io.github.bucket4j.grid.CommandResult;
import io.github.bucket4j.grid.GridBucketState;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import static org.junit.Assert.assertEquals;

public class EqualityUtils {

    private static final Map<Class<?>, BiFunction<?, ?, Boolean>> comparators = new HashMap<>();
    static {
        registerComparator(Bandwidth.class, (bandwidth1, bandwidth2) ->
                bandwidth1.capacity == bandwidth2.capacity &&
                bandwidth1.initialTokens == bandwidth2.initialTokens &&
                bandwidth1.refillPeriodNanos == bandwidth2.refillPeriodNanos &&
                bandwidth1.refillTokens == bandwidth2.refillTokens &&
                bandwidth1.refillIntervally == bandwidth2.refillIntervally &&
                bandwidth1.timeOfFirstRefillMillis == bandwidth2.timeOfFirstRefillMillis &&
                bandwidth1.useAdaptiveInitialTokens == bandwidth2.useAdaptiveInitialTokens
        );

        registerComparator(BucketConfiguration.class, (config1, config2) -> {
                if (config1.getBandwidths().length != config2.getBandwidths().length) {
                    return false;
                }
                for (int i = 0; i < config1.getBandwidths().length; i++) {
                    Bandwidth bandwidth1 = config1.getBandwidths()[i];
                    Bandwidth bandwidth2 = config2.getBandwidths()[i];
                    if (!equals(bandwidth1, bandwidth2)) {
                        return false;
                    }
                }
                return true;
            }
        );

        registerComparator(CommandResult.class, (result1, result2) -> {
            return result1.isBucketNotFound() == result2.isBucketNotFound() &&
                    equals(result1.getData(), result2.getData());
        });

        registerComparator(GridBucketState.class, (state1, state2) -> {
            return equals(state1.getConfiguration(), state2.getConfiguration()) &&
                    equals(state1.getState(), state2.getState());
        });

    }

    private static <T> void registerComparator(Class<T> clazz, BiFunction<T, T, Boolean> comparator) {
        comparators.put(clazz, comparator);
    }

    public static <T> boolean equals(T object1, T object2) {
        if (object1 == object2) {
            return true;
        }
        if ((object1 == null && object2 != null) || (object1 != null && object2 == null)) {
            return false;
        }
        BiFunction<T, T, Boolean> comparator = (BiFunction<T, T, Boolean>) comparators.get(object1.getClass());
        if (comparator == null) {
            return object1.equals(object2);
        }
        return comparator.apply(object1, object2);
    }

}
