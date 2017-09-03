package io.github.bucket4j.local;

import org.junit.Test;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

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