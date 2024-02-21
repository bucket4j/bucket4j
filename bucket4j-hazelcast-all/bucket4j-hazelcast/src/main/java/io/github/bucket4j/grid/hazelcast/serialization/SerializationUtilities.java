package io.github.bucket4j.grid.hazelcast.serialization;

import com.hazelcast.internal.util.StringUtil;
import com.hazelcast.nio.serialization.Serializer;

import java.text.MessageFormat;
import java.util.*;
import java.util.function.Supplier;

public class SerializationUtilities {
    public static final String TYPE_ID_BASE_PROP_NAME = "Bucket4jSerializers_TypeId_Base";
    private static final Map<Class<? extends Serializer>, Integer> serializerTypeIdOffsets = Map.ofEntries(
            new AbstractMap.SimpleEntry<Class<? extends Serializer>, Integer>(HazelcastEntryProcessorSerializer.class, 0),
            new AbstractMap.SimpleEntry<Class<? extends Serializer>, Integer>(SimpleBackupProcessorSerializer.class, 1),
            new AbstractMap.SimpleEntry<Class<? extends Serializer>, Integer>(HazelcastOffloadableEntryProcessorSerializer.class, 2)
    );

    public static int getSerializerTypeId(Class<? extends Serializer> serializerType) {
        var typeIdBase = getSerializersTypeIdBase();

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
                                .orElseGet(() -> null)));
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