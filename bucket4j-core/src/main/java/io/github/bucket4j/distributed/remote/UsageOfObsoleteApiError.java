package io.github.bucket4j.distributed.remote;

import io.github.bucket4j.distributed.serialization.DeserializationAdapter;
import io.github.bucket4j.distributed.serialization.SerializationAdapter;
import io.github.bucket4j.distributed.serialization.SerializationHandle;
import io.github.bucket4j.distributed.versioning.UsageOfObsoleteApiException;
import io.github.bucket4j.distributed.versioning.Version;
import io.github.bucket4j.distributed.versioning.Versions;
import io.github.bucket4j.util.ComparableByContent;

import java.io.IOException;

import static io.github.bucket4j.distributed.versioning.Versions.v_5_0_0;

public class UsageOfObsoleteApiError implements ComparableByContent<UsageOfObsoleteApiError>, CommandError {

    private final int requestedFormatNumber;
    private final int minSupportedFormatNumber;

    public UsageOfObsoleteApiError(int requestedFormatNumber, int minSupportedFormatNumber) {
        this.requestedFormatNumber = requestedFormatNumber;
        this.minSupportedFormatNumber = minSupportedFormatNumber;
    }

    public int getRequestedFormatNumber() {
        return requestedFormatNumber;
    }

    public int getMinSupportedFormatNumber() {
        return minSupportedFormatNumber;
    }

    @Override
    public RuntimeException asException() {
        return new UsageOfObsoleteApiException(requestedFormatNumber, minSupportedFormatNumber);
    }

    @Override
    public boolean equalsByContent(UsageOfObsoleteApiError other) {
        return other.requestedFormatNumber == requestedFormatNumber
                && other.minSupportedFormatNumber == minSupportedFormatNumber;
    }

    public static SerializationHandle<UsageOfObsoleteApiError> SERIALIZATION_HANDLE = new SerializationHandle<UsageOfObsoleteApiError>() {
        @Override
        public <S> UsageOfObsoleteApiError deserialize(DeserializationAdapter<S> adapter, S input, Version backwardCompatibilityVersion) throws IOException {
            int formatNumber = adapter.readInt(input);
            Versions.check(formatNumber, v_5_0_0, v_5_0_0);

            int requestedFormatNumber = adapter.readInt(input);
            int minSupportedFormatNumber = adapter.readInt(input);
            return new UsageOfObsoleteApiError(requestedFormatNumber, minSupportedFormatNumber);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, UsageOfObsoleteApiError error, Version backwardCompatibilityVersion) throws IOException {
            adapter.writeInt(output, v_5_0_0.getNumber());
            adapter.writeInt(output, error.requestedFormatNumber);
            adapter.writeInt(output, error.minSupportedFormatNumber);
        }

        @Override
        public int getTypeId() {
            return 17;
        }

        @Override
        public Class<UsageOfObsoleteApiError> getSerializedType() {
            return (Class) UsageOfObsoleteApiError.class;
        }

    };


}
