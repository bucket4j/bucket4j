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

package com.github.bucket4j.mock;


import com.github.bucket4j.grid.CommandResult;
import com.github.bucket4j.grid.GridBucketState;
import com.github.bucket4j.grid.GridCommand;
import com.github.bucket4j.grid.GridProxy;

import java.io.Serializable;
import java.util.concurrent.ThreadLocalRandom;

public class GridProxyMock implements GridProxy {

    private GridBucketState state;
    private Serializable key = ThreadLocalRandom.current().nextInt(100);
    private CommandResult predefinedAnswer;

    @Override
    public CommandResult execute(GridCommand command) {
        if (predefinedAnswer != null) {
            return predefinedAnswer;
        }
        Serializable resultData = command.execute(state);
        return CommandResult.success(resultData);
    }

    @Override
    public void setInitialState(GridBucketState initialState) {
        this.state = initialState;
    }

    @Override
    public Serializable getBucketKey() {
        return key;
    }

    public void setPredefinedAnswer(CommandResult predefinedAnswer) {
        this.predefinedAnswer = predefinedAnswer;
    }

}
