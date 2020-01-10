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

import io.github.bucket4j.distributed.remote.RemoteCommand;

import java.util.ArrayList;
import java.util.List;

class TaskQueue {

    private List<WaitingTask> waitingCommands = new ArrayList<>();
    private Thread exclusiveOwner;

    public WaitingTask lockExclusivelyOrEnqueue(RemoteCommand<?> command) {
        Thread currentThread = Thread.currentThread();

        synchronized (this) {
            if (exclusiveOwner == null) {
                exclusiveOwner = currentThread;
                return null;
            } else {
                WaitingTask waitingNode = new WaitingTask(command);
                waitingCommands.add(waitingNode);
                return waitingNode;
            }
        }
    }

    public void wakeupFirstThreadFromNextBatch() {
        synchronized (this) {
            if (waitingCommands.isEmpty()) {
                exclusiveOwner = null;
            } else {
                WaitingTask firstWaitingNode = waitingCommands.get(0);
                exclusiveOwner = firstWaitingNode.waitingThread;
                firstWaitingNode.future.complete(WaitingTaskResult.needToBeExecutedInBatch());
            }
        }
    }

    public List<WaitingTask> takeAllWaitingTasks() {
        synchronized (this) {
            List<WaitingTask> waitingNodes = waitingCommands;
            waitingCommands = new ArrayList<>();
            return waitingNodes;
        }
    }

}
