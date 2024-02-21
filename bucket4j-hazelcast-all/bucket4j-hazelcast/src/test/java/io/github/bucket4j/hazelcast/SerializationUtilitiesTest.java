package io.github.bucket4j.hazelcast;

import com.hazelcast.internal.serialization.impl.defaultserializers.ArrayBlockingQueueStreamSerializer;
import io.github.bucket4j.grid.hazelcast.serialization.*;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearEnvironmentVariable;
import org.junitpioneer.jupiter.ClearSystemProperty;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.junitpioneer.jupiter.SetSystemProperty;

import static org.junit.jupiter.api.Assertions.*;

public class SerializationUtilitiesTest {

    private static final String TYPE_ID_BASE_ENV_VAR_VALUE_VALID = "12345";
    private static final String TYPE_ID_BASE_ENV_VAR_VALUE_INVALID = "12?45";
    private static final String TYPE_ID_BASE_SYS_PROP_VALUE_VALID = "56789";
    private static final String TYPE_ID_BASE_SYS_PROP_VALUE_INVALID = "56?89";

    // ************************************************************************************
    // TYPE_ID
    // Read each, for expected and unexpected serializers 
    // ************************************************************************************
    @Test
    public void SerializationUtilities_Expected_Serializers_with_explicit_typeIdBase_Test() {
        assertEquals(10, SerializationUtilities.getSerializerTypeId(HazelcastEntryProcessorSerializer.class, 10));
        assertEquals(11, SerializationUtilities.getSerializerTypeId(SimpleBackupProcessorSerializer.class, 10));
        assertEquals(12, SerializationUtilities.getSerializerTypeId(HazelcastOffloadableEntryProcessorSerializer.class, 10));
    }
    @Test
    public void SerializationUtilities_UNExpected_Serializer_with_explicit_typeIdBase_Test() {
        assertThrowsExactly(IllegalStateException.class, () -> { int typeId = SerializationUtilities.getSerializerTypeId(ArrayBlockingQueueStreamSerializer.class, 10); });
    }


    // ************************************************************************************
    // TYPE_ID BASE NOT CONFIGURED
    // Variable Source  ::  NONE
    // ************************************************************************************
    @Test
    @ClearSystemProperty(key = SerializationUtilities.TYPE_ID_BASE_PROP_NAME)
    @ClearEnvironmentVariable(key = SerializationUtilities.TYPE_ID_BASE_PROP_NAME)
    public void SerializationUtilities_TypeIdBase_Not_Configured_Test() {
        assertThrowsExactly(MissingConfigurationParameterException.class, () -> { int typeId = SerializationUtilities.getSerializerTypeId(HazelcastEntryProcessorSerializer.class); });
        assertThrowsExactly(MissingConfigurationParameterException.class, () -> { int typeId = SerializationUtilities.getSerializerTypeId(SimpleBackupProcessorSerializer.class); });
        assertThrowsExactly(MissingConfigurationParameterException.class, () -> { int typeId = SerializationUtilities.getSerializerTypeId(HazelcastOffloadableEntryProcessorSerializer.class); });
    }


    // ************************************************************************************
    // TYPE_ID BASE
    // Variable Source  ::  ENVIRONMENT VARIABLES
    // ************************************************************************************
    @Test
    @ClearSystemProperty(key = SerializationUtilities.TYPE_ID_BASE_PROP_NAME)
    @SetEnvironmentVariable(key = SerializationUtilities.TYPE_ID_BASE_PROP_NAME, value = TYPE_ID_BASE_ENV_VAR_VALUE_VALID)
    public void SerializationUtilities_TypeIdBase_Configured_Valid_As_Env_Test() {
        assertEquals(TYPE_ID_BASE_ENV_VAR_VALUE_VALID, System.getenv(SerializationUtilities.TYPE_ID_BASE_PROP_NAME));

        assertEquals(Integer.parseInt(TYPE_ID_BASE_ENV_VAR_VALUE_VALID)+0, SerializationUtilities.getSerializerTypeId(HazelcastEntryProcessorSerializer.class));
        assertEquals(Integer.parseInt(TYPE_ID_BASE_ENV_VAR_VALUE_VALID)+1, SerializationUtilities.getSerializerTypeId(SimpleBackupProcessorSerializer.class));
        assertEquals(Integer.parseInt(TYPE_ID_BASE_ENV_VAR_VALUE_VALID)+2, SerializationUtilities.getSerializerTypeId(HazelcastOffloadableEntryProcessorSerializer.class));
    }
    @Test
    @ClearSystemProperty(key = SerializationUtilities.TYPE_ID_BASE_PROP_NAME)
    @SetEnvironmentVariable(key = SerializationUtilities.TYPE_ID_BASE_PROP_NAME, value = TYPE_ID_BASE_ENV_VAR_VALUE_INVALID)
    public void SerializationUtilities_TypeIdBase_Configured_Invalid_As_Env_Test() {
        assertEquals(TYPE_ID_BASE_ENV_VAR_VALUE_INVALID, System.getenv(SerializationUtilities.TYPE_ID_BASE_PROP_NAME));

        assertThrowsExactly(InvalidConfigurationParameterException.class, () -> { int typeId = SerializationUtilities.getSerializerTypeId(HazelcastEntryProcessorSerializer.class); });
        assertThrowsExactly(InvalidConfigurationParameterException.class, () -> { int typeId = SerializationUtilities.getSerializerTypeId(SimpleBackupProcessorSerializer.class); });
        assertThrowsExactly(InvalidConfigurationParameterException.class, () -> { int typeId = SerializationUtilities.getSerializerTypeId(HazelcastOffloadableEntryProcessorSerializer.class); });
    }


