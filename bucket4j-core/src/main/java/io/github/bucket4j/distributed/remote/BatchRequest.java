package io.github.bucket4j.distributed.remote;

import java.util.List;

public class BatchRequest {

    private final List<Request<?>> requests;
    private final byte[] originalState;

    public BatchRequest(List<Request<?>> requests, byte[] originalState) {
        this.requests = requests;
        this.originalState = originalState;
    }

    public byte[] execute(List<CommandResult<?>> results) {
        MutableBucketEntry entryWrapper = new MutableBucketEntry(originalState);
        for (Request<?> request : requests) {
            long currentTimeNanos = request.getClientSideTime() != null ? request.getClientSideTime() : System.currentTimeMillis() * 1_000_000;
            RemoteCommand<?> command = request.getCommand();
            CommandResult<?> result = command.execute(entryWrapper, currentTimeNanos);
            results.add(result);
        }
        if (!entryWrapper.isStateModified()) {
            return null;
        } else {
            Request<?> lastRequest = requests.get(requests.size() - 1);
            return entryWrapper.getStateBytes(lastRequest.getBackwardCompatibilityVersion());
        }
    }

}
