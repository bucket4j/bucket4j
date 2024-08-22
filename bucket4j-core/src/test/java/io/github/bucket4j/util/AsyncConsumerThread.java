/*
 *
 * Copyright 2015-2019 Vladimir Bukhtoyarov
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

import java.util.concurrent.CountDownLatch;
import java.util.function.Function;

import io.github.bucket4j.distributed.AsyncBucketProxy;

public class AsyncConsumerThread extends Thread {

    private final CountDownLatch startLatch;
    private final CountDownLatch endLatch;
    private final AsyncBucketProxy bucket;
    private final long workTimeNanos;
    private final Function<AsyncBucketProxy, Long> action;
    private long consumed;
    private Exception exception;

    public AsyncConsumerThread(CountDownLatch startLatch, CountDownLatch endLatch, AsyncBucketProxy bucket, long workTimeNanos, Function<AsyncBucketProxy, Long> action) {
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
            e.printStackTrace();
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
