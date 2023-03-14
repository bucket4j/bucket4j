/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2023 Vladimir Bukhtoyarov
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

import java.nio.charset.StandardCharsets;
import java.util.Base64;

// TODO write tests
public interface Mapper<T> {

    byte[] toBytes(T value);

    String toString(T value);

    Mapper<Long> LONG = new Mapper<Long>() {
        @Override
        public byte[] toBytes(Long value) {
            byte[] result = new byte[8];
            for (int i = 7; i >= 0; i--) {
                result[i] = (byte)(value & 0xFF);
                value >>= 8;
            }
            return result;
        }

        @Override
        public String toString(Long value) {
            return value.toString();
        }
    };

    Mapper<Integer> INT = new Mapper<Integer>() {
        @Override
        public byte[] toBytes(Integer value) {
            byte[] result = new byte[4];
            for (int i = 3; i >= 0; i--) {
                result[i] = (byte)(value & 0xFF);
                value >>= 8;
            }
            return result;
        }

        @Override
        public String toString(Integer value) {
            return value.toString();
        }
    };

    Mapper<String> STRING = new Mapper<String>() {
        @Override
        public byte[] toBytes(String value) {
            return value.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public String toString(String value) {
            return value;
        }
    };

    Mapper<byte[]> BYTES = new Mapper<byte[]>() {
        @Override
        public byte[] toBytes(byte[] value) {
            return value;
        }

        @Override
        public String toString(byte[] value) {
            return Base64.getEncoder().encodeToString(value);
        }
    };

}
