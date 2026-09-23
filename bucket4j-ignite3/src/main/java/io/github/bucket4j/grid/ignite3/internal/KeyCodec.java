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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

/**
 * Encodes/decodes bucket keys into the binary envelope sent to {@link io.github.bucket4j.grid.ignite3.Ignite3ComputeJob}.
 *
 * <p>Only key types that Ignite 3 itself treats as native/simple column types are supported, because the same
 * key value is separately handed to {@code org.apache.ignite.table.mapper.Mapper#of(Class)} in order to route the
 * compute job to the node colocated with that key - so this is not an additional restriction imposed by bucket4j.
 */
public final class KeyCodec {

    private static final byte TAG_STRING = 1;
    private static final byte TAG_LONG = 2;
    private static final byte TAG_INTEGER = 3;
    private static final byte TAG_SHORT = 4;
    private static final byte TAG_BYTE = 5;
    private static final byte TAG_UUID = 6;

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

    public static void encode(DataOutputStream out, Object key) {
        try {
            if (key instanceof String s) {
                out.writeByte(TAG_STRING);
                out.writeUTF(s);
            } else if (key instanceof Long l) {
                out.writeByte(TAG_LONG);
                out.writeLong(l);
            } else if (key instanceof Integer i) {
                out.writeByte(TAG_INTEGER);
                out.writeInt(i);
            } else if (key instanceof Short s) {
                out.writeByte(TAG_SHORT);
                out.writeShort(s);
            } else if (key instanceof Byte b) {
                out.writeByte(TAG_BYTE);
                out.writeByte(b);
            } else if (key instanceof UUID uuid) {
                out.writeByte(TAG_UUID);
                out.writeLong(uuid.getMostSignificantBits());
                out.writeLong(uuid.getLeastSignificantBits());
            } else {
                throw new IllegalArgumentException("Key type " + key.getClass() + " is not supported by bucket4j-ignite3. " +
                        "Supported key types are: String, Long, Integer, Short, Byte, UUID");
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <K> K decode(DataInputStream in) {
        try {
            byte tag = in.readByte();
            Object key = switch (tag) {
                case TAG_STRING -> in.readUTF();
                case TAG_LONG -> in.readLong();
                case TAG_INTEGER -> in.readInt();
                case TAG_SHORT -> in.readShort();
                case TAG_BYTE -> in.readByte();
                case TAG_UUID -> new UUID(in.readLong(), in.readLong());
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
