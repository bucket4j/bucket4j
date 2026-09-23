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
import java.util.ArrayList;
import java.util.List;

/**
 * Packs several serialized bucket4j requests/results coalesced by {@code BatchHelper} into one {@code byte[]},
 * so that a batch of requests for the same key can be sent to {@link io.github.bucket4j.grid.ignite3.Ignite3ComputeJob}
 * (and its reply decoded back) in a single compute call.
 */
public final class ByteArrayListCodec {

    private ByteArrayListCodec() {
    }

    public static byte[] encode(List<byte[]> items) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(items.size());
            for (byte[] item : items) {
                out.writeInt(item.length);
                out.write(item);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    public static List<byte[]> decode(byte[] bytes) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            int size = in.readInt();
            List<byte[]> items = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                int length = in.readInt();
                byte[] item = new byte[length];
                in.readFully(item);
                items.add(item);
            }
            return items;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
