package io.github.bucket4j.distributed.remote;

import io.github.bucket4j.serialization.DeserializationAdapter;
import io.github.bucket4j.serialization.SerializationAdapter;
import io.github.bucket4j.serialization.SerializationHandle;
import io.github.bucket4j.serialization.SerializationRoot;

import java.io.IOException;

public class RemoteRequest<T> implements SerializationRoot {

    private final RemoteCommand<T> command;

    public RemoteRequest(RemoteCommand<T> command) {
        this.command = command;
    }

    public RemoteCommand<T> getCommand() {
        return command;
    }

    public static SerializationHandle<RemoteRequest> SERIALIZATION_HANDLE = new SerializationHandle<RemoteRequest>() {
        @Override
        public <S> RemoteRequest deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            RemoteCommand<?> targetCommand = RemoteCommand.deserialize(adapter, input);
            return new RemoteRequest(targetCommand);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, RemoteRequest request) throws IOException {
            RemoteCommand.serialize(adapter, output, request.command);
        }

        @Override
        public int getTypeId() {
            return 34;
        }

        @Override
        public Class<RemoteRequest> getSerializedType() {
            return RemoteRequest.class;
        }

    };

    @Override
    public SerializationHandle<?> getSerializationHandle() {
        return SERIALIZATION_HANDLE;
    }

}
