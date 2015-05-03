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

package com.github.bucket4j.grid.hazelcast;

import com.github.bucket4j.grid.GridBucketState;
import com.github.bucket4j.grid.GridCommand;
import com.github.bucket4j.grid.GridProxy;
import com.hazelcast.core.IMap;

import java.io.Serializable;

public class HazelcastProxy implements GridProxy {

    private final IMap<Object, GridBucketState> map;
    private final Serializable key;

    public HazelcastProxy(IMap<Object, GridBucketState> map, Serializable key) {
        this.map = map;
        this.key = key;
    }

    @Override
    public <T extends Serializable> T execute(GridCommand<T> command) {
        HazelcastCommand entryProcessor = new HazelcastCommand(command);
        return (T) map.executeOnKey(key, entryProcessor);
    }

    @Override
    public void setInitialState(GridBucketState initialState) {
        map.putIfAbsent(key, initialState);
    }

}
