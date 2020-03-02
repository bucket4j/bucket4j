
package io.github.bucket4j;


import java.util.concurrent.locks.LockSupport;

/**
 * Specifies the way to block current thread to amount of time required to refill missed number of tokens in the bucket.
 *
 * There is default implementation {@link #PARKING},
 * also you can provide any other implementation which for example does something useful instead of blocking(acts as co-routine) or does spin loop.
 */
public interface UninterruptibleBlockingStrategy {

    /**
     * Parks current thread to required duration of nanoseconds ignoring all interrupts,
     * if interrupt was happen then interruption flag will be restored on the current thread.
     *
     * @param nanosToPark time to park in nanoseconds
     */
    void parkUninterruptibly(long nanosToPark);

    UninterruptibleBlockingStrategy PARKING = new UninterruptibleBlockingStrategy() {

        @Override
        public void parkUninterruptibly(final long nanosToPark) {
            final long endNanos = System.nanoTime() + nanosToPark;
            long remainingParkNanos = nanosToPark;
            boolean interrupted = false;
            try {
                while (true) {
                    LockSupport.parkNanos(remainingParkNanos);
                    long currentTimeNanos = System.nanoTime();
                    remainingParkNanos = endNanos - currentTimeNanos;
                    if (remainingParkNanos <= 0) {
                        return;
                    }
                    if (Thread.interrupted()) {
                        interrupted = true;
                    }
                }
            } finally {
                if (interrupted) {
                    // restore interrupted status
                    Thread.currentThread().interrupt();
                }
            }
        }

    };

}
