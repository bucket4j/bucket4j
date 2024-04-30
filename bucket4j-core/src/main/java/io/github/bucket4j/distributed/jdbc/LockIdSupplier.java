/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2024 Vladimir Bukhtoyarov
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
package io.github.bucket4j.distributed.jdbc;

/**
 * Used by some inheritors of {@link io.github.bucket4j.distributed.proxy.generic.pessimistic_locking.AbstractLockBasedProxyManager}
 * when it needs to calculate numeric value of locks.
 *
 * @param <K> type of key
 */
public interface LockIdSupplier<K> {

    LockIdSupplier<?> DEFAULT = (LockIdSupplier<Object>) key -> key instanceof Number number? number.longValue(): key.hashCode();

    /**
     * Returns the lock-id specified with the key.
     *
     * @param key the key of bucket
     *
     * @return id of lock specified with key
     */
    long toLockId(K key);

}
