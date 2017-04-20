
/*
 *
 *   Copyright 2015-2017 Vladimir Bukhtoyarov
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.github.bucket4j.grid.jcache;

import io.github.bucket4j.grid.CommandResult;
import io.github.bucket4j.grid.GridCommand;
import io.github.bucket4j.grid.GridProxy;
import io.github.bucket4j.grid.GridBucketState;

import javax.cache.Cache;
import java.io.Serializable;
import java.util.Objects;

public class JCacheProxy<K extends Serializable> implements GridProxy<K> {

    private final Cache<K, GridBucketState> cache;

    public JCacheProxy(Cache<K, GridBucketState> cache) {
        this.cache = Objects.requireNonNull(cache);
    }

    @Override
    public <T extends Serializable> CommandResult<T> execute(K key, GridCommand<T> command) {
        return cache.invoke(key, new JCacheCommand<>(), command);
    }

    @Override
    public void setInitialState(K key, GridBucketState initialState) {
        cache.putIfAbsent(key, initialState);
    }

}
