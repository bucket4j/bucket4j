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

import java.util.concurrent.atomic.AtomicLong;

public class SimpleBucketListener implements BucketListener {

    private AtomicLong consumed = new AtomicLong();
    private AtomicLong rejected = new AtomicLong();
    private AtomicLong delayedNanos = new AtomicLong();
    private AtomicLong parkedNanos = new AtomicLong();
    private AtomicLong interrupted = new AtomicLong();

    @Override
    public void onConsumed(long tokens) {
        consumed.addAndGet(tokens);
    }

    @Override
    public void onRejected(long tokens) {
        rejected.addAndGet(tokens);
    }

    @Override
    public void onDelayed(long nanos) {
        delayedNanos.addAndGet(nanos);
    }

    @Override
    public void onParked(long nanos) {
        parkedNanos.addAndGet(nanos);
    }

    @Override
    public void onInterrupted(InterruptedException e) {
        interrupted.incrementAndGet();
    }

    public long getConsumed() {
        return consumed.get();
    }

    public long getRejected() {
        return rejected.get();
    }

    public long getDelayedNanos() {
        return delayedNanos.get();
    }

    public long getParkedNanos() {
        return parkedNanos.get();
    }

    public long getInterrupted() {
        return interrupted.get();
    }

}
