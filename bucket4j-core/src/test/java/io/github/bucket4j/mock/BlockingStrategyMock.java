/*
 *
 *   Copyright 2015-2017 Vladimir Bukhtoyarov
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.github.bucket4j.mock;

import io.github.bucket4j.BlockingStrategy;

public class BlockingStrategyMock implements BlockingStrategy {

    private final TimeMeterMock meterMock;
    private long sleeped = 0;

    public BlockingStrategyMock(TimeMeterMock meterMock) {
        this.meterMock = meterMock;
    }

    public long getSleeped() {
        return sleeped;
    }

    @Override
    public void park(long nanosToPark) throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }
        sleeped += nanosToPark;
        meterMock.addTime(nanosToPark);
    }

    @Override
    public void parkUninterruptibly(long nanosToPark) {
        sleeped += nanosToPark;
        meterMock.addTime(nanosToPark);
    }

}