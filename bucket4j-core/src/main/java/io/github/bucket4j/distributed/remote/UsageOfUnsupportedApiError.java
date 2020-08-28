package io.github.bucket4j.distributed.remote;

import io.github.bucket4j.distributed.serialization.DeserializationAdapter;
import io.github.bucket4j.distributed.serialization.SerializationAdapter;
import io.github.bucket4j.distributed.serialization.SerializationHandle;
import io.github.bucket4j.distributed.versioning.UsageOfUnsupportedApiException;
import io.github.bucket4j.distributed.versioning.Version;
import io.github.bucket4j.distributed.versioning.Versions;
import io.github.bucket4j.util.ComparableByContent;

import java.io.IOException;

import static io.github.bucket4j.distributed.versioning.Versions.v_5_0_0;

public class UsageOfUnsupportedApiError implements CommandError, ComparableByContent<UsageOfUnsupportedApiError> {

    private final int requestedFormatNumber;
    private final int maxSupportedFormatNumber;

    public UsageOfUnsupportedApiError(int requestedFormatNumber, int maxSupportedFormatNumber) {
        this.requestedFormatNumber = requestedFormatNumber;
        this.maxSupportedFormatNumber = maxSupportedFormatNumber;
    }

    public int getRequestedFormatNumber() {
        return requestedFormatNumber;
    }

    public int getMaxSupportedFormatNumber() {
        return maxSupportedFormatNumber;
    }

    @Override
    public RuntimeException asException() {
        return new UsageOfUnsupportedApiException(requestedFormatNumber, maxSupportedFormatNumber);
    }

    @Override
    public boolean equalsByContent(UsageOfUnsupportedApiError other) {
        return requestedFormatNumber == other.requestedFormatNumber &&
                maxSupportedFormatNumber == other.maxSupportedFormatNumber;
    }

    public static SerializationHandle<UsageOfUnsupportedApiError> SERIALIZATION_HANDLE = new SerializationHandle<UsageOfUnsupportedApiError>() {
        @Override
        public <S> UsageOfUnsupportedApiError deserialize(DeserializationAdapter<S> adapter, S input, Version backwardCompatibilityVersion) throws IOException {
            int formatNumber = adapter.readInt(input);
            Versions.check(formatNumber, v_5_0_0, v_5_0_0);

            int requestedFormatNumber = adapter.readInt(input);
            int minSupportedFormatNumber = adapter.readInt(input);
            return new UsageOfUnsupportedApiError(requestedFormatNumber, minSupportedFormatNumber);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, UsageOfUnsupportedApiError error, Version backwardCompatibilityVersion) throws IOException {
            adapter.writeInt(output, v_5_0_0.getNumber());
            adapter.writeInt(output, error.requestedFormatNumber);
            adapter.writeInt(output, error.maxSupportedFormatNumber);
        }

        @Override
        public int getTypeId() {
            return 18;
        }

        @Override
        public Class<UsageOfUnsupportedApiError> getSerializedType() {
            return (Class) UsageOfUnsupportedApiError.class;
        }

    };

}
