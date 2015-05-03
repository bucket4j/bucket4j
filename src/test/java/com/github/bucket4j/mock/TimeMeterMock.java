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

package com.github.bucket4j.mock;

import com.github.bucket4j.TimeMeter;

/**
 * Created by vladimir.bukhtoyarov on 09.04.2015.
 */
public class TimeMeterMock implements TimeMeter {

    private long currentTime;
    private long sleeped = 0;
    private long incrementAfterEachSleep;

    public TimeMeterMock() {
        currentTime = 0;
    }

    public TimeMeterMock(long currentTime) {
        this.currentTime = currentTime;
    }

    public void addTime(long duration) {
        currentTime += duration;
    }

    public void setCurrentTime(long currentTime) {
        this.currentTime = currentTime;
    }

    public void setIncrementAfterEachSleep(long incrementAfterEachSleep) {
        this.incrementAfterEachSleep = incrementAfterEachSleep;
    }

    public long getSleeped() {
        return sleeped;
    }

    @Override
    public long currentTime() {
        return currentTime;
    }

    @Override
    public void sleep(long units) throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }
        currentTime += units + incrementAfterEachSleep;
        sleeped += units;
    }

    public void reset() {
        currentTime = 0;
        sleeped = 0;
    }
}
