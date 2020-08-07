package io.github.bucket4j.distributed.proxy.optimization.batch.mock;

import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.MutableBucketEntry;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.serialization.SerializationHandle;

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
    public SerializationHandle<RemoteCommand<?>> getSerializationHandle() {
        return null;
    }

    @Override
    public boolean isImmediateSyncRequired(long unsynchronizedTokens, long nanosSinceLastSync) {
        return false;
    }

    @Override
    public long estimateTokensToConsume() {
        return 0;
    }

    @Override
    public long getConsumedTokens(Long result) {
        return 0;
    }

    public long getAmount() {
        return amount;
    }

}
