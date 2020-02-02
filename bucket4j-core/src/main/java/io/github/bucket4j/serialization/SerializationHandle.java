package io.github.bucket4j.serialization;

import io.github.bucket4j.*;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.MultiResult;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.remote.RemoteRequest;
import io.github.bucket4j.distributed.remote.commands.*;

import java.io.IOException;
import java.util.Arrays;

public interface SerializationHandle<T> {

    SerializationHandles CORE_HANDLES = new SerializationHandles(Arrays.asList(
            Bandwidth.SERIALIZATION_HANDLE, // 1
            BucketConfiguration.SERIALIZATION_HANDLE, // 2
            BucketState64BitsInteger.SERIALIZATION_HANDLE, // 3
            BucketStateIEEE754.SERIALIZATION_HANDLE, // 4
            RemoteBucketState.SERIALIZATION_HANDLE, // 5

            CommandResult.SERIALIZATION_HANDLE, // 10
            ConsumptionProbe.SERIALIZATION_HANDLE, // 11
            EstimationProbe.SERIALIZATION_HANDLE, // 12
            MultiResult.SERIALIZATION_HANDLE, // 13

            CreateInitialStateCommand.SERIALIZATION_HANDLE, // 20
            CreateInitialStateAndExecuteCommand.SERIALIZATION_HANDLE, // 21
            MultiCommand.SERIALIZATION_HANDLE, // 22
            ReserveAndCalculateTimeToSleepCommand.SERIALIZATION_HANDLE, // 23
            AddTokensCommand.SERIALIZATION_HANDLE, // 24
            ConsumeAsMuchAsPossibleCommand.SERIALIZATION_HANDLE, // 25
            CreateSnapshotCommand.SERIALIZATION_HANDLE, // 26
            GetAvailableTokensCommand.SERIALIZATION_HANDLE, // 27
            EstimateAbilityToConsumeCommand.SERIALIZATION_HANDLE, // 28
            TryConsumeCommand.SERIALIZATION_HANDLE, // 29
            TryConsumeAndReturnRemainingTokensCommand.SERIALIZATION_HANDLE, // 30
            ReplaceConfigurationOrReturnPreviousCommand.SERIALIZATION_HANDLE, // 32
            GetConfigurationCommand.SERIALIZATION_HANDLE, // 33
            RemoteRequest.SERIALIZATION_HANDLE //34
    ));

    <I> T deserialize(DeserializationAdapter<I> adapter, I input) throws IOException;

    <O> void serialize(SerializationAdapter<O> adapter, O output, T serializableObject) throws IOException;

    /**
     * @return the type identifier that is unique across all Bucket4j classes
     */
    int getTypeId();

    Class<T> getSerializedType();

}
