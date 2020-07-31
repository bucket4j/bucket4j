/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2020 Vladimir Bukhtoyarov
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package io.github.bucket4j.serialization;

import io.github.bucket4j.*;
import io.github.bucket4j.distributed.remote.*;
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
            RemoteStat.SERIALIZATION_HANDLE, // 6

            CommandResult.SERIALIZATION_HANDLE, // 10
            ConsumptionProbe.SERIALIZATION_HANDLE, // 11
            EstimationProbe.SERIALIZATION_HANDLE, // 12
            MultiResult.SERIALIZATION_HANDLE, // 13
            RemoteVerboseResult.SERIALIZATION_HANDLE, // 14

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
            ConsumeIgnoringRateLimitsCommand.SERIALIZATION_HANDLE, // 34
            VerboseCommand.SERIALIZATION_HANDLE, // 35
            SyncCommand.SERIALIZATION_HANDLE // 36
    ));

    <I> T deserialize(DeserializationAdapter<I> adapter, I input) throws IOException;

    <O> void serialize(SerializationAdapter<O> adapter, O output, T serializableObject) throws IOException;

    /**
     * @return the type identifier that is unique across all Bucket4j classes
     */
    int getTypeId();

    Class<T> getSerializedType();

}
