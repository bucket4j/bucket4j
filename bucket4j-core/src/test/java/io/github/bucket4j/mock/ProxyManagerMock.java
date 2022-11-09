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
import io.github.bucket4j.distributed.serialization.DataOutputSerializationAdapter;
import io.github.bucket4j.distributed.serialization.Scope;
import io.github.bucket4j.distributed.serialization.SerializationHandle;
import io.github.bucket4j.distributed.serialization.SerializationHandles;
import io.github.bucket4j.distributed.versioning.Version;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class ProxyManagerMock<K> extends AbstractProxyManager<K> {

    private static Map<Class, SerializationHandle> allHandles = new HashMap<Class, SerializationHandle>()
    {{
        for (SerializationHandle<?> handle : SerializationHandles.CORE_HANDLES.getAllHandles()) {
            put(handle.getSerializedType(), handle);
        }
    }};

    private Map<K, RemoteBucketState> stateMap = new HashMap<>();
    private RuntimeException exception;

    public ProxyManagerMock(TimeMeter timeMeter) {
        super(ClientSideConfig.getDefault().withClientClock(timeMeter));
    }

    public ProxyManagerMock(TimeMeter timeMeter, Version backwardCompatibilityVersion) {
        super(ClientSideConfig.getDefault().withClientClock(timeMeter));
    }

    public void setException(RuntimeException exception) {
        this.exception = exception;
    }

    @Override
    synchronized public <T> CommandResult<T> execute(K key, Request<T> request) {
        if (exception != null) {
            throw new RuntimeException();
        }
        RemoteCommand<T> command = request.getCommand();
        command = emulateDataSerialization(command, request.getBackwardCompatibilityVersion(), Scope.REQUEST);

        MutableBucketEntry entry = new MutableBucketEntry() {
            @Override
            public boolean exists() {
                return stateMap.containsKey(key);
            }
            @Override
            public void set(RemoteBucketState state) {
                ProxyManagerMock.this.stateMap.put(key, emulateDataSerialization(state, request.getBackwardCompatibilityVersion(), Scope.PERSISTED_STATE));
            }
            @Override
            public RemoteBucketState get() {
                RemoteBucketState state = stateMap.get(key);
                Objects.requireNonNull(state);
                return emulateDataSerialization(state, request.getBackwardCompatibilityVersion(), Scope.PERSISTED_STATE);
            }
        };

        CommandResult<T> result = command.execute(entry, getClientSideTime());
        return emulateDataSerialization(result, request.getBackwardCompatibilityVersion(), Scope.RESPONSE);
    }

    @Override
    public void removeProxy(K key) {
        stateMap.remove(key);
    }

    @Override
    public boolean isAsyncModeSupported() {
        return true;
    }

    @Override
    synchronized public <T> CompletableFuture<CommandResult<T>> executeAsync(K key, Request<T> request) {
        if (exception != null) {
            CompletableFuture future = new CompletableFuture();
            future.completeExceptionally(new RuntimeException());
            return future;
        }
        return CompletableFuture.completedFuture(execute(key, request));
    }

    @Override
    protected CompletableFuture<Void> removeAsync(K key) {
        stateMap.remove(key);
        return CompletableFuture.completedFuture(null);
    }

    protected <T> T emulateDataSerialization(T object, Version version, Scope scope) {
        SerializationHandle serializationHandle = allHandles.get(object.getClass());
        if (serializationHandle == null) {
            throw new IllegalArgumentException("Serializer for class " + serializationHandle + " is not specified");
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            serializationHandle.serialize(DataOutputSerializationAdapter.INSTANCE, dos, object, version, scope);
            byte[] bytes = baos.toByteArray();

            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            DataInputStream dis = new DataInputStream(bais);
            return (T) serializationHandle.deserialize(DataOutputSerializationAdapter.INSTANCE, dis);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}
