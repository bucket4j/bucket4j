package io.github.bucket4j.distributed.serialization;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.BucketState;
import io.github.bucket4j.MathType;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.remote.RemoteStat;
import io.github.bucket4j.distributed.remote.Request;
import io.github.bucket4j.distributed.remote.commands.GetAvailableTokensCommand;
import io.github.bucket4j.distributed.versioning.Versions;
import io.github.bucket4j.util.ComparableByContent;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InternalSerializationHelperTest {

    private final BucketConfiguration configuration = new BucketConfiguration(Arrays.asList(
            Bandwidth.simple(10, Duration.ofSeconds(42))
    ));
    private final BucketState state = BucketState.createInitialState(configuration, MathType.INTEGER_64_BITS, System.nanoTime());
    private final RemoteBucketState remoteBucketState = new RemoteBucketState(state, new RemoteStat(42), null);
    private final Request<Long> request = new Request<>(new GetAvailableTokensCommand(), Versions.getLatest(), null, null);
    private final CommandResult<Long> result = CommandResult.success(42L, PrimitiveSerializationHandles.LONG_HANDLE);

    @Test
    public void defaultStyleRoundTripsState() {
        byte[] bytes = InternalSerializationHelper.serializeState(remoteBucketState, Versions.getLatest());
        RemoteBucketState deserialized = InternalSerializationHelper.deserializeState(bytes);
        assertTrue(ComparableByContent.equals(remoteBucketState, deserialized));
    }

    @Test
    public void bothStylesProduceIdenticalBytesAndRoundTrip_forState() {
        byte[] dataOutputBytes = InternalSerializationHelper.serializeState(remoteBucketState, Versions.getLatest(), SerializationStyle.DATA_OUTPUT);
        byte[] byteBufferBytes = InternalSerializationHelper.serializeState(remoteBucketState, Versions.getLatest(), SerializationStyle.BYTE_BUFFER);
        assertArrayEquals(dataOutputBytes, byteBufferBytes);

        RemoteBucketState fromDataOutput = InternalSerializationHelper.deserializeState(byteBufferBytes, SerializationStyle.DATA_OUTPUT);
        RemoteBucketState fromByteBuffer = InternalSerializationHelper.deserializeState(dataOutputBytes, SerializationStyle.BYTE_BUFFER);
        assertTrue(ComparableByContent.equals(remoteBucketState, fromDataOutput));
        assertTrue(ComparableByContent.equals(remoteBucketState, fromByteBuffer));
    }

    @Test
    public void bothStylesProduceIdenticalBytesAndRoundTrip_forRequest() {
        byte[] dataOutputBytes = InternalSerializationHelper.serializeRequest(request, SerializationStyle.DATA_OUTPUT);
        byte[] byteBufferBytes = InternalSerializationHelper.serializeRequest(request, SerializationStyle.BYTE_BUFFER);
        assertArrayEquals(dataOutputBytes, byteBufferBytes);

        Request<Long> fromDataOutput = InternalSerializationHelper.deserializeRequest(byteBufferBytes, SerializationStyle.DATA_OUTPUT);
        Request<Long> fromByteBuffer = InternalSerializationHelper.deserializeRequest(dataOutputBytes, SerializationStyle.BYTE_BUFFER);
        assertTrue(ComparableByContent.equals(request, fromDataOutput));
        assertTrue(ComparableByContent.equals(request, fromByteBuffer));
    }

    @Test
    public void bothStylesProduceIdenticalBytesAndRoundTrip_forResult() {
        byte[] dataOutputBytes = InternalSerializationHelper.serializeResult(result, Versions.getLatest(), SerializationStyle.DATA_OUTPUT);
        byte[] byteBufferBytes = InternalSerializationHelper.serializeResult(result, Versions.getLatest(), SerializationStyle.BYTE_BUFFER);
        assertArrayEquals(dataOutputBytes, byteBufferBytes);

        CommandResult<Long> fromDataOutput = InternalSerializationHelper.deserializeResult(byteBufferBytes, Versions.getLatest(), SerializationStyle.DATA_OUTPUT);
        CommandResult<Long> fromByteBuffer = InternalSerializationHelper.deserializeResult(dataOutputBytes, Versions.getLatest(), SerializationStyle.BYTE_BUFFER);
        assertTrue(ComparableByContent.equals(result, fromDataOutput));
        assertTrue(ComparableByContent.equals(result, fromByteBuffer));
    }

}
