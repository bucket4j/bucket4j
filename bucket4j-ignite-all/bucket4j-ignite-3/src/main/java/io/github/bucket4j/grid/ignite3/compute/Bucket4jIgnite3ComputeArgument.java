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
package io.github.bucket4j.grid.ignite3.compute;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Packs/unpacks the argument passed to {@link Bucket4jIgnite3ComputeJob}.
 *
 * <p>
 * {@link org.apache.ignite.compute.ComputeJob} arguments are exchanged as plain {@code byte[]},
 * so the table/column names, the bucket key and the serialized Bucket4j request are packed
 * together into a single buffer that is unpacked again on the server side.
 */
class Bucket4jIgnite3ComputeArgument {

    final String tableName;
    final String idColumnName;
    final String stateColumnName;
    final String key;
    final byte[] requestBytes;

    Bucket4jIgnite3ComputeArgument(String tableName, String idColumnName, String stateColumnName, String key, byte[] requestBytes) {
        this.tableName = tableName;
        this.idColumnName = idColumnName;
        this.stateColumnName = stateColumnName;
        this.key = key;
        this.requestBytes = requestBytes;
    }

    byte[] encode() {
        byte[] tableNameBytes = tableName.getBytes(StandardCharsets.UTF_8);
        byte[] idColumnBytes = idColumnName.getBytes(StandardCharsets.UTF_8);
        byte[] stateColumnBytes = stateColumnName.getBytes(StandardCharsets.UTF_8);
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(
            4 + tableNameBytes.length +
            4 + idColumnBytes.length +
            4 + stateColumnBytes.length +
            4 + keyBytes.length +
            requestBytes.length
        );
        putChunk(buffer, tableNameBytes);
        putChunk(buffer, idColumnBytes);
        putChunk(buffer, stateColumnBytes);
        putChunk(buffer, keyBytes);
        buffer.put(requestBytes);
        return buffer.array();
    }

    static Bucket4jIgnite3ComputeArgument decode(byte[] argument) {
        ByteBuffer buffer = ByteBuffer.wrap(argument);
        String tableName = getChunkAsString(buffer);
        String idColumnName = getChunkAsString(buffer);
        String stateColumnName = getChunkAsString(buffer);
        String key = getChunkAsString(buffer);
        byte[] requestBytes = new byte[buffer.remaining()];
        buffer.get(requestBytes);
        return new Bucket4jIgnite3ComputeArgument(tableName, idColumnName, stateColumnName, key, requestBytes);
    }

    private static void putChunk(ByteBuffer buffer, byte[] chunk) {
        buffer.putInt(chunk.length);
        buffer.put(chunk);
    }

    private static String getChunkAsString(ByteBuffer buffer) {
        int length = buffer.getInt();
        byte[] chunk = new byte[length];
        buffer.get(chunk);
        return new String(chunk, StandardCharsets.UTF_8);
    }

}
