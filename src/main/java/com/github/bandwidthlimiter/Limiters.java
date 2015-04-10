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

import com.github.bandwidthlimiter.bucket.BucketBuilder;
import com.github.bandwidthlimiter.bucket.TimeMeter;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public final class Limiters {

    private Limiters() {}

    public static BucketBuilder bucketWithNanoPrecision() {
        return new BucketBuilder(TimeMeter.SYSTEM_NANOTIME);
    }

    public static BucketBuilder bucketWithMillisPrecision() {
        return new BucketBuilder(TimeMeter.SYSTEM_MILLISECONDS);
    }

    public static BucketBuilder bucketWithCustomPrecisionPrecision(TimeMeter timeMeter) {
        return new BucketBuilder(timeMeter);
    }

    public static void main(String[] args) {
        System.out.println(new Date(TimeUnit.NANOSECONDS.toMillis(System.nanoTime())));
    }

}
