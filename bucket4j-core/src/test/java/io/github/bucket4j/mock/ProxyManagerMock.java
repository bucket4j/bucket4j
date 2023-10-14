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


import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.distributed.proxy.AbstractProxyManager;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.remote.*;
import io.github.bucket4j.distributed.versioning.Version;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static io.github.bucket4j.distributed.serialization.InternalSerializationHelper.deserializeResult;
import static io.github.bucket4j.distributed.serialization.InternalSerializationHelper.serializeRequest;

public class ProxyManagerMock<K> extends AbstractProxyManager<K> {

    private Map<K, byte[]> stateMap = new HashMap<>();
    private RuntimeException exception;
    private int modificationCount = 0;
    private int readCount = 0;

    public ProxyManagerMock(TimeMeter timeMeter) {
        super(ClientSideConfig.getDefault().withClientClock(timeMeter));
    }

    public ProxyManagerMock(ClientSideConfig config) {
        super(config);
    }

    synchronized public void setException(RuntimeException exception) {
        this.exception = exception;
    }

    synchronized public int getModificationCount() {
        return modificationCount;
    }

    synchronized public int getReadCount() {
        return readCount;
    }

    @Override
    synchronized public <T> CommandResult<T> execute(K key, Request<T> request) {
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
            protected void setRawState(byte[] stateBytes) {
                modificationCount++;
                stateMap.put(key, stateBytes);
            }
            @Override
            public boolean exists() {
                return stateMap.containsKey(key);
            }
        };
        byte[] responseBytes = transaction.execute();
        Version backwardCompatibilityVersion = request.getBackwardCompatibilityVersion();
        return deserializeResult(responseBytes, backwardCompatibilityVersion);
    }

    @Override
    synchronized public void removeProxy(K key) {
        stateMap.remove(key);
    }

    @Override
    public boolean isAsyncModeSupported() {
        return true;
    }

    @Override
    synchronized public <T> CompletableFuture<CommandResult<T>> executeAsync(K key, Request<T> request) {
        if (exception != null) {
            CompletableFuture<CommandResult<T>> future = new CompletableFuture<>();
            future.completeExceptionally(new RuntimeException());
            return future;
        }
        return CompletableFuture.completedFuture(execute(key, request));
    }

    @Override
    synchronized protected CompletableFuture<Void> removeAsync(K key) {
        stateMap.remove(key);
        return CompletableFuture.completedFuture(null);
    }

}
