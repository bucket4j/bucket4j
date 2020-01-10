/*
 *
 * Copyright 2015-2020 Vladimir Bukhtoyarov
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package io.github.bucket4j.distributed.proxy.optimizers.batch.sync;

import io.github.bucket4j.distributed.remote.CommandResult;


class WaitingTaskResult {

    private static final WaitingTaskResult NEED_TO_EXECUTE_NEXT_BATCH = new WaitingTaskResult(null);

    private final CommandResult<?> result;

    private WaitingTaskResult(CommandResult<?> result) {
        this.result = result;
    }

    static WaitingTaskResult completed(CommandResult<?> result) {
        return new WaitingTaskResult(result);
    }

    static WaitingTaskResult needToBeExecutedInBatch() {
        return NEED_TO_EXECUTE_NEXT_BATCH;
    }

    public CommandResult<?> getResult() {
        return result;
    }

    public boolean isCompleted() {
        return result != null;
    }

    public boolean isCurrentThreadIsResponsibleForNextBatchExecution() {
        return this == NEED_TO_EXECUTE_NEXT_BATCH;
    }

}
