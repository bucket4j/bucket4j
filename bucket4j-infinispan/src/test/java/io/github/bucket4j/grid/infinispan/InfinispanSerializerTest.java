package io.github.bucket4j.grid.infinispan;

import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.remote.commands.AddTokensCommand;
import io.github.bucket4j.grid.infinispan.serialization.Bucket4jProtobufContextInitializer;
import io.github.bucket4j.grid.infinispan.serialization.ProtobufMessageMarshaller;
import io.github.bucket4j.serialization.AbstractSerializationTest;
import io.github.bucket4j.serialization.SerializationHandle;
import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.MessageMarshaller;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static io.github.bucket4j.Bandwidth.simple;
import static java.time.Duration.ofSeconds;

public class InfinispanSerializerTest extends AbstractSerializationTest {


    private static Map<Class<?>, ProtobufMessageMarshaller<?>> serializerByClass = new HashMap<>();

    @Before
    public void setup() {
        for (SerializationHandle<?> serializationHandle : Bucket4jProtobufContextInitializer.handles.getAllHandles()) {
            String protoTypeId = "bucket4j.Bucket4jType_" + serializationHandle.getTypeId();
            serializerByClass.put(serializationHandle.getSerializedType(), new ProtobufMessageMarshaller<>(serializationHandle, protoTypeId));
        }
    }

