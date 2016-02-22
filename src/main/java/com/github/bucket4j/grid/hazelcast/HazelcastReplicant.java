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

import com.github.bucket4j.BucketState;
import com.github.bucket4j.grid.GridBucketState;
import com.hazelcast.map.EntryBackupProcessor;

import java.io.Serializable;
import java.util.Map;

public class HazelcastReplicant implements EntryBackupProcessor, Serializable {

    private BucketState snapshot;

    public HazelcastReplicant(BucketState snapshot) {
        this.snapshot = snapshot;
    }

    public HazelcastReplicant() {}

    @Override
    public void processBackup(Map.Entry entry) {
        GridBucketState gridState = (GridBucketState) entry.getValue();
        gridState.getBucketState().copyStateFrom(snapshot);
        entry.setValue(gridState);
    }

}
