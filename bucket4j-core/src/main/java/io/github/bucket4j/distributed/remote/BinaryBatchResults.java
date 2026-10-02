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
package io.github.bucket4j.distributed.remote;

import java.util.List;

public class BinaryBatchResults {

    public final boolean stateModified;
    public final List<CommandResult<?>> results;
    public final byte[] newStateBytes;
    public final Long ttlMillis;

    public BinaryBatchResults(Long ttlMillis, boolean stateModified, List<CommandResult<?>> results, byte[] newStateBytes) {
        this.ttlMillis = ttlMillis;
        this.stateModified = stateModified;
        this.results = results;
        this.newStateBytes = newStateBytes;
    }

}