    // ************************************************************************************
    // TYPE_ID BASE
    // Variable Source  ::  SYSTEM PROPERTIES
    // ************************************************************************************
    @Test
    @ClearEnvironmentVariable(key = SerializationUtilities.TYPE_ID_BASE_PROP_NAME)
    @SetSystemProperty(key = SerializationUtilities.TYPE_ID_BASE_PROP_NAME, value = TYPE_ID_BASE_SYS_PROP_VALUE_VALID)
    public void SerializationUtilities_TypeIdBase_Configured_Valid_As_SysProp_Test() {
        assertEquals(TYPE_ID_BASE_SYS_PROP_VALUE_VALID, System.getProperty(SerializationUtilities.TYPE_ID_BASE_PROP_NAME));

        assertEquals(Integer.parseInt(TYPE_ID_BASE_SYS_PROP_VALUE_VALID)+0, SerializationUtilities.getSerializerTypeId(HazelcastEntryProcessorSerializer.class));
        assertEquals(Integer.parseInt(TYPE_ID_BASE_SYS_PROP_VALUE_VALID)+1, SerializationUtilities.getSerializerTypeId(SimpleBackupProcessorSerializer.class));
        assertEquals(Integer.parseInt(TYPE_ID_BASE_SYS_PROP_VALUE_VALID)+2, SerializationUtilities.getSerializerTypeId(HazelcastOffloadableEntryProcessorSerializer.class));
    }
    @Test
    @ClearEnvironmentVariable(key = SerializationUtilities.TYPE_ID_BASE_PROP_NAME)
    @SetSystemProperty(key = SerializationUtilities.TYPE_ID_BASE_PROP_NAME, value = TYPE_ID_BASE_SYS_PROP_VALUE_INVALID)
    public void SerializationUtilities_TypeIdBase_Configured_Invalid_As_SysProp_Test() {
        assertEquals(TYPE_ID_BASE_SYS_PROP_VALUE_INVALID, System.getProperty(SerializationUtilities.TYPE_ID_BASE_PROP_NAME));

        assertThrowsExactly(InvalidConfigurationParameterException.class, () -> { int typeId = SerializationUtilities.getSerializerTypeId(HazelcastEntryProcessorSerializer.class); });
        assertThrowsExactly(InvalidConfigurationParameterException.class, () -> { int typeId = SerializationUtilities.getSerializerTypeId(SimpleBackupProcessorSerializer.class); });
        assertThrowsExactly(InvalidConfigurationParameterException.class, () -> { int typeId = SerializationUtilities.getSerializerTypeId(HazelcastOffloadableEntryProcessorSerializer.class); });
    }



    // ************************************************************************************
    // TYPE_ID BASE
    // BOTH Variable Source simultaneously present
    // ************************************************************************************
    @Test
    @SetEnvironmentVariable(key = SerializationUtilities.TYPE_ID_BASE_PROP_NAME, value = TYPE_ID_BASE_ENV_VAR_VALUE_VALID)
    @SetSystemProperty(key = SerializationUtilities.TYPE_ID_BASE_PROP_NAME, value = TYPE_ID_BASE_SYS_PROP_VALUE_VALID)
    public void SerializationUtilities_TypeIdBase_Configured_Valid_SysProp_Valid_EnvVar_Test() {
        assertEquals(TYPE_ID_BASE_ENV_VAR_VALUE_VALID, System.getenv(SerializationUtilities.TYPE_ID_BASE_PROP_NAME));
        assertEquals(TYPE_ID_BASE_SYS_PROP_VALUE_VALID, System.getProperty(SerializationUtilities.TYPE_ID_BASE_PROP_NAME));

        assertEquals(Integer.parseInt(TYPE_ID_BASE_SYS_PROP_VALUE_VALID)+0, SerializationUtilities.getSerializerTypeId(HazelcastEntryProcessorSerializer.class));
        assertEquals(Integer.parseInt(TYPE_ID_BASE_SYS_PROP_VALUE_VALID)+1, SerializationUtilities.getSerializerTypeId(SimpleBackupProcessorSerializer.class));
        assertEquals(Integer.parseInt(TYPE_ID_BASE_SYS_PROP_VALUE_VALID)+2, SerializationUtilities.getSerializerTypeId(HazelcastOffloadableEntryProcessorSerializer.class));
    }

