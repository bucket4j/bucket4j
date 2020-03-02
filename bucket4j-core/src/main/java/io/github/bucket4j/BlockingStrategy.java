package io.github.bucket4j;


import java.util.concurrent.locks.LockSupport;

/**
 * Specifies the way to block current thread to amount of time required to refill missed number of tokens in the bucket.
 *
 * There is default implementation {@link #PARKING},
 * also you can provide any other implementation which for example does something useful instead of blocking(acts as co-routine) or does spin loop.
 */
public interface BlockingStrategy {

    /**
     * Park current thread to required duration of nanoseconds.
     * Throws {@link InterruptedException} in case of current thread was interrupted.
     *
     * @param nanosToPark time to park in nanoseconds
     *
     * @throws InterruptedException if current tread is interrupted.
     */
    void park(long nanosToPark) throws InterruptedException;

    BlockingStrategy PARKING = new BlockingStrategy() {

        @Override
        public void park(final long nanosToPark) throws InterruptedException {
            final long endNanos = System.nanoTime() + nanosToPark;
            long remainingParkNanos = nanosToPark;
            while (true) {
                LockSupport.parkNanos(remainingParkNanos);
                long currentTimeNanos = System.nanoTime();
                remainingParkNanos = endNanos - currentTimeNanos;
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
                if (remainingParkNanos <= 0) {
                    return;
                }
            }
        }
    };

}
