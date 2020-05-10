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

package io.github.bucket4j.grid.coherence.pof;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofSerializer;
import com.tangosol.io.pof.PofWriter;
import io.github.bucket4j.serialization.DataStreamAdapter;
import io.github.bucket4j.serialization.SerializationHandle;

import java.io.*;

public class AbstractBucket4jPofSerializer<T> implements PofSerializer<T> {

    private static DataStreamAdapter ADAPTER = new DataStreamAdapter();

    private final SerializationHandle<T> serializationHandle;

    public AbstractBucket4jPofSerializer(SerializationHandle<T> serializationHandle) {
        this.serializationHandle = serializationHandle;
    }

    @Override
    public void serialize(PofWriter pofWriter, T object) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(byteStream);

        serializationHandle.serialize(ADAPTER, output, object);

        output.close();
        byteStream.close();
        byte[] bytes = byteStream.toByteArray();
        pofWriter.writeByteArray(0, bytes);
        pofWriter.writeRemainder(null);
    }

    @Override
    public T deserialize(PofReader pofReader) throws IOException {
        byte[] bytes = pofReader.readByteArray(0);

        try (DataInputStream inputSteam = new DataInputStream(new ByteArrayInputStream(bytes))) {
            T deserializeObject = serializationHandle.deserialize(ADAPTER, inputSteam);
            pofReader.readRemainder();
            return deserializeObject;
        }
    }

}
