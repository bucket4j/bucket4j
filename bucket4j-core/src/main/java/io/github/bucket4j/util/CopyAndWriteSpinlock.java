package io.github.bucket4j.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Locking primitive that is addressed to be a helper to build a CopyAndWrite shared state with following advantages:
 * <ul>
 *     <li>Readers must be never blocked.</li>
 *     <li>Writer can be blocked, but only for time that enough to complete the reads that were in progress when writer came, but
 *     readers that came after writer must never block writer.
 *     </li>
 *     <li>
 *         Copy-and-write operates by two pre-allocated chunks of data, and writer does not allocate a garbage while doing chunk mutation/replacement.
 *     </li>
 * </ul>
 *
 * <p>
 * This lock is suitable to be used when following conditions are meat:
 * <ul>
 *     <li>It can be multiple parallel readers, but it must be only one parallel mutator at time.</li>
 *     <li>In case of multiple mutators, synchronization of mutators must be implemented by outside code.</li>
 *     <li>You need for garbage free implementation. Shared state should be enough costly, to take advantage versus standard read -> copy -> mutate -> compare&swap idiom,
 *     otherwise if shared state is cheap then standard idiomatic code should be used instead of {@link CopyAndWriteSpinlock}</li>
 * </ul>
 *
 * <p>Example of usage can be found there {@link CopyAndWriteState}
 *
 * <p> Original idea is taken from <a href="http://stuff-gil-says.blogspot.com/2014/11/writerreaderphaser-story-about-new.html">WriterReaderPhaser: A story about a new (?) synchronization primitive</a>
 */
public class CopyAndWriteSpinlock {

    // This field is used to count each reader enter, Initial phase is red
    private final AtomicLong readerEnterCount = new AtomicLong(Long.MIN_VALUE);

    // Each reader exit is accounted in dedicated atomic for particular phase.
    private final AtomicLong redReaderExitCount = new AtomicLong(Long.MIN_VALUE);
    private final AtomicLong blackReaderExitCount = new AtomicLong(0);

    public long countReaderEnter() {
        return readerEnterCount.getAndIncrement();
    }

    public void countReaderExit(long stamp) {
        if (stamp < 0) {
            redReaderExitCount.getAndIncrement();
        } else {
            blackReaderExitCount.getAndIncrement();
        }
    }

    public void flipPhaseAfterCopyAndWrite() {
        boolean isCurrentPhaseRed = readerEnterCount.get() < 0;
        AtomicLong currentReaderExitCounter = isCurrentPhaseRed ? redReaderExitCount : blackReaderExitCount;
        AtomicLong nextExitPhase = isCurrentPhaseRed ? blackReaderExitCount : redReaderExitCount;
        long nextPhaseStartValue = isCurrentPhaseRed ? 0 : Long.MIN_VALUE;
        nextExitPhase.set(nextPhaseStartValue);
        long countOfInProgressReadersAtFlip = readerEnterCount.getAndSet(nextPhaseStartValue);
        while (currentReaderExitCounter.get() != countOfInProgressReadersAtFlip) {
            Thread.yield();
        }
    }

}
