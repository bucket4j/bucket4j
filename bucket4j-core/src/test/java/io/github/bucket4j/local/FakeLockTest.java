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

package io.github.bucket4j.local;

import org.junit.Test;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by vladimir.bukhtoyarov on 16.05.2017.
 */
public class FakeLockTest {

    @Test(expected = UnsupportedOperationException.class)
    public void lockInterruptibly() throws Exception {
        FakeLock.INSTANCE.lockInterruptibly();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void tryLock() throws Exception {
        FakeLock.INSTANCE.tryLock();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void tryLock1() throws Exception {
        FakeLock.INSTANCE.tryLock(1, TimeUnit.SECONDS);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void newCondition() throws Exception {
        FakeLock.INSTANCE.newCondition();
    }

    public static void main(String[] args) {
        ConcurrentHashMap map = new ConcurrentHashMap();
        map.compute(1, (key, value) -> {
            map.remove(1);
            return 42;
        });
    }

}