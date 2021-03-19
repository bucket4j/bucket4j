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
package io.github.bucket4j;

import io.github.bucket4j.grid.*;
import io.github.bucket4j.local.LocalBucketBuilder;
import io.github.bucket4j.serialization.SerializationHandle;

import java.util.*;

/**
 * This is entry point for functionality provided bucket4j library.
 */
public class Bucket4j {

    private static final Map<Class<?>, Extension<?>> extensions;
    static {
        extensions = new HashMap<>();
        for (Extension<?> extension : ServiceLoader.load(Extension.class)) {
            extensions.put(extension.getClass(), extension);
        }
    }

    private static final List<SerializationHandle<?>> serializationHandles;
    static {
        Map<Integer, SerializationHandle<?>> serializersById = new HashMap<>();

        List<SerializationHandle<?>> coreHandles = Arrays.asList(
                Bandwidth.SERIALIZATION_HANDLE,
                BucketConfiguration.SERIALIZATION_HANDLE,
                BucketState.SERIALIZATION_HANDLE,
                GridBucketState.SERIALIZATION_HANDLE,
                ReserveAndCalculateTimeToSleepCommand.SERIALIZATION_HANDLE,
                AddTokensCommand.SERIALIZATION_HANDLE,
                ConsumeAsMuchAsPossibleCommand.SERIALIZATION_HANDLE,
                CreateSnapshotCommand.SERIALIZATION_HANDLE,
                GetAvailableTokensCommand.SERIALIZATION_HANDLE,
                EstimateAbilityToConsumeCommand.SERIALIZATION_HANDLE,
                TryConsumeCommand.SERIALIZATION_HANDLE,
                TryConsumeAndReturnRemainingTokensCommand.SERIALIZATION_HANDLE,
                ReplaceConfigurationCommand.SERIALIZATION_HANDLE,
                ConsumeIgnoringRateLimitsCommand.SERIALIZATION_HANDLE,
                CommandResult.SERIALIZATION_HANDLE,
                ConsumptionProbe.SERIALIZATION_HANDLE,
                EstimationProbe.SERIALIZATION_HANDLE,
                VerboseResult.SERIALIZATION_HANDLE,
                VerboseCommand.SERIALIZATION_HANDLE,
                ForceAddTokensCommand.SERIALIZATION_HANDLE
        );

        List<SerializationHandle<?>> allHandles = new ArrayList<>(coreHandles);
        for (Extension<?> extension : extensions.values()) {
            allHandles.addAll(extension.getSerializers());
        }

        for (SerializationHandle<?> coreHandle : allHandles) {
            int typeId = coreHandle.getTypeId();
            SerializationHandle<?> conflictingHandle = serializersById.get(typeId);
            if (conflictingHandle != null) {
                String msg = "Serialization ID " + typeId + " duplicated for " + coreHandle + " and " + conflictingHandle;
                throw new IllegalArgumentException(msg);
            }
            serializersById.put(typeId, coreHandle);
        }

        serializationHandles = Collections.unmodifiableList(allHandles);
    }

    public static List<SerializationHandle<?>> getSerializationHandles() {
        return serializationHandles;
    }

    private Bucket4j() {
        // to avoid initialization of utility class
    }

    /**
     * Creates the new builder of in-memory buckets.
     *
     * @return new instance of {@link LocalBucketBuilder}
     */
    public static LocalBucketBuilder builder() {
        return new LocalBucketBuilder();
    }

    /**
     * Creates new instance of {@link ConfigurationBuilder}
     *
     * @return instance of {@link ConfigurationBuilder}
     */
    public static ConfigurationBuilder configurationBuilder() {
        return new ConfigurationBuilder();
    }

    /**
     * Locates Bucket4j extension by class {@code extensionClass}.
     *
     * @param extensionClass must be registered in "/META-INF/services/io.github.bucket4j.Extension" according to java SPI rules.
     * @param <T>
     * @param <E>
     *
     * @return library extension
     */
    public static <T extends AbstractBucketBuilder<T>, E extends Extension<T>> E extension(Class<E> extensionClass) {
        E extension = (E) extensions.get(extensionClass);
        if (extension == null) {
            String msg = "extension with class [" + extensionClass + "] is not registered";
            throw new IllegalArgumentException(msg);
        }
        return extension;
    }

}
