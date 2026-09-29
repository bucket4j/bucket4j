package io.github.bucket4j.distributed.remote;

import java.util.ArrayList;
import java.util.List;

import io.github.bucket4j.distributed.versioning.BackwardCompatibilityException;
import io.github.bucket4j.distributed.versioning.Version;

public class BatchRequest {

    private final List<Request<?>> requests;
    private final byte[] originalState;

    public BatchRequest(List<Request<?>> requests, byte[] originalState) {
        this.requests = requests;
        this.originalState = originalState;
    }

    public BatchResults execute() {
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
        for (Request<?> request : requests) {
            long currentTimeNanos = request.getClientSideTime() != null ? request.getClientSideTime() : defaultTime;
            RemoteCommand<?> command = request.getCommand();
            CommandResult<?> result = command.execute(entryWrapper, currentTimeNanos);
            results.add(result);
            if (entryWrapper.isStateModified()) {
                versionOfLatestUpdate = request.getBackwardCompatibilityVersion();
                entryWrapper = new MutableBucketEntry(entryWrapper.get());
            }
        }
        if (versionOfLatestUpdate == null) {
            // nothing was updated
            return new BatchResults(false, results, originalState);
        }
        try {
            return new BatchResults(true, results, entryWrapper.getStateBytes(versionOfLatestUpdate));
        } catch (BackwardCompatibilityException e) {
            return populateBatchResults(e.toResult());
        }
    }

    private BatchResults populateBatchResults(CommandResult<?> result) {
        List<CommandResult<?>> results = new ArrayList<>(requests.size());
        for (Request<?> request : requests) {
            results.add(result);
        }
        return new BatchResults(false, results, originalState);
    }

}
