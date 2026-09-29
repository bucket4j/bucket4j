package io.github.bucket4j.distributed.remote;

import java.util.List;

public class BatchResults {

    public final boolean stateModified;
    public final List<CommandResult<?>> results;
    public final byte[] finalState;

    public BatchResults(boolean stateModified, List<CommandResult<?>> results, byte[] finalState) {
        this.stateModified = stateModified;
        this.results = results;
        this.finalState = finalState;
    }

}