    @Override
    protected <T> T serializeAndDeserialize(T original) {
        try {
            HashMap<String, Object> data = new HashMap<>();
            MockProtoStreamWriter writer = new MockProtoStreamWriter(data);
            MockProtoStreamReader reader = new MockProtoStreamReader(data);

            ProtobufMessageMarshaller marshaller = serializerByClass.get(original.getClass());
            marshaller.writeTo(writer, original);
            return (T) marshaller.readFrom(reader);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Test
    public void tetsSerializationOfEntryProcessors() {
        BucketConfiguration configuration = Bucket4j.configurationBuilder()
                .addLimit(simple(10, ofSeconds(1)))
                .build();
        RemoteCommand command = new AddTokensCommand(42);

        testSerialization(new InfinispanProcessor<>(command));
    }


    public static class MockProtoStreamWriter implements MessageMarshaller.ProtoStreamWriter {

        private final Map<String, Object> data;

        public MockProtoStreamWriter(Map<String, Object> data) {
            this.data = data;
        }

        @Override
        public ImmutableSerializationContext getSerializationContext() {
            return null;
        }

        @Override
        public void writeInt(String fieldName, int value) throws IOException {
            data.put(fieldName, value);
        }

        @Override
        public void writeInt(String fieldName, Integer value) throws IOException {
            data.put(fieldName, value);
        }

        @Override
        public void writeInts(String fieldName, int[] values) throws IOException {
            data.put(fieldName, values);
        }

        @Override
        public void writeLong(String fieldName, long value) throws IOException {
            data.put(fieldName, value);
        }

        @Override
        public void writeLong(String fieldName, Long value) throws IOException {
            data.put(fieldName, value);
        }

        @Override
        public void writeLongs(String fieldName, long[] values) throws IOException {
            data.put(fieldName, values);
        }

        @Override
        public void writeDate(String fieldName, Date value) throws IOException {
            data.put(fieldName, value);
        }

        @Override
        public void writeInstant(String fieldName, Instant value) throws IOException {
            data.put(fieldName, value);
        }

        @Override
        public void writeDouble(String fieldName, double value) throws IOException {
            data.put(fieldName, value);
        }

        @Override
        public void writeDouble(String fieldName, Double value) throws IOException {
            data.put(fieldName, value);
        }

        @Override
        public void writeDoubles(String fieldName, double[] values) throws IOException {
            data.put(fieldName, values);
        }

        @Override
        public void writeFloat(String fieldName, float value) throws IOException {
            data.put(fieldName, value);
        }

        @Override
        public void writeFloat(String fieldName, Float value) throws IOException {
            data.put(fieldName, value);
        }

        @Override
        public void writeFloats(String fieldName, float[] values) throws IOException {
            data.put(fieldName, values);
        }

        @Override
        public void writeBoolean(String fieldName, boolean value) throws IOException {
            data.put(fieldName, value);
        }

        @Override
        public void writeBoolean(String fieldName, Boolean value) throws IOException {
            data.put(fieldName, value);
        }

        @Override
        public void writeBooleans(String fieldName, boolean[] values) throws IOException {
            data.put(fieldName, values);
        }

        @Override
        public void writeString(String fieldName, String value) throws IOException {
            data.put(fieldName, value);
        }

        @Override
        public void writeBytes(String fieldName, byte[] value) throws IOException {
            data.put(fieldName, value);
        }

        @Override
        public void writeBytes(String fieldName, InputStream input) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public <E> void writeObject(String fieldName, E value, Class<? extends E> clazz) throws IOException {
            data.put(fieldName, value);
        }

        @Override
        public <E extends Enum<E>> void writeEnum(String fieldName, E value, Class<E> clazz) throws IOException {
            data.put(fieldName, value);
        }

        @Override
        public <E extends Enum<E>> void writeEnum(String fieldName, E value) throws IOException {
            data.put(fieldName, value);
        }

        @Override
        public <E> void writeCollection(String fieldName, Collection<? super E> collection, Class<E> elementClass) throws IOException {
            data.put(fieldName, collection);
        }

        @Override
        public <E> void writeArray(String fieldName, E[] array, Class<? extends E> elementClass) throws IOException {
            data.put(fieldName, array);
        }
    }

    public static class MockProtoStreamReader implements MessageMarshaller.ProtoStreamReader {

        private final Map<String, Object> data;

        public MockProtoStreamReader(Map<String, Object> data) {
            this.data = data;
        }

        @Override
        public ImmutableSerializationContext getSerializationContext() {
            return null;
        }

        @Override
        public Integer readInt(String fieldName) throws IOException {
            return (Integer) data.get(fieldName);
        }

        @Override
        public int[] readInts(String fieldName) throws IOException {
            return (int[]) data.get(fieldName);
        }

        @Override
        public Long readLong(String fieldName) throws IOException {
            return (Long) data.get(fieldName);
        }

        @Override
        public long[] readLongs(String fieldName) throws IOException {
            return (long[]) data.get(fieldName);
        }

        @Override
        public Date readDate(String fieldName) throws IOException {
            return (Date) data.get(fieldName);
        }

        @Override
        public Instant readInstant(String fieldName) throws IOException {
            return (Instant) data.get(fieldName);
        }

        @Override
        public Float readFloat(String fieldName) throws IOException {
            return (Float) data.get(fieldName);
        }

        @Override
        public float[] readFloats(String fieldName) throws IOException {
            return (float[]) data.get(fieldName);
        }

        @Override
        public Double readDouble(String fieldName) throws IOException {
            return (Double) data.get(fieldName);
        }

        @Override
        public double[] readDoubles(String fieldName) throws IOException {
            return (double[]) data.get(fieldName);
        }

        @Override
        public Boolean readBoolean(String fieldName) throws IOException {
            return (Boolean) data.get(fieldName);
        }

        @Override
        public boolean[] readBooleans(String fieldName) throws IOException {
            return (boolean[]) data.get(fieldName);
        }

        @Override
        public String readString(String fieldName) throws IOException {
            return (String) data.get(fieldName);
        }

        @Override
        public byte[] readBytes(String fieldName) throws IOException {
            return (byte[]) data.get(fieldName);
        }

        @Override
        public InputStream readBytesAsInputStream(String fieldName) throws IOException {
            return (InputStream) data.get(fieldName);
        }

        @Override
        public <E extends Enum<E>> E readEnum(String fieldName, Class<E> clazz) throws IOException {
            return (E) data.get(fieldName);
        }

        @Override
        public <E> E readObject(String fieldName, Class<E> clazz) throws IOException {
            return (E) data.get(fieldName);
        }

        @Override
        public <E, C extends Collection<? super E>> C readCollection(String fieldName, C collection, Class<E> elementClass) throws IOException {
            return (C) data.get(fieldName);
        }

        @Override
        public <E> E[] readArray(String fieldName, Class<? extends E> elementClass) throws IOException {
            return (E[]) data.get(fieldName);
        }

    }


}
