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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Ignite 3's {@code ComputeJob} accepts a single argument, but bucket4j-ignite3 needs to ship both the routing
 * key and the serialized bucket4j request to the job body. This codec packs {@code (tableName, key, requestBytes)}
 * into one {@code byte[]} envelope that becomes the job argument; the key itself is separately handed to
 * {@code JobTarget.colocated} for node routing.
 */
public final class JobInputCodec {

    private JobInputCodec() {
    }

    public record JobInput<K>(String tableName, K key, byte[] requestBytes) {
    }

    public static <K> byte[] encode(String tableName, K key, byte[] requestBytes) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(requestBytes.length + 64);
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeUTF(tableName);
            KeyCodec.encode(out, key);
            out.writeInt(requestBytes.length);
            out.write(requestBytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    public static <K> JobInput<K> decode(byte[] bytes) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            String tableName = in.readUTF();
            K key = KeyCodec.decode(in);
            int length = in.readInt();
            byte[] requestBytes = new byte[length];
            in.readFully(requestBytes);
            return new JobInput<>(tableName, key, requestBytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
