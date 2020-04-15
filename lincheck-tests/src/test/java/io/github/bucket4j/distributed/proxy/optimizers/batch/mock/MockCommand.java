package io.github.bucket4j.distributed.proxy.optimizers.batch.mock;

import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.MutableBucketEntry;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.serialization.SerializationHandle;

public class MockCommand implements RemoteCommand<Long> {

    private final long amount;

    public MockCommand(long amount) {
        this.amount = amount;
    }

    @Override
    public CommandResult<Long> execute(MutableBucketEntry mutableEntry, long currentTimeNanos) {
        return null;
    }

    @Override
    public <T> SerializationHandle<T> getSerializationHandle() {
        return null;
    }

    public long getAmount() {
        return amount;
    }

}
