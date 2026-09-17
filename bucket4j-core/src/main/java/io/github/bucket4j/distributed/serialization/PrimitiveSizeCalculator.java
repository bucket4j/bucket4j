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

/**
 * Provides the exact binary size (in bytes) that {@link SerializationAdapter}/{@link DeserializationAdapter}
 * implementations spend on each primitive, so that {@link SerializationHandle#estimateSize} implementations
 * can pre-calculate the total buffer size required for a two-phase(estimate-then-serialize) serialization,
 * without allocating any intermediate buffers.
 *
 * <p>The byte layout mirrors {@link java.io.DataOutput}/{@link java.io.DataInput}, because
 * {@link DataOutputSerializationAdapter} and {@link ByteBufferSerializationAdapter} are required to
 * produce byte-identical output for the same input.
 */
public final class PrimitiveSizeCalculator {

    public static final int SIZE_OF_BOOLEAN = 1;
    public static final int SIZE_OF_BYTE = 1;
    public static final int SIZE_OF_INT = 4;
    public static final int SIZE_OF_LONG = 8;
    public static final int SIZE_OF_DOUBLE = 8;

    private PrimitiveSizeCalculator() {
    }

    public static int sizeOfLongArray(long[] array) {
        return SIZE_OF_INT + array.length * SIZE_OF_LONG;
    }

    public static int sizeOfDoubleArray(double[] array) {
        return SIZE_OF_INT + array.length * SIZE_OF_DOUBLE;
    }

    /**
     * @return the number of bytes that {@link java.io.DataOutput#writeUTF(String)}(and consequently
     * {@link ByteBufferSerializationAdapter#writeString}) spends to encode {@code value}, including
     * the leading 2-byte length prefix.
     */
    public static int sizeOfString(String value) {
        return SIZE_OF_SHORT + modifiedUtf8Length(value);
    }

    private static final int SIZE_OF_SHORT = 2;

    /**
     * Re-implementation of the length-counting loop from {@code java.io.DataOutputStream#writeUTF},
     * kept in sync intentionally so that {@link #sizeOfString} matches the real encoded length.
     */
    static int modifiedUtf8Length(String str) {
        int length = str.length();
        int utfLength = 0;
        for (int i = 0; i < length; i++) {
            char c = str.charAt(i);
            if (c >= 0x0001 && c <= 0x007F) {
                utfLength++;
            } else if (c > 0x07FF) {
                utfLength += 3;
            } else {
                utfLength += 2;
            }
        }
        return utfLength;
    }

}
