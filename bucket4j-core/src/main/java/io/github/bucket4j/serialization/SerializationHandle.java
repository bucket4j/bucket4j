package io.github.bucket4j.serialization;

import io.github.bucket4j.*;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.MultiResult;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.remote.commands.*;

import java.io.IOException;
import java.util.Arrays;

public interface SerializationHandle<T> {

    SerializationHandles CORE_HANDLES = new SerializationHandles(Arrays.asList(
            Bandwidth.SERIALIZATION_HANDLE,
            BucketConfiguration.SERIALIZATION_HANDLE,
            BucketState64BitsInteger.SERIALIZATION_HANDLE, // TODO test
            BucketState64BitsInteger.SERIALIZATION_HANDLE, // TODO test
            RemoteBucketState.SERIALIZATION_HANDLE,

            ReserveAndCalculateTimeToSleepCommand.SERIALIZATION_HANDLE,
            AddTokensCommand.SERIALIZATION_HANDLE,
            ConsumeAsMuchAsPossibleCommand.SERIALIZATION_HANDLE,
            CreateSnapshotCommand.SERIALIZATION_HANDLE,
            GetAvailableTokensCommand.SERIALIZATION_HANDLE,
            EstimateAbilityToConsumeCommand.SERIALIZATION_HANDLE,
            TryConsumeCommand.SERIALIZATION_HANDLE,
            TryConsumeAndReturnRemainingTokensCommand.SERIALIZATION_HANDLE,
            ReplaceConfigurationOrReturnPreviousCommand.SERIALIZATION_HANDLE,
            GetConfigurationCommand.SERIALIZATION_HANDLE, // TODO test
            MultiCommand.SERIALIZATION_HANDLE, // TODO test

            CommandResult.SERIALIZATION_HANDLE,
            ConsumptionProbe.SERIALIZATION_HANDLE,
            EstimationProbe.SERIALIZATION_HANDLE,
            MultiResult.SERIALIZATION_HANDLE // TODO test
    ));

    <I> T deserialize(DeserializationAdapter<I> adapter, I input) throws IOException;

    <O> void serialize(SerializationAdapter<O> adapter, O output, T serializableObject) throws IOException;

    /**
     * @return the type identifier that is unique across all Bucket4j classes
     */
    int getTypeId();

    Class<T> getSerializedType();

}
