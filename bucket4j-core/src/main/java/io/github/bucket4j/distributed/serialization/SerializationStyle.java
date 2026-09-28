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
 * Selects which {@link SerializationAdapter}/{@link DeserializationAdapter} implementation is used to turn
 * Bucket4j objects into {@code byte[]} and back. Both styles produce/consume byte-for-byte identical binary
 * output, so switching the style never affects wire or persisted-state compatibility - it is purely a
 * performance choice.
 */
public enum SerializationStyle {

    /**
     * Legacy behavior: serializes through {@code ByteArrayOutputStream}/{@code DataOutputStream} and
     * deserializes through {@code ByteArrayInputStream}/{@code DataInputStream}.
     */
    DATA_OUTPUT,

    /**
     * Serializes in two phases: first {@link SerializationHandle#estimateSize} calculates the exact
     * number of bytes required, then a single right-sized {@link java.nio.ByteBuffer} is allocated and
     * filled - avoiding the intermediate array growth/copying performed by {@code ByteArrayOutputStream}.
     */
    BYTE_BUFFER

}
