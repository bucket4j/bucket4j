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

package io.github.bucket4j.distributed.proxy.optimizers.batch;

import io.github.bucket4j.distributed.proxy.optimizers.batch.sync.BatchFailedException;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.RemoteCommand;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static io.github.bucket4j.serialization.PrimitiveSerializationHandles.LONG_HANDLE;

public class WaitingTask {

    public static final CommandResult<?> NEED_TO_EXECUTE_NEXT_BATCH = CommandResult.success(42L, LONG_HANDLE);

    public final RemoteCommand<?> command;
    public final CompletableFuture<CommandResult<?>> future = new CompletableFuture<>();

    public WaitingTask previous;

    WaitingTask(RemoteCommand<?> command) {
        this.command = command;
    }

    public CommandResult<?> waitUninterruptedly() {
        boolean wasInterrupted = false;;
        try {
            while (true) {
                wasInterrupted = wasInterrupted || Thread.interrupted();
                try {
                    return future.get();
                } catch (InterruptedException e) {
                    wasInterrupted = true;
                } catch (ExecutionException e) {
                    throw new BatchFailedException(e.getCause());
                }
            }
        } finally {
            if (wasInterrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

}
