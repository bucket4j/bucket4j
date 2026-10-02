package io.github.bucket4j.distributed.remote;

import java.util.List;

public class BinaryBatchResults {

    public final boolean stateModified;
    public final List<CommandResult<?>> results;
    public final byte[] newStateBytes;
    public final Long ttlMillis;

    public BinaryBatchResults(Long ttlMillis, boolean stateModified, List<CommandResult<?>> results, byte[] newStateBytes) {
        this.ttlMillis = ttlMillis;
        this.stateModified = stateModified;
        this.results = results;
        this.newStateBytes = newStateBytes;
    }

}
