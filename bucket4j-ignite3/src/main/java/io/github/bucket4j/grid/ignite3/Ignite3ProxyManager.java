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
package io.github.bucket4j.grid.ignite3;

import io.github.bucket4j.distributed.proxy.AbstractProxyManager;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.Request;
import io.github.bucket4j.distributed.serialization.SerializationStyle;
import io.github.bucket4j.grid.ignite3.internal.JobInputCodec;

import org.apache.ignite.Ignite;
import org.apache.ignite.compute.JobTarget;
import org.apache.ignite.table.KeyValueView;
import org.apache.ignite.table.Table;
import org.apache.ignite.table.mapper.Mapper;

import java.util.concurrent.CompletableFuture;

import static io.github.bucket4j.distributed.serialization.InternalSerializationHelper.deserializeResult;
import static io.github.bucket4j.distributed.serialization.InternalSerializationHelper.serializeRequest;

/**
 * The {@link io.github.bucket4j.distributed.proxy.ProxyManager} that keeps bucket state in a row of an Apache Ignite 3.x table.
 *
 * <p>Every command is serialized and sent as an {@link Ignite3ComputeJob} to the node that is colocated with the
 * bucket's key. The job reads the state, applies the command and writes the new state back inside an Ignite
 * transaction, so the state itself never travels over the network. Both the synchronous and the asynchronous
 * API are supported.
 *
 * <p>Instances are created via {@link Bucket4jIgnite3#builder()}, see {@link Bucket4jIgnite3} for the requirements
 * to the table structure and to the classpath of the cluster nodes.
 *
 * @param <K> the type of bucket key, one of {@code String}, {@code Long}, {@code Integer}, {@code Short},
 *            {@code Byte}, {@code UUID}
 */
public class Ignite3ProxyManager<K> extends AbstractProxyManager<K> {

    /**
     * The name of the primary-key column that identifies a bucket.
     */
    public static final String KEY_COLUMN_NAME = "BUCKET_KEY";

    /**
     * The name of the {@code VARBINARY} column that holds the serialized bucket state.
     */
    public static final String STATE_COLUMN_NAME = "BUCKET_STATE";

    private final Ignite ignite;
    private final String tableName;
    private final Mapper<K> keyMapper;
    private final KeyValueView<K, byte[]> keyValueView;

    Ignite3ProxyManager(Bucket4jIgnite3.Ignite3ProxyManagerBuilder<K> builder) {
        super(builder.getClientSideConfig());
        this.ignite = builder.getIgnite();
        this.tableName = builder.getTableName();
        Class<K> keyType = builder.getKeyType();
        this.keyMapper = Mapper.of(keyType);

        Table table = ignite.tables().table(tableName);
        if (table == null) {
            throw new IllegalArgumentException("Table '" + tableName + "' does not exist");
        }
        this.keyValueView = table.keyValueView(keyType, byte[].class);
    }

    @Override
    protected <T> CommandResult<T> execute(K key, Request<T> request) {
        byte[] requestBytes = serializeRequest(request, SerializationStyle.BYTE_BUFFER);
        byte[] encodedJobBytes =  JobInputCodec.encode(tableName, key, requestBytes);
        JobTarget jobTarget = JobTarget.colocated(tableName, key, keyMapper);
        byte[] resultBytes = ignite.compute().execute(jobTarget, Ignite3ComputeJob.JOB_DESCRIPTOR, encodedJobBytes);
        return deserializeResult(resultBytes, request.getBackwardCompatibilityVersion(), SerializationStyle.BYTE_BUFFER);
    }

    @Override
    protected <T> CompletableFuture<CommandResult<T>> executeAsync(K key, Request<T> request) {
        byte[] requestBytes = serializeRequest(request, SerializationStyle.BYTE_BUFFER);
        byte[] encodedJobBytes =  JobInputCodec.encode(tableName, key, requestBytes);
        JobTarget jobTarget = JobTarget.colocated(tableName, key, keyMapper);
        CompletableFuture<byte[]> resultFuture = ignite.compute().executeAsync(jobTarget, Ignite3ComputeJob.JOB_DESCRIPTOR, encodedJobBytes);
        return resultFuture.thenApply(resultBytes ->
                deserializeResult(resultBytes, request.getBackwardCompatibilityVersion(), SerializationStyle.BYTE_BUFFER));
    }

    /**
     * Deletes the row of the bucket directly from the calling node, the compute job is not involved.
     */
    @Override
    public void removeProxy(K key) {
        keyValueView.remove(null, key);
    }

    @Override
    protected CompletableFuture<Void> removeAsync(K key) {
        return keyValueView.removeAsync(null, key).thenApply(removed -> null);
    }

    @Override
    public boolean isAsyncModeSupported() {
        return true;
    }

}
