package io.github.bucket4j.distributed.remote;

import java.util.List;

public class BinaryBatchResults {

    public final boolean stateModified;
    public final List<CommandResult<?>> results;
    public final byte[] finalState;

    public BinaryBatchResults(boolean stateModified, List<CommandResult<?>> results, byte[] finalState) {
        this.stateModified = stateModified;
        this.results = results;
        this.finalState = finalState;
    }

}
