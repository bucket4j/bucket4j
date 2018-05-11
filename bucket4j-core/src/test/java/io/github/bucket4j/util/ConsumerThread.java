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

package io.github.bucket4j.util;

import io.github.bucket4j.Bucket;

import java.util.concurrent.CountDownLatch;
import java.util.function.Function;

public class ConsumerThread extends Thread {

    private final CountDownLatch startLatch;
    private final CountDownLatch endLatch;
    private final Bucket bucket;
    private final long workTimeNanos;
    private final Function<Bucket, Long> action;
    private long consumed;
    private Exception exception;

    public ConsumerThread(CountDownLatch startLatch, CountDownLatch endLatch, Bucket bucket, long workTimeNanos, Function<Bucket, Long> action) {
        this.startLatch = startLatch;
        this.endLatch = endLatch;
        this.bucket = bucket;
        this.workTimeNanos = workTimeNanos;
        this.action = action;
    }

    @Override
    public void run() {
        try {
            startLatch.countDown();
            startLatch.await();
            long endNanoTime = System.nanoTime() + workTimeNanos;
            while(true) {
                if (System.nanoTime() >= endNanoTime) {
                    return;
                }
                consumed += action.apply(bucket);
            }
        } catch (Exception e) {
            exception.printStackTrace();
            exception = e;
        } finally {
            endLatch.countDown();
        }
    }

    public Exception getException() {
        return exception;
    }

    public long getConsumed() {
        return consumed;
    }

}
