/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2022 Vladimir Bukhtoyarov
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
package io.github.bucket4j.distributed.proxy.generic.select_for_update;

public class LockAndGetResult {

    private static final LockAndGetResult NOT_LOCKED = new LockAndGetResult(null, false);

    private final byte[] data;
    private final boolean locked;

    private LockAndGetResult(byte[] data, boolean locked) {
        this.data = data;
        this.locked = locked;
    }

    public static LockAndGetResult notLocked() {
        return NOT_LOCKED;
    }

    public static LockAndGetResult locked(byte[] data) {
        return new LockAndGetResult(data, true);
    }

    public byte[] getData() {
        return data;
    }

    public boolean isLocked() {
        return locked;
    }

}
