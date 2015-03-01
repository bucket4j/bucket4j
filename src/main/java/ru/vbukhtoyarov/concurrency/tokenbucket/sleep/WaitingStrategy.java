package ru.vbukhtoyarov.concurrency.tokenbucket.sleep;

import java.util.concurrent.locks.LockSupport;

public interface WaitingStrategy {
    
    void sleep(long millisToAwait);
    
    public static final WaitingStrategy PARKING_WAIT_STRATEGY = new WaitingStrategy() {
        @Override
        public void sleep(long millisToAwait) {
            LockSupport.parkNanos(millisToAwait);
        }
    };

    public static final WaitingStrategy YIELDING_WAIT_STRATEGY = new WaitingStrategy() {
        @Override
        public void sleep(long millisToAwait) {
            // Sleep for the smallest unit of time possible just to relinquish
            // control
            // and to allow other threads to run.
            LockSupport.parkNanos(1);
        }
    };

    public static final WaitingStrategy SPINLOOP_WAIT_STRATEGY = new WaitingStrategy() {
        @Override
        public void sleep(long millisToAwait) {
            // Do nothing, don't sleep.
        }
    };

}