    @Test
    @SetEnvironmentVariable(key = SerializationUtilities.TYPE_ID_BASE_PROP_NAME, value = TYPE_ID_BASE_ENV_VAR_VALUE_INVALID)
    @SetSystemProperty(key = SerializationUtilities.TYPE_ID_BASE_PROP_NAME, value = TYPE_ID_BASE_SYS_PROP_VALUE_VALID)
    public void SerializationUtilities_TypeIdBase_Configured_Valid_SysProp_INValid_EnvVar_Test() {
        assertEquals(TYPE_ID_BASE_ENV_VAR_VALUE_INVALID, System.getenv(SerializationUtilities.TYPE_ID_BASE_PROP_NAME));
        assertEquals(TYPE_ID_BASE_SYS_PROP_VALUE_VALID, System.getProperty(SerializationUtilities.TYPE_ID_BASE_PROP_NAME));

        assertEquals(Integer.parseInt(TYPE_ID_BASE_SYS_PROP_VALUE_VALID)+0, SerializationUtilities.getSerializerTypeId(HazelcastEntryProcessorSerializer.class));
        assertEquals(Integer.parseInt(TYPE_ID_BASE_SYS_PROP_VALUE_VALID)+1, SerializationUtilities.getSerializerTypeId(SimpleBackupProcessorSerializer.class));
        assertEquals(Integer.parseInt(TYPE_ID_BASE_SYS_PROP_VALUE_VALID)+2, SerializationUtilities.getSerializerTypeId(HazelcastOffloadableEntryProcessorSerializer.class));
    }

    @Test
    @SetEnvironmentVariable(key = SerializationUtilities.TYPE_ID_BASE_PROP_NAME, value = TYPE_ID_BASE_ENV_VAR_VALUE_VALID)
    @SetSystemProperty(key = SerializationUtilities.TYPE_ID_BASE_PROP_NAME, value = TYPE_ID_BASE_SYS_PROP_VALUE_INVALID)
    public void SerializationUtilities_TypeIdBase_Configured_INValid_SysProp_Valid_EnvVar_Test() {
        assertEquals(TYPE_ID_BASE_ENV_VAR_VALUE_VALID, System.getenv(SerializationUtilities.TYPE_ID_BASE_PROP_NAME));
        assertEquals(TYPE_ID_BASE_SYS_PROP_VALUE_INVALID, System.getProperty(SerializationUtilities.TYPE_ID_BASE_PROP_NAME));

        assertThrowsExactly(InvalidConfigurationParameterException.class, () -> { int typeId = SerializationUtilities.getSerializerTypeId(HazelcastEntryProcessorSerializer.class); });
        assertThrowsExactly(InvalidConfigurationParameterException.class, () -> { int typeId = SerializationUtilities.getSerializerTypeId(SimpleBackupProcessorSerializer.class); });
        assertThrowsExactly(InvalidConfigurationParameterException.class, () -> { int typeId = SerializationUtilities.getSerializerTypeId(HazelcastOffloadableEntryProcessorSerializer.class); });
    }

    @Test
    @SetEnvironmentVariable(key = SerializationUtilities.TYPE_ID_BASE_PROP_NAME, value = TYPE_ID_BASE_ENV_VAR_VALUE_INVALID)
    @SetSystemProperty(key = SerializationUtilities.TYPE_ID_BASE_PROP_NAME, value = TYPE_ID_BASE_SYS_PROP_VALUE_INVALID)
    public void SerializationUtilities_TypeIdBase_Configured_INValid_SysProp_INValid_EnvVar_Test() {
        assertEquals(TYPE_ID_BASE_ENV_VAR_VALUE_INVALID, System.getenv(SerializationUtilities.TYPE_ID_BASE_PROP_NAME));
        assertEquals(TYPE_ID_BASE_SYS_PROP_VALUE_INVALID, System.getProperty(SerializationUtilities.TYPE_ID_BASE_PROP_NAME));

        assertThrowsExactly(InvalidConfigurationParameterException.class, () -> { int typeId = SerializationUtilities.getSerializerTypeId(HazelcastEntryProcessorSerializer.class); });
        assertThrowsExactly(InvalidConfigurationParameterException.class, () -> { int typeId = SerializationUtilities.getSerializerTypeId(SimpleBackupProcessorSerializer.class); });
        assertThrowsExactly(InvalidConfigurationParameterException.class, () -> { int typeId = SerializationUtilities.getSerializerTypeId(HazelcastOffloadableEntryProcessorSerializer.class); });
    }


}
