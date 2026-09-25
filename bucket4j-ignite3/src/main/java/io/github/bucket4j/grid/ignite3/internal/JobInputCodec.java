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

/**
 * Ignite 3's {@code ComputeJob} accepts a single argument, but bucket4j-ignite3 needs to ship both the routing
 * key and the serialized bucket4j request to the job body. This codec packs {@code (tableName, key, requestBytes)}
 * into one {@code byte[]} envelope that becomes the job argument; the key itself is separately handed to
 * {@code JobTarget.colocated} for node routing.
 *
 * <p>Follows the estimate-size-then-serialize {@link ByteBuffer} approach used by
 * {@code io.github.bucket4j.distributed.serialization.InternalSerializationHelper}: the exact envelope size is
 * computed upfront so that exactly one right-sized {@link ByteBuffer} is allocated.
 */
public final class JobInputCodec {

    private JobInputCodec() {
    }

    public record JobInput<K>(String tableName, K key, byte[] requestBytes) {
    }

    public static <K> byte[] encode(String tableName, K key, byte[] requestBytes) {
        int size = PrimitiveSizeCalculator.sizeOfString(tableName)
                + KeyCodec.estimateSize(key)
                + PrimitiveSizeCalculator.SIZE_OF_INT
                + requestBytes.length;
        ByteBuffer out = ByteBuffer.allocate(size);
        try {
            ByteBufferSerializationAdapter.INSTANCE.writeString(out, tableName);
            KeyCodec.encode(out, key);
            out.putInt(requestBytes.length);
            out.put(requestBytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.array();
    }

    public static <K> JobInput<K> decode(byte[] bytes) {
        ByteBuffer in = ByteBuffer.wrap(bytes);
        try {
            String tableName = ByteBufferSerializationAdapter.INSTANCE.readString(in);
            K key = KeyCodec.decode(in);
            int length = in.getInt();
            byte[] requestBytes = new byte[length];
            in.get(requestBytes);
            return new JobInput<>(tableName, key, requestBytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
