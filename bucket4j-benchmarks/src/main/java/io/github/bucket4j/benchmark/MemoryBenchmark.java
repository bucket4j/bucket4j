/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2022 Vladimir Bukhtoyarov
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
package io.github.bucket4j.benchmark;

import com.google.common.util.concurrent.RateLimiter;
import io.github.bucket4j.Bucket;
import io.github.resilience4j.ratelimiter.internal.AtomicRateLimiter;
import org.openjdk.jol.info.ClassLayout;

public class MemoryBenchmark {

    public static void main(String[] args) {
        System.out.println("Bucket4j: " + ClassLayout.parseClass(Bucket.class).toPrintable());
        System.out.println("Guava: " + ClassLayout.parseClass(RateLimiter.class).toPrintable());
        System.out.println("Resilience4j: " + ClassLayout.parseClass(AtomicRateLimiter.class).toPrintable());
        System.out.println("Resilience4j.semaphoreBasedRateLimiter: " + ClassLayout.parseClass(io.github.resilience4j.ratelimiter.RateLimiter.class).toPrintable());
    }

    public static class Bucket4jLayoutInvestigation {

    }

}
