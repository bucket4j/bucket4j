package io.github.bucket4j.distributed.serialization;

import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.remote.Request;
import io.github.bucket4j.distributed.versioning.Version;

import java.io.*;

public class InternalSerializationHelper {

    public static byte[] serializeState(RemoteBucketState state, Version backwardCompatibilityVersion) {
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(byteStream);

            RemoteBucketState.SERIALIZATION_HANDLE.serialize(DataOutputSerializationAdapter.INSTANCE, output, state, backwardCompatibilityVersion);

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
                return RemoteBucketState.SERIALIZATION_HANDLE.deserialize(DataOutputSerializationAdapter.INSTANCE, inputSteam, null);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static byte[] serializeRequest(Request<?> request) {
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(byteStream);

            Request.SERIALIZATION_HANDLE.serialize(DataOutputSerializationAdapter.INSTANCE, output, request, request.getBackwardCompatibilityVersion());
            
            output.close();
            byteStream.close();

            return byteStream.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static <T> Request<T> deserializeRequest(byte[] bytes) {
        try {
            try (DataInputStream inputSteam = new DataInputStream(new ByteArrayInputStream(bytes))) {
                return (Request<T>) Request.SERIALIZATION_HANDLE.deserialize(DataOutputSerializationAdapter.INSTANCE, inputSteam, null);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static byte[] serializeResult(CommandResult<?> result, Version backwardCompatibilityVersion) {
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(byteStream);

            CommandResult.SERIALIZATION_HANDLE.serialize(DataOutputSerializationAdapter.INSTANCE, output, result, backwardCompatibilityVersion);

            output.close();
            byteStream.close();

            return byteStream.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static <T> CommandResult<T> deserializeResult(byte[] bytes, Version backwardCompatibilityVersion) {
        try {
            try (DataInputStream inputSteam = new DataInputStream(new ByteArrayInputStream(bytes))) {
                return (CommandResult<T>) CommandResult.SERIALIZATION_HANDLE.deserialize(DataOutputSerializationAdapter.INSTANCE, inputSteam, backwardCompatibilityVersion);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}
