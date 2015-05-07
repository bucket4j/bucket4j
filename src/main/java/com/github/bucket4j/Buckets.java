
/*
 * Copyright 2015 Vladimir Bukhtoyarov
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.github.bucket4j;

/**
 * Entry point for bucket builder.
 */
public  final class Buckets {

    /**
     * Creates instance of {@link com.github.bucket4j.BucketBuilder} which will create buckets with {@link com.github.bucket4j.TimeMeter#SYSTEM_NANOTIME} as time meter.
     */
    public static BucketBuilder withNanoTimePrecision() {
        return new BucketBuilder(TimeMeter.SYSTEM_NANOTIME);
    }

    /**
     * Creates instance of {@link com.github.bucket4j.BucketBuilder} which will create buckets with {@link com.github.bucket4j.TimeMeter#SYSTEM_MILLISECONDS} as time meter.
     */
    public static BucketBuilder withMillisTimePrecision() {
        return new BucketBuilder(TimeMeter.SYSTEM_MILLISECONDS);
    }

    /**
     * Creates instance of {@link com.github.bucket4j.BucketBuilder} which will create buckets with {@code customTimeMeter} as time meter.
     *
     * @param customTimeMeter object which will measure time.
     */
    public static BucketBuilder withCustomTimePrecision(TimeMeter customTimeMeter) {
        return new BucketBuilder(customTimeMeter);
    }

}
