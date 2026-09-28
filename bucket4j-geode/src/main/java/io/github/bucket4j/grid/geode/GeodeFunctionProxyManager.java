/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2026 Vladimir Bukhtoyarov
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package io.github.bucket4j.grid.geode;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.apache.geode.cache.Region;
import org.apache.geode.cache.execute.Execution;
import org.apache.geode.cache.execute.FunctionService;
import org.apache.geode.cache.execute.ResultCollector;

import io.github.bucket4j.distributed.proxy.AbstractProxyManager;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.Request;
import io.github.bucket4j.distributed.serialization.SerializationStyle;

import static io.github.bucket4j.distributed.serialization.InternalSerializationHelper.deserializeResult;
import static io.github.bucket4j.distributed.serialization.InternalSerializationHelper.serializeRequest;

/**
 * Dispatches every request to {@link GeodeBucketFunction}, colocating the whole compare-and-swap retry
 * loop with the data on the member that owns the key's bucket, instead of retrying from the client the
 * way {@link GeodeProxyManager} does.
 *
 * <p>This trades a deployment cost - {@code GeodeBucketFunction} (and therefore the Bucket4j jar) must be
 * present on the classpath of every Geode server that can own a bucket key, the same cost
 * {@code bucket4j-ignite}'s thick-client integration and {@code bucket4j-coherence} already require - for
 * a runtime benefit: a losing racer's retry never leaves the server, so contention on a hot key produces
 * one client-server round-trip instead of one round-trip per attempt. Client code that cannot add Bucket4j
 * to the server classpath should use {@link GeodeProxyManager} instead.
 *
 * @param <K> type of the key
 */
public class GeodeFunctionProxyManager<K> extends AbstractProxyManager<K> {

    private final Region<K, byte[]> region;

    GeodeFunctionProxyManager(Bucket4jGeode.GeodeFunctionProxyManagerBuilder<K> builder) {
        super(builder.getClientSideConfig());
        this.region = builder.region;
    }

    @Override
    protected <T> CommandResult<T> execute(K key, Request<T> request) {
        byte[] requestBytes = serializeRequest(request, SerializationStyle.BYTE_BUFFER);
        Execution<byte[], byte[], List<byte[]>> execution = FunctionService.<byte[], byte[], List<byte[]>>onRegion(region)
            .withFilter(Collections.singleton(key))
            .setArguments(requestBytes);
        ResultCollector<byte[], List<byte[]>> resultCollector = execution.execute(new GeodeBucketFunction<K>());
        byte[] resultBytes = resultCollector.getResult().get(0);
        return deserializeResult(resultBytes, request.getBackwardCompatibilityVersion(), SerializationStyle.BYTE_BUFFER);
    }

    @Override
    protected <T> CompletableFuture<CommandResult<T>> executeAsync(K key, Request<T> request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAsyncModeSupported() {
        return false;
    }

    @Override
    public void removeProxy(K key) {
        region.remove(key);
    }

    @Override
    protected CompletableFuture<Void> removeAsync(K key) {
        throw new UnsupportedOperationException();
    }

}
