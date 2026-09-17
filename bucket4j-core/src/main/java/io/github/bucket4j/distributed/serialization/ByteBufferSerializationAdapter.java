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

package io.github.bucket4j.distributed.serialization;

import java.io.IOException;
import java.io.UTFDataFormatException;
import java.nio.ByteBuffer;

/**
 * Reads/writes primitives directly against a {@link ByteBuffer}, instead of going through
 * {@code DataOutputStream}/{@code DataInputStream} backed by a growable {@code byte[]}.
 *
 * <p>This adapter is required to produce byte-for-byte identical output to
 * {@link DataOutputSerializationAdapter} for the same input, so that data written by one adapter
 * can always be read back by the other, and so that switching {@link SerializationStyle} never
 * changes the wire/persisted format - only how fast it is produced/consumed.
 *
 * <p>Both {@code ByteBuffer} and {@code DataOutput}/{@code DataInput} use big-endian(network) byte
 * order for multi-byte primitives, so {@code putInt}/{@code putLong}/{@code putDouble} already match.
 * The one primitive that has no direct {@code ByteBuffer} counterpart is {@code writeUTF}'s "modified
 * UTF-8" string encoding, which is re-implemented here to match {@code java.io.DataOutputStream#writeUTF}
 * exactly.
 */
public class ByteBufferSerializationAdapter implements SerializationAdapter<ByteBuffer>, DeserializationAdapter<ByteBuffer> {

    public static final ByteBufferSerializationAdapter INSTANCE = new ByteBufferSerializationAdapter();

    private ByteBufferSerializationAdapter() {
    }

    @Override
    public boolean readBoolean(ByteBuffer source) {
        return source.get() != 0;
    }

    @Override
    public byte readByte(ByteBuffer source) {
        return source.get();
    }

    @Override
    public int readInt(ByteBuffer source) {
        return source.getInt();
    }

    @Override
    public long readLong(ByteBuffer source) {
        return source.getLong();
    }

    @Override
    public long[] readLongArray(ByteBuffer source) {
        int size = source.getInt();
        long[] array = new long[size];
        for (int i = 0; i < size; i++) {
            array[i] = source.getLong();
        }
        return array;
    }

    @Override
    public double[] readDoubleArray(ByteBuffer source) {
        int size = source.getInt();
        double[] array = new double[size];
        for (int i = 0; i < size; i++) {
            array[i] = source.getDouble();
        }
        return array;
    }

    @Override
    public String readString(ByteBuffer source) throws IOException {
        int utfLength = source.getShort() & 0xFFFF;
        byte[] bytes = new byte[utfLength];
        source.get(bytes);
        return decodeModifiedUtf8(bytes);
    }

    @Override
    public void writeBoolean(ByteBuffer target, boolean value) {
        target.put((byte) (value ? 1 : 0));
    }

    @Override
    public void writeByte(ByteBuffer target, byte value) {
        target.put(value);
    }

    @Override
    public void writeInt(ByteBuffer target, int value) {
        target.putInt(value);
    }

    @Override
    public void writeLong(ByteBuffer target, long value) {
        target.putLong(value);
    }

    @Override
    public void writeLongArray(ByteBuffer target, long[] value) {
        target.putInt(value.length);
        for (long v : value) {
            target.putLong(v);
        }
    }

    @Override
    public void writeDoubleArray(ByteBuffer target, double[] value) {
        target.putInt(value.length);
        for (double v : value) {
            target.putDouble(v);
        }
    }

    @Override
    public void writeString(ByteBuffer target, String value) throws IOException {
        int utfLength = PrimitiveSizeCalculator.modifiedUtf8Length(value);
        if (utfLength > 65535) {
            throw new UTFDataFormatException("encoded string too long: " + utfLength + " bytes");
        }
        target.putShort((short) utfLength);
        encodeModifiedUtf8(value, utfLength, target);
    }

    /**
     * Mirrors {@code java.io.DataOutputStream#writeUTF(String, DataOutput)} byte-for-byte.
     */
    private static void encodeModifiedUtf8(String str, int utfLength, ByteBuffer target) {
        int length = str.length();
        int i = 0;
        for (; i < length; i++) {
            char c = str.charAt(i);
            if (c < 0x0001 || c > 0x007F) {
                break;
            }
            target.put((byte) c);
        }
        for (; i < length; i++) {
            char c = str.charAt(i);
            if (c >= 0x0001 && c <= 0x007F) {
                target.put((byte) c);
            } else if (c > 0x07FF) {
                target.put((byte) (0xE0 | ((c >> 12) & 0x0F)));
                target.put((byte) (0x80 | ((c >> 6) & 0x3F)));
                target.put((byte) (0x80 | (c & 0x3F)));
            } else {
                target.put((byte) (0xC0 | ((c >> 6) & 0x1F)));
                target.put((byte) (0x80 | (c & 0x3F)));
            }
        }
    }

    /**
     * Mirrors {@code java.io.DataInputStream#readUTF(DataInput)} byte-for-byte.
     */
    private static String decodeModifiedUtf8(byte[] bytes) throws UTFDataFormatException {
        int utfLength = bytes.length;
        char[] chars = new char[utfLength];
        int count = 0;
        int charsCount = 0;

        while (count < utfLength) {
            int c = bytes[count] & 0xff;
            if (c > 127) {
                break;
            }
            count++;
            chars[charsCount++] = (char) c;
        }

        while (count < utfLength) {
            int c = bytes[count] & 0xff;
            switch (c >> 4) {
                case 0, 1, 2, 3, 4, 5, 6, 7 -> {
                    count++;
                    chars[charsCount++] = (char) c;
                }
                case 12, 13 -> {
                    count += 2;
                    if (count > utfLength) {
                        throw new UTFDataFormatException("malformed input: partial character at end");
                    }
                    int char2 = bytes[count - 1];
                    if ((char2 & 0xC0) != 0x80) {
                        throw new UTFDataFormatException("malformed input around byte " + count);
                    }
                    chars[charsCount++] = (char) (((c & 0x1F) << 6) | (char2 & 0x3F));
                }
                case 14 -> {
                    count += 3;
                    if (count > utfLength) {
                        throw new UTFDataFormatException("malformed input: partial character at end");
                    }
                    int char2 = bytes[count - 2];
                    int char3 = bytes[count - 1];
                    if (((char2 & 0xC0) != 0x80) || ((char3 & 0xC0) != 0x80)) {
                        throw new UTFDataFormatException("malformed input around byte " + (count - 1));
                    }
                    chars[charsCount++] = (char) (((c & 0x0F) << 12) | ((char2 & 0x3F) << 6) | (char3 & 0x3F));
                }
                default -> throw new UTFDataFormatException("malformed input around byte " + count);
            }
        }
        return new String(chars, 0, charsCount);
    }

}
