/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2026 Vladimir Bukhtoyarov
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
package io.github.bucket4j.grid.ignite3.internal;

import io.github.bucket4j.distributed.serialization.ByteBufferSerializationAdapter;
import io.github.bucket4j.distributed.serialization.PrimitiveSizeCalculator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Encodes/decodes bucket keys into the binary envelope sent to {@link io.github.bucket4j.grid.ignite3.Ignite3ComputeJob}.
 *
 * <p>Only key types that Ignite 3 itself treats as native/simple column types are supported, because the same
 * key value is separately handed to {@code org.apache.ignite.table.mapper.Mapper#of(Class)} in order to route the
 * compute job to the node colocated with that key - so this is not an additional restriction imposed by bucket4j.
 *
 * <p>Follows the estimate-size-then-serialize {@link ByteBuffer} approach used by
 * {@code io.github.bucket4j.distributed.serialization.InternalSerializationHelper}: {@link #estimateSize(Object)}
 * must be called to size the buffer before {@link #encode(ByteBuffer, Object)} writes into it. A {@code String}
 * key that is too long to fit into the modified-UTF8 length prefix simply fails inside
 * {@link ByteBufferSerializationAdapter#writeString} with a {@code UTFDataFormatException}.
 */
public final class KeyCodec {

    private static final byte TAG_STRING = 1;
    private static final byte TAG_LONG = 2;
    private static final byte TAG_INTEGER = 3;
    private static final byte TAG_SHORT = 4;
    private static final byte TAG_BYTE = 5;
    private static final byte TAG_UUID = 6;

    private static final int SIZE_OF_SHORT = 2;

    private KeyCodec() {
    }

    public static boolean isSupported(Class<?> keyType) {
        return keyType == String.class
                || keyType == Long.class
                || keyType == Integer.class
                || keyType == Short.class
                || keyType == Byte.class
                || keyType == UUID.class;
    }

    public static int estimateSize(Object key) {
        if (key instanceof String s) {
            return PrimitiveSizeCalculator.SIZE_OF_BYTE + PrimitiveSizeCalculator.sizeOfString(s);
        } else if (key instanceof Long) {
            return PrimitiveSizeCalculator.SIZE_OF_BYTE + PrimitiveSizeCalculator.SIZE_OF_LONG;
        } else if (key instanceof Integer) {
            return PrimitiveSizeCalculator.SIZE_OF_BYTE + PrimitiveSizeCalculator.SIZE_OF_INT;
        } else if (key instanceof Short) {
            return PrimitiveSizeCalculator.SIZE_OF_BYTE + SIZE_OF_SHORT;
        } else if (key instanceof Byte) {
            return PrimitiveSizeCalculator.SIZE_OF_BYTE + PrimitiveSizeCalculator.SIZE_OF_BYTE;
        } else if (key instanceof UUID) {
            return PrimitiveSizeCalculator.SIZE_OF_BYTE + 2 * PrimitiveSizeCalculator.SIZE_OF_LONG;
        } else {
            throw new IllegalArgumentException("Key type " + key.getClass() + " is not supported by bucket4j-ignite3. " +
                    "Supported key types are: String, Long, Integer, Short, Byte, UUID");
        }
    }

    public static void encode(ByteBuffer out, Object key) {
        try {
            if (key instanceof String s) {
                out.put(TAG_STRING);
                ByteBufferSerializationAdapter.INSTANCE.writeString(out, s);
            } else if (key instanceof Long l) {
                out.put(TAG_LONG);
                out.putLong(l);
            } else if (key instanceof Integer i) {
                out.put(TAG_INTEGER);
                out.putInt(i);
            } else if (key instanceof Short s) {
                out.put(TAG_SHORT);
                out.putShort(s);
            } else if (key instanceof Byte b) {
                out.put(TAG_BYTE);
                out.put(b);
            } else if (key instanceof UUID uuid) {
                out.put(TAG_UUID);
                out.putLong(uuid.getMostSignificantBits());
                out.putLong(uuid.getLeastSignificantBits());
            } else {
                throw new IllegalArgumentException("Key type " + key.getClass() + " is not supported by bucket4j-ignite3. " +
                        "Supported key types are: String, Long, Integer, Short, Byte, UUID");
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <K> K decode(ByteBuffer in) {
        try {
            byte tag = in.get();
            Object key = switch (tag) {
                case TAG_STRING -> ByteBufferSerializationAdapter.INSTANCE.readString(in);
                case TAG_LONG -> in.getLong();
                case TAG_INTEGER -> in.getInt();
                case TAG_SHORT -> in.getShort();
                case TAG_BYTE -> in.get();
                case TAG_UUID -> new UUID(in.getLong(), in.getLong());
                default -> throw new IllegalStateException("Unknown key type tag " + tag);
            };
            return (K) key;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void requireSupported(Class<?> keyType) {
        if (!isSupported(keyType)) {
            throw new IllegalArgumentException("Key type " + keyType + " is not supported by bucket4j-ignite3. " +
                    "Supported key types are: String, Long, Integer, Short, Byte, UUID");
        }
    }

}
