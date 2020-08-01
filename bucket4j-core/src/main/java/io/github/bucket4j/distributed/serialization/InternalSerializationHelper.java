package io.github.bucket4j.distributed.serialization;

import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.serialization.DataOutputSerializationAdapter;

import java.io.*;

public class InternalSerializationHelper {

    public static byte[] serializeState(RemoteBucketState state) {
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(byteStream);

            RemoteBucketState.SERIALIZATION_HANDLE.serialize(DataOutputSerializationAdapter.INSTANCE, output, state);

            output.close();
            byteStream.close();

            return byteStream.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static RemoteBucketState deserializeState(byte[] bytes) {
        try {
            try (DataInputStream inputSteam = new DataInputStream(new ByteArrayInputStream(bytes))) {
                return RemoteBucketState.SERIALIZATION_HANDLE.deserialize(DataOutputSerializationAdapter.INSTANCE, inputSteam);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static byte[] serializeCommand(RemoteCommand<?> command) {
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(byteStream);

            RemoteCommand.serialize(DataOutputSerializationAdapter.INSTANCE, output, command);

            output.close();
            byteStream.close();

            return byteStream.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static <T> RemoteCommand<T> deserializeCommand(byte[] bytes) {
        try {
            try (DataInputStream inputSteam = new DataInputStream(new ByteArrayInputStream(bytes))) {
                return (RemoteCommand<T>) RemoteCommand.deserialize(DataOutputSerializationAdapter.INSTANCE, inputSteam);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static byte[] serializeResult(CommandResult<?> result) {
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(byteStream);

            CommandResult.SERIALIZATION_HANDLE.serialize(DataOutputSerializationAdapter.INSTANCE, output, result);

            output.close();
            byteStream.close();

            return byteStream.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static <T> CommandResult<T> deserializeResult(byte[] bytes) {
        try {
            try (DataInputStream inputSteam = new DataInputStream(new ByteArrayInputStream(bytes))) {
                return (CommandResult<T>) CommandResult.SERIALIZATION_HANDLE.deserialize(DataOutputSerializationAdapter.INSTANCE, inputSteam);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}
