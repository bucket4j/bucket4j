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

import io.github.bucket4j.distributed.remote.RemoteCommand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import java.util.concurrent.atomic.AtomicReference;

public class TaskQueue {

    private static final WaitingTask QUEUE_EMPTY_BUT_EXECUTION_IN_PROGRESS = new WaitingTask(null);
    private static final WaitingTask QUEUE_EMPTY = new WaitingTask(null);

    private final AtomicReference<WaitingTask> headReference = new AtomicReference<>(QUEUE_EMPTY);

    public WaitingTask lockExclusivelyOrEnqueue(RemoteCommand<?> command) {
        WaitingTask waitingTask = new WaitingTask(command);

        while (true) {
            WaitingTask previous = headReference.get();
            if (previous == QUEUE_EMPTY) {
                if (headReference.compareAndSet(previous, QUEUE_EMPTY_BUT_EXECUTION_IN_PROGRESS)) {
                    return null;
                } else {
                    continue;
                }
            }

            waitingTask.previous = previous;
            if (headReference.compareAndSet(previous, waitingTask)) {
                return waitingTask;
            } else {
                waitingTask.previous = null;
            }
        }
    }

    public void wakeupAnyThreadFromNextBatchOrFreeLock() {
        while (true) {
            WaitingTask previous = headReference.get();
            if (previous == QUEUE_EMPTY_BUT_EXECUTION_IN_PROGRESS) {
                if (headReference.compareAndSet(QUEUE_EMPTY_BUT_EXECUTION_IN_PROGRESS, QUEUE_EMPTY)) {
                    return;
                } else {
                    continue;
                }
            } else if (previous != QUEUE_EMPTY) {
                previous.future.complete(WaitingTask.NEED_TO_EXECUTE_NEXT_BATCH);
                return;
            } else {
                // should never come there
                String msg = "Detected illegal usage of API, wakeupAnyThreadFromNextBatchOrFreeLock should not be called on empty queue";
                throw new IllegalStateException(msg);
            }
        }
    }

    public List<WaitingTask> takeAllWaitingTasksOrFreeLock() {
        WaitingTask head;
        while (true) {
            head = headReference.get();
            if (head == QUEUE_EMPTY_BUT_EXECUTION_IN_PROGRESS) {
                if (headReference.compareAndSet(QUEUE_EMPTY_BUT_EXECUTION_IN_PROGRESS, QUEUE_EMPTY)) {
                    return Collections.emptyList();
                } else {
                    continue;
                }
            }

            if (headReference.compareAndSet(head, QUEUE_EMPTY_BUT_EXECUTION_IN_PROGRESS)) {
                break;
            }
        }

        WaitingTask current = head;
        List<WaitingTask> waitingNodes = new ArrayList<>();
        while (current != QUEUE_EMPTY_BUT_EXECUTION_IN_PROGRESS) {
            waitingNodes.add(current);
            WaitingTask tmp = current.previous;
            current.previous = null; // nullify the reference to previous node in order to avoid GC nepotism
            current = tmp;
        }
        Collections.reverse(waitingNodes);
        return waitingNodes;
    }

}