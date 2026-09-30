package io.github.bucket4j.distributed.remote;

import java.util.ArrayList;
import java.util.List;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.versioning.BackwardCompatibilityException;
import io.github.bucket4j.distributed.versioning.Version;

public class BinaryBatchRequest {

    private final List<Request<?>> requests;
    private final byte[] originalState;

    public BinaryBatchRequest(List<Request<?>> requests, byte[] originalState) {
        this.requests = requests;
        this.originalState = originalState;
    }

    public BinaryBatchResults execute() {
        MutableBucketEntry entryWrapper;
        try {
            entryWrapper = new MutableBucketEntry(originalState);
        } catch (BackwardCompatibilityException e) {
            CommandResult<?> result = e.toResult();
            return populateBatchResults(result);
        }

        List<CommandResult<?>> results = new ArrayList<>(requests.size());
        long defaultTime = System.currentTimeMillis() * 1_000_000;
        Version versionOfLatestUpdate = null;
        Long ttlMillis = null;
        for (Request<?> request : requests) {
            long currentTimeNanos = request.getClientSideTime() != null ? request.getClientSideTime() : defaultTime;
            RemoteCommand<?> command = request.getCommand();
            CommandResult<?> result = command.execute(entryWrapper, currentTimeNanos);
            results.add(result);
            if (entryWrapper.isStateModified()) {
                versionOfLatestUpdate = request.getBackwardCompatibilityVersion();
                entryWrapper = new MutableBucketEntry(entryWrapper.get());
                ExpirationAfterWriteStrategy expiration = request.getExpirationStrategy();
                ttlMillis = expiration == null ? null : expiration.calculateTimeToLiveMillis(entryWrapper.get(), currentTimeNanos);
            }
        }
        if (versionOfLatestUpdate == null) {
            // nothing was updated
            return new BinaryBatchResults(null, false, results, null);
        }
        try {
            return new BinaryBatchResults(ttlMillis, true, results, entryWrapper.getStateBytes(versionOfLatestUpdate));
        } catch (BackwardCompatibilityException e) {
            return populateBatchResults(e.toResult());
        }
    }

    private BinaryBatchResults populateBatchResults(CommandResult<?> result) {
        List<CommandResult<?>> results = new ArrayList<>(requests.size());
        for (Request<?> request : requests) {
            results.add(result);
        }
        return new BinaryBatchResults(null, false, results, null);
    }

}
