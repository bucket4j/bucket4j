package io.github.bucket4j.util;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class CopyAndWriteState<S> {

    private final S redState;
    private final S blackState;
    private volatile S currentState;
    private final BiConsumer<S, S> copier;
    private final CopyAndWriteSpinlock spinlock = new CopyAndWriteSpinlock();

    public CopyAndWriteState(Supplier<S> stateSupplier, BiConsumer<S, S> copier) {
        redState = stateSupplier.get();
        blackState = stateSupplier.get();
        currentState = redState;
        this.copier = copier;
    }

    public void read(Consumer<S> reader) {
        long stamp = spinlock.countReaderEnter();
        try {
            reader.accept(currentState);
        } finally {
            spinlock.countReaderExit(stamp);
        }
    }

    public void write(Consumer<S> writer) {
        boolean currentPhaseIsRed = this.currentState == redState;
        S currentState = currentPhaseIsRed? redState : blackState;
        S nextState = currentPhaseIsRed? blackState : redState;
        copier.accept(currentState, nextState);
        writer.accept(nextState);
        this.currentState = nextState;
        spinlock.flipPhaseAfterCopyAndWrite();
    }

}
