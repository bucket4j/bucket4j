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

package com.github.bucket4j.grid.coherence;

import com.github.bucket4j.grid.GridBucketState;
import com.github.bucket4j.grid.GridCommand;
import com.github.bucket4j.grid.GridProxy;
import com.tangosol.net.NamedCache;
import com.tangosol.util.filter.NotFilter;
import com.tangosol.util.filter.PresentFilter;
import com.tangosol.util.processor.ConditionalPut;

import java.io.Serializable;

public class CoherenceProxy implements GridProxy {

    private final NamedCache cache;
    private final Object key;

    public CoherenceProxy(NamedCache cache, Object key) {
        this.cache = cache;
        this.key = key;
    }

    @Override
    public <T extends Serializable> T execute(GridCommand<T> command) {
        CoherenceCommand<T> entryProcessor = new CoherenceCommand<>(command);
        return (T) cache.invoke(key, entryProcessor);
    }

    @Override
    public void setInitialState(GridBucketState initialState) {
        NotFilter filter = new NotFilter(PresentFilter.INSTANCE);
        cache.invoke(key, new ConditionalPut(filter, initialState));
    }

}
