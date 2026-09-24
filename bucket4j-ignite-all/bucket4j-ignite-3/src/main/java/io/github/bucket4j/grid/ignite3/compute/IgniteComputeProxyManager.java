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
package io.github.bucket4j.grid.ignite3.compute;

import java.util.concurrent.CompletableFuture;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.compute.JobDescriptor;
import org.apache.ignite.compute.JobTarget;
import org.apache.ignite.table.KeyValueView;
import org.apache.ignite.table.Tuple;

import io.github.bucket4j.distributed.jdbc.BucketTableSettings;
import io.github.bucket4j.distributed.proxy.AbstractProxyManager;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.Request;
import io.github.bucket4j.distributed.serialization.SerializationStyle;
import io.github.bucket4j.distributed.versioning.Version;
import io.github.bucket4j.grid.ignite3.Bucket4jIgnite3.IgniteComputeProxyManagerBuilder;

import static io.github.bucket4j.distributed.serialization.InternalSerializationHelper.deserializeResult;
import static io.github.bucket4j.distributed.serialization.InternalSerializationHelper.serializeRequest;

/**
 * The Apache Ignite 3.x specific implementation of the proxy manager based on colocated compute jobs,
 * see {@link Bucket4jIgnite3ComputeJob} for the job that actually runs on the server side.
 *
 * <p>
 * Buckets are keyed by plain {@link String} identifiers, that are stored together with the serialized
 * bucket state in the table described by the configured {@link BucketTableSettings}.
 */
public class IgniteComputeProxyManager extends AbstractProxyManager<String> {

    private final IgniteClient client;
    private final BucketTableSettings tableSettings;
    private final JobDescriptor<byte[], byte[]> jobDescriptor;

    public IgniteComputeProxyManager(IgniteComputeProxyManagerBuilder builder) {
        super(builder.getClientSideConfig());
        this.client = builder.getClient();
        this.tableSettings = builder.getTableSettings();
        this.jobDescriptor = JobDescriptor.builder(Bucket4jIgnite3ComputeJob.class).build();
    }

    @Override
    public <T> CommandResult<T> execute(String key, Request<T> request) {
        byte[] argument = encodeArgument(key, request);
        byte[] resultBytes = client.compute().execute(target(key), jobDescriptor, argument);
        return deserializeResult(resultBytes, request.getBackwardCompatibilityVersion(), SerializationStyle.BYTE_BUFFER);
    }

    @Override
    public boolean isAsyncModeSupported() {
        return true;
    }

    @Override
    public <T> CompletableFuture<CommandResult<T>> executeAsync(String key, Request<T> request) {
        byte[] argument = encodeArgument(key, request);
        Version backwardCompatibilityVersion = request.getBackwardCompatibilityVersion();
        return client.compute().executeAsync(target(key), jobDescriptor, argument)
            .thenApply((byte[] resultBytes) -> deserializeResult(resultBytes, backwardCompatibilityVersion, SerializationStyle.BYTE_BUFFER));
    }

    @Override
    public void removeProxy(String key) {
        keyValueView().remove(null, keyTuple(key));
    }

    @Override
    protected CompletableFuture<Void> removeAsync(String key) {
        return keyValueView().removeAsync(null, keyTuple(key)).thenApply(result -> null);
    }

    private byte[] encodeArgument(String key, Request<?> request) {
        byte[] requestBytes = serializeRequest(request, SerializationStyle.BYTE_BUFFER);
        return new Bucket4jIgnite3ComputeArgument(tableSettings.getTableName(), tableSettings.getIdName(), tableSettings.getStateName(), key, requestBytes).encode();
    }

    private JobTarget target(String key) {
        return JobTarget.colocated(tableSettings.getTableName(), keyTuple(key));
    }

    private Tuple keyTuple(String key) {
        return Tuple.create().set(tableSettings.getIdName(), key);
    }

    private KeyValueView<Tuple, Tuple> keyValueView() {
        return client.tables().table(tableSettings.getTableName()).keyValueView();
    }

}
