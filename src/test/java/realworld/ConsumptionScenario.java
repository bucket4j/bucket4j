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

import com.github.bucket4j.Bucket;

import java.util.concurrent.CountDownLatch;

public class ConsumptionScenario {

    private final CountDownLatch startLatch;
    private final CountDownLatch endLatch;
    private final ConsumerThread[] consumers;
    private long duration;

    public ConsumptionScenario(int threadCount, long workTimeNanos, Bucket bucket) {
        this.startLatch = new CountDownLatch(threadCount);
        this.endLatch = new CountDownLatch(threadCount);
        this.consumers = new ConsumerThread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            this.consumers[i] = new ConsumerThread(startLatch, endLatch, bucket, workTimeNanos);
        }
    }

    public long execute() throws Exception {
        long start = System.nanoTime();

        for (ConsumerThread consumer : consumers) {
            consumer.start();
        }
        endLatch.await();
        this.duration = System.nanoTime() - start;

        long consumed = 0;
        for (ConsumerThread consumer : consumers) {
            if (consumer.getException() != null) {
                throw consumer.getException();
            } else {
                consumed += consumer.consumed;
            }
        }


        return consumed;
    }

    public long getDurationNanos() {
        return duration;
    }

}
