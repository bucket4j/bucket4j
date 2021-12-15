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
package io.github.bucket4j.grid.hazelcast;

import com.hazelcast.map.EntryProcessor;
import io.github.bucket4j.util.ComparableByContent;

import java.util.Arrays;
import java.util.Map;

public class SimpleBackupProcessor<K> implements EntryProcessor<K, byte[], byte[]>, ComparableByContent<SimpleBackupProcessor> {

    private static final long serialVersionUID = 1L;

    private final byte[] state;

    public SimpleBackupProcessor(byte[] state) {
        this.state = state;
    }

    public byte[] getState() {
        return state;
    }

    @Override
    public boolean equalsByContent(SimpleBackupProcessor other) {
        return Arrays.equals(state, other.state);
    }

    @Override
    public byte[] process(Map.Entry<K, byte[]> entry) {
        entry.setValue(state);
        return null; // return value from backup processor is ignored, see https://github.com/hazelcast/hazelcast/pull/14995
    }

}
