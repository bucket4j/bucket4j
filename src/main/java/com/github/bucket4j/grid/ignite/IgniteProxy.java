/*
 * Copyright 2015 Vladimir Bukhtoyarov
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.github.bucket4j.grid.ignite;

import com.github.bucket4j.grid.GridBucketState;
import com.github.bucket4j.grid.GridCommand;
import com.github.bucket4j.grid.GridProxy;
import org.apache.ignite.IgniteCache;

import java.io.Serializable;

public class IgniteProxy implements GridProxy {

    private final IgniteCache<Object, GridBucketState> cache;
    private final Object key;

    public IgniteProxy(IgniteCache<Object, GridBucketState> cache, Object key) {
        this.cache = cache;
        this.key = key;
    }

    @Override
    public <T extends Serializable> T execute(GridCommand<T> command) {
        return cache.invoke(key, new IgniteCommand<T>(), command);
    }

    @Override
    public void setInitialState(GridBucketState initialState) {
        cache.putIfAbsent(key, initialState);
    }

}
