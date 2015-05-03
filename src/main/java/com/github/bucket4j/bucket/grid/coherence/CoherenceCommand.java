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

package com.github.bucket4j.bucket.grid.coherence;

import com.github.bucket4j.bucket.grid.GridBucketState;
import com.github.bucket4j.bucket.grid.GridCommand;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.processor.AbstractProcessor;

import java.io.Serializable;

public class CoherenceCommand<T extends Serializable> extends AbstractProcessor {

    private final GridCommand<T> command;

    public CoherenceCommand(GridCommand<T> command) {
        this.command = command;
    }

    @Override
    public Object process(InvocableMap.Entry entry) {
        GridBucketState state = (GridBucketState) entry.getValue();
        T result = command.execute(state);
        if (command.isBucketStateModified()) {
            entry.setValue(state);
        }
        return result;
    }

}
