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

package com.github.bucket4j.state;

import org.openjdk.jmh.annotations.*;

@State(Scope.Thread)
public class ThreadDistributionState {

    long threadId = Thread.currentThread().getId();

    public long invocationCount;

    @Setup
    public void setUp() {
        threadId = Thread.currentThread().getId();
    }

    @TearDown(Level.Iteration)
    public void printDistribution() {
        System.out.print(this);
        invocationCount = 0;
    }

    @Override
    public String toString() {
        return "\nThreadDistributionState{" +
                "threadId=" + threadId +
                ", invocationCount=" + invocationCount +
                '}';
    }

}
