/*
 *
 * Copyright 2015-2018 Vladimir Bukhtoyarov
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

package io.github.bucket4j.mock;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.distributed.proxy.AbstractAsyncProxyManager;
import io.github.bucket4j.distributed.proxy.ProxyManagerConfig;
import io.github.bucket4j.distributed.remote.AbstractBinaryTransaction;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.remote.Request;
import io.github.bucket4j.distributed.versioning.Version;

import static io.github.bucket4j.distributed.serialization.InternalSerializationHelper.deserializeResult;
import static io.github.bucket4j.distributed.serialization.InternalSerializationHelper.serializeRequest;

public class AsyncProxyManagerMock<K> extends AbstractAsyncProxyManager<K> {

    private static final int HISTORY_SIZE = 1000;
    private List<Request<?>> history = new LinkedList<>();

    private Map<K, byte[]> stateMap = new HashMap<>();
    private RuntimeException exception;
    private int modificationCount = 0;
    private int readCount = 0;
    private ReentrantLock executionLock = new ReentrantLock();
    private Condition allowExecutionCondition = executionLock.newCondition();
    private Condition blockedRequestsCondition = executionLock.newCondition();
    private Condition allowResultReturningCondition = executionLock.newCondition();
    private int blockedRequests = 0;
    private boolean allowExecution = true;
    private boolean allowReturnResult = true;

    public AsyncProxyManagerMock(TimeMeter timeMeter) {
        super(ProxyManagerConfig.getDefault().withClientClock(timeMeter));
    }

    public AsyncProxyManagerMock(ProxyManagerConfig config) {
        super(config);
    }

    public void awaitBlockedRequests(int count) {
        executionLock.lock();
        try {
            while (blockedRequests != count) {
                try {
                    blockedRequestsCondition.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        } finally {
            executionLock.unlock();
        }
    }

    public void setException(RuntimeException exception) {
        executionLock.lock();
        try {
            this.exception = exception;
        } finally {
            executionLock.unlock();
        }
    }

    public int getModificationCount() {
        executionLock.lock();
        try {
            return modificationCount;
        } finally {
            executionLock.unlock();
        }
    }

    public int getReadCount() {
        executionLock.lock();
        try {
            return readCount;
        } finally {
            executionLock.unlock();
        }
    }

    public void blockExecution() {
        executionLock.lock();
        try {
            allowExecution = false;
        } finally {
            executionLock.unlock();
        }
    }

    public void blockResultReturning() {
        executionLock.lock();
        try {
            allowReturnResult = false;
        } finally {
            executionLock.unlock();
        }
    }

    public void allowResultReturning() {
        executionLock.lock();
        try {
            allowReturnResult = true;
            allowResultReturningCondition.signalAll();
        } finally {
            executionLock.unlock();
        }
    }

    public void unblockExecution() {
        executionLock.lock();
        try {
            allowExecution = true;
            allowExecutionCondition.signalAll();
        } finally {
            executionLock.unlock();
        }
    }

    public List<Request<?>> getHistory() {
        executionLock.lock();
        try {
            return new ArrayList<>(history);
        } finally {
            executionLock.unlock();
        }
    }

    public void clearHistory() {
        executionLock.lock();
        try {
            history.clear();
        } finally {
            executionLock.unlock();
        }
    }

    private <T> CommandResult<T> execute(K key, Request<T> request) {
        executionLock.lock();
        try {
            history.add(request);
            if (history.size() > HISTORY_SIZE) {
                history.remove(0);
            }

            if (!allowExecution) {
                blockedRequests++;
                blockedRequestsCondition.signalAll();
                while (!allowExecution) {
                    try {
                        allowExecutionCondition.await();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                blockedRequests--;
                blockedRequestsCondition.signalAll();
            }

            if (exception != null) {
                throw new RuntimeException();
            }


            byte[] requestBytes = serializeRequest(request);
            AbstractBinaryTransaction transaction = new AbstractBinaryTransaction(requestBytes) {
                @Override
                protected byte[] getRawState() {
                    if (!stateMap.containsKey(key)) {
                        throw new IllegalStateException("Map has no key " + key);
                    }
                    readCount++;
                    return stateMap.get(key);
                }
                @Override
                protected void setRawState(byte[] newStateBytes, RemoteBucketState newState) {
                    modificationCount++;
                    stateMap.put(key, newStateBytes);
                }
                @Override
                public boolean exists() {
                    return stateMap.containsKey(key);
                }
            };
            byte[] responseBytes = transaction.execute();
            Version backwardCompatibilityVersion = request.getBackwardCompatibilityVersion();
            while (!allowReturnResult) {
                try {
                    allowResultReturningCondition.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            return deserializeResult(responseBytes, backwardCompatibilityVersion);
        } finally {
            executionLock.unlock();
        }
    }

    @Override
    public <T> CompletableFuture<CommandResult<T>> executeAsync(K key, Request<T> request) {
        executionLock.lock();
        try {
            if (exception != null) {
                CompletableFuture<CommandResult<T>> future = new CompletableFuture<>();
                future.completeExceptionally(new RuntimeException());
                return future;
            }
            return CompletableFuture.completedFuture(execute(key, request));
        } finally {
            executionLock.unlock();
        }
    }

    @Override
    protected CompletableFuture<Void> removeAsync(K key) {
        executionLock.lock();
        try {
            stateMap.remove(key);
            return CompletableFuture.completedFuture(null);
        } finally {
            executionLock.unlock();
        }
    }

    @Override
    public boolean isExpireAfterWriteSupported() {
        return false;
    }
}
