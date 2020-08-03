package io.github.bucket4j.distributed.remote;

import io.github.bucket4j.distributed.serialization.InternalSerializationHelper;
import io.github.bucket4j.distributed.versioning.Version;

import static io.github.bucket4j.distributed.serialization.InternalSerializationHelper.*;

public abstract class AbstractBinaryTransaction implements MutableBucketEntry {

    private final byte[] requestBytes;
    private Version backwardCompatibilityVersion;

    protected AbstractBinaryTransaction(byte[] requestBytes) {
        this.requestBytes = requestBytes;
    }

    public byte[] execute() {
        Request<?> request = InternalSerializationHelper.deserializeRequest(requestBytes);
        backwardCompatibilityVersion = request.getBackwardCompatibilityVersion();

        long time = request.getClientSideTime() != null? request.getClientSideTime(): System.currentTimeMillis() * 1_000_000;
        RemoteCommand<?> command = request.getCommand();
        CommandResult<?> result = command.execute(this, time);

        return serializeResult(result, request.getBackwardCompatibilityVersion());
    }

    @Override
    public void set(RemoteBucketState state) {
        byte[] stateBytes = serializeState(state, backwardCompatibilityVersion);
        setRawState(stateBytes);
    }

    @Override
    public RemoteBucketState get() {
        byte[] stateBytes = getRawState();
        return deserializeState(stateBytes);
    }

    protected abstract byte[] getRawState();

    protected abstract void setRawState(byte[] stateBytes);



}
