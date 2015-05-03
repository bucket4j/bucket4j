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

package realworld;

import com.github.bucket4j.bucket.Bucket;

import java.util.concurrent.CountDownLatch;

public class ConsumerThread extends Thread {

    final CountDownLatch startLatch;
    final CountDownLatch endLatch;
    final Bucket bucket;
    final long workTimeNanos;
    long consumed;
    Exception exception;

    public ConsumerThread(CountDownLatch startLatch, CountDownLatch endLatch, Bucket bucket, long workTimeNanos) {
        this.startLatch = startLatch;
        this.endLatch = endLatch;
        this.bucket = bucket;
        this.workTimeNanos = workTimeNanos;
    }

    @Override
    public void run() {
        try {
            startLatch.countDown();
            startLatch.await();
            long startTime = System.nanoTime();
            do {
                if (bucket.tryConsumeSingleToken()) {
                    consumed++;
                }
            } while (System.nanoTime() - startTime < workTimeNanos);
        } catch (Exception e) {
            exception = e;
        } finally {
            endLatch.countDown();
        }
    }

    public Exception getException() {
        return exception;
    }

}
