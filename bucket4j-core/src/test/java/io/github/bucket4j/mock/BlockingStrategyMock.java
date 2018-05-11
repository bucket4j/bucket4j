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

package io.github.bucket4j.mock;

import io.github.bucket4j.BlockingStrategy;
import io.github.bucket4j.UninterruptibleBlockingStrategy;

public class BlockingStrategyMock implements BlockingStrategy, UninterruptibleBlockingStrategy {

    private final TimeMeterMock meterMock;
    private long parkedNanos = 0;
    private long atemptToParkNanos = 0;

    public BlockingStrategyMock(TimeMeterMock meterMock) {
        this.meterMock = meterMock;
    }

    public long getParkedNanos() {
        return parkedNanos;
    }

    public long getAtemptToParkNanos() {
        return atemptToParkNanos;
    }

    @Override
    public void park(long nanosToPark) throws InterruptedException {
        atemptToParkNanos += nanosToPark;
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        parkedNanos += nanosToPark;
        meterMock.addTime(nanosToPark);
    }

    @Override
    public void parkUninterruptibly(long nanosToPark) {
        atemptToParkNanos += nanosToPark;
        parkedNanos += nanosToPark;
        meterMock.addTime(nanosToPark);
    }

}