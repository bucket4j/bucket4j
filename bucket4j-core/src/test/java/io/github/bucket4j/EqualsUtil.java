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


public class EqualsUtil {

    public static boolean isConfigEquals(BucketConfiguration config1, BucketConfiguration config2) {
        if (config1 == config2) {
            return true;
        }
        if (config1 == null || config2 == null) {
            return false;
        }
        if (config1.getBandwidths().length != config2.getBandwidths().length) {
            return false;
        }
        for (int i = 0; i < config1.getBandwidths().length; i++) {
            Bandwidth bandwidth1 = config1.getBandwidths()[i];
            Bandwidth bandwidth2 = config2.getBandwidths()[i];
            if (bandwidth1.capacity != bandwidth2.capacity) {
                return false;
            }
            if (bandwidth1.initialTokens != bandwidth2.initialTokens) {
                return false;
            }
            if (bandwidth1.refillPeriodNanos != bandwidth2.refillPeriodNanos) {
                return false;
            }
            if (bandwidth1.refillIntervally != bandwidth2.refillIntervally) {
                return false;
            }
            if (bandwidth1.refillTokens != bandwidth2.refillTokens) {
                return false;
            }
        }
        return true;
    }

}
