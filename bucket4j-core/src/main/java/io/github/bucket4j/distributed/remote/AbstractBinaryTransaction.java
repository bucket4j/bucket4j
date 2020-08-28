package io.github.bucket4j.distributed.remote;

import io.github.bucket4j.distributed.serialization.InternalSerializationHelper;
import io.github.bucket4j.distributed.versioning.*;

import static io.github.bucket4j.distributed.serialization.InternalSerializationHelper.*;

public abstract class AbstractBinaryTransaction implements MutableBucketEntry {

    private final byte[] requestBytes;
    private Version backwardCompatibilityVersion;

    protected AbstractBinaryTransaction(byte[] requestBytes) {
        this.requestBytes = requestBytes;
    }

    public byte[] execute() {
        Request<?> request;
        try {
            request = InternalSerializationHelper.deserializeRequest(requestBytes);
        } catch (UnsupportedTypeException e) {
            return serializeResult(CommandResult.unsupportedType(e.getTypeId()), Versions.getOldest());
        } catch (UsageOfUnsupportedApiException e) {
            return serializeResult(CommandResult.usageOfUnsupportedApiException(e.getRequestedFormatNumber(), e.getMaxSupportedFormatNumber()), Versions.getOldest());
        } catch (UsageOfObsoleteApiException e) {
            return serializeResult(CommandResult.usageOfObsoleteApiException(e.getRequestedFormatNumber(), e.getMinSupportedFormatNumber()), Versions.getOldest());
        }

        backwardCompatibilityVersion = request.getBackwardCompatibilityVersion();

        try {
            long time = request.getClientSideTime() != null? request.getClientSideTime(): System.currentTimeMillis() * 1_000_000;
            RemoteCommand<?> command = request.getCommand();
            CommandResult<?> result = command.execute(this, time);
            return serializeResult(result, request.getBackwardCompatibilityVersion());
        } catch (UnsupportedTypeException e) {
            return serializeResult(CommandResult.unsupportedType(e.getTypeId()), backwardCompatibilityVersion);
        } catch (UsageOfUnsupportedApiException e) {
            return serializeResult(CommandResult.usageOfUnsupportedApiException(e.getRequestedFormatNumber(), e.getMaxSupportedFormatNumber()), backwardCompatibilityVersion);
        } catch (UsageOfObsoleteApiException e) {
            return serializeResult(CommandResult.usageOfObsoleteApiException(e.getRequestedFormatNumber(), e.getMinSupportedFormatNumber()), backwardCompatibilityVersion);
        }
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
