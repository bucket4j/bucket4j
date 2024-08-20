/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2024 Vladimir Bukhtoyarov
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
package io.github.bucket4j.grid.hazelcast.serialization;

import java.text.MessageFormat;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import com.hazelcast.config.SerializationConfig;
import com.hazelcast.config.SerializerConfig;
import com.hazelcast.internal.util.StringUtil;
import com.hazelcast.nio.serialization.Serializer;

import io.github.bucket4j.grid.hazelcast.HazelcastEntryProcessor;
import io.github.bucket4j.grid.hazelcast.HazelcastOffloadableEntryProcessor;
import io.github.bucket4j.grid.hazelcast.SimpleBackupProcessor;
import io.github.bucket4j.grid.hazelcast.VersionedBackupProcessor;

public class SerializationUtilities {
    public static final String TYPE_ID_BASE_PROP_NAME = "bucket4j.hazelcast.serializer.type_id_base";
    private static final Map<Class<? extends Serializer>, Integer> serializerTypeIdOffsets = Map.ofEntries(
            new AbstractMap.SimpleEntry<Class<? extends Serializer>, Integer>(HazelcastEntryProcessorSerializer.class, 0),
            new AbstractMap.SimpleEntry<Class<? extends Serializer>, Integer>(SimpleBackupProcessorSerializer.class, 1),
            new AbstractMap.SimpleEntry<Class<? extends Serializer>, Integer>(HazelcastOffloadableEntryProcessorSerializer.class, 2),
            new AbstractMap.SimpleEntry<Class<? extends Serializer>, Integer>(VersionedBackupProcessorSerializer.class, 3)
    );

    /**
     * Registers custom Hazelcast serializers for all classes from Bucket4j library which can be transferred over network.
     * Each serializer will have different typeId, and this id will not be changed in the feature releases.
     *
     * <p>
     *     <strong>Note:</strong> it would be better to leave an empty space in the Ids in order to handle the extension of Bucket4j library when new classes can be added to library.
     *     For example if you called {@code getAllSerializers(10000)} then it would be reasonable to avoid registering your custom types in the interval 10000-10100.
     * </p>
     *
     * @param typeIdBase a starting number from for typeId sequence
     */
    public static void addCustomSerializers(SerializationConfig serializationConfig, final int typeIdBase) {
        serializationConfig.addSerializerConfig(
            new SerializerConfig()
                .setImplementation(new HazelcastEntryProcessorSerializer(SerializationUtilities.getSerializerTypeId(HazelcastEntryProcessorSerializer.class, typeIdBase)))
                .setTypeClass(HazelcastEntryProcessor.class)
        );

        serializationConfig.addSerializerConfig(
            new SerializerConfig()
                .setImplementation(new SimpleBackupProcessorSerializer(SerializationUtilities.getSerializerTypeId(SimpleBackupProcessorSerializer.class, typeIdBase)))
                .setTypeClass(SimpleBackupProcessor.class)
        );

        serializationConfig.addSerializerConfig(
            new SerializerConfig()
                .setImplementation(new HazelcastOffloadableEntryProcessorSerializer(SerializationUtilities.getSerializerTypeId(HazelcastOffloadableEntryProcessorSerializer.class, typeIdBase)))
                .setTypeClass(HazelcastOffloadableEntryProcessor.class)
        );

        serializationConfig.addSerializerConfig(
            new SerializerConfig()
                .setImplementation(new VersionedBackupProcessorSerializer(SerializationUtilities.getSerializerTypeId(VersionedBackupProcessorSerializer.class, typeIdBase)))
                .setTypeClass(VersionedBackupProcessor.class)
        );

    }

    public static int getSerializerTypeId(Class<? extends Serializer> serializerType) {
        Optional<Integer> typeIdBase = getSerializersTypeIdBase();

        if (typeIdBase.isEmpty()) {
            String msg = MessageFormat.format("Missing TypeIdBase number, impossible to load Bucket4j custom serializers. It must be provided in form of Environment Variable or System Property, both using the following key: [{0}]", TYPE_ID_BASE_PROP_NAME);
            throw new MissingConfigurationParameterException(msg);
        }

        return getSerializerTypeId(serializerType, typeIdBase.get());
    }

    public static int getSerializerTypeId(Class<? extends Serializer> serializerType, int typeIdBase) {
        return typeIdBase + SerializationUtilities.getSerializerTypeIdOffset(serializerType);
    }

    private static Optional<Integer> getSerializersTypeIdBase() {
        return Optional.ofNullable(
                getSerializerTypeIdBaseFromSystemProperty()
                        .orElseGet(() -> getSerializerTypeIdBaseFromEnvironmentVariable()
                                .orElse(null)));
    }

    private static int getSerializerTypeIdOffset(Class<? extends Serializer> serializerType) {
        if (serializerTypeIdOffsets.containsKey(serializerType)) {
            return serializerTypeIdOffsets.get(serializerType);
        } else {
            String msg = MessageFormat.format("The internal configuration does not include any offset for the serializerType [{0}]", serializerType);
            throw new IllegalStateException(msg);
        }
    }

    private static Optional<Integer> getSerializerTypeIdBaseFromEnvironmentVariable() {
        return getPropertyValueFromExternal(() -> System.getenv(SerializationUtilities.TYPE_ID_BASE_PROP_NAME), "Environment Variable");
    }

    private static Optional<Integer> getSerializerTypeIdBaseFromSystemProperty() {
        return getPropertyValueFromExternal(() -> System.getProperty(SerializationUtilities.TYPE_ID_BASE_PROP_NAME), "System Property");
    }

    private static Optional<Integer> getPropertyValueFromExternal(Supplier<String> typeIdSupplier, String source) {
        Optional<Integer> retVal = Optional.empty();

        String typeIdBaseStr = typeIdSupplier.get();
        if (!StringUtil.isNullOrEmptyAfterTrim(typeIdBaseStr)) {
            retVal = parseInteger(typeIdBaseStr);
            if (retVal.isEmpty()) {
                String msg = MessageFormat.format("The {0} [{1}] has an invalid format. It must be a positive Integer.", source, TYPE_ID_BASE_PROP_NAME);
                throw new InvalidConfigurationParameterException(msg);
            }
        }

        return retVal;
    }

    private static Optional<Integer> parseInteger(String strNum) {
        Optional<Integer> retVal = Optional.empty();
        if (null != strNum) {
            try {
                Integer d = Integer.parseInt(strNum.trim());
                retVal = Optional.of(d);
            } catch (NumberFormatException nfe) {
                retVal = Optional.empty();
            }
        }
        return retVal;
    }
}
