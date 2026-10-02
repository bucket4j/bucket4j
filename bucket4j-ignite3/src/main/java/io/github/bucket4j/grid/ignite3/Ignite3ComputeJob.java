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

import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.Request;
import io.github.bucket4j.distributed.serialization.InternalSerializationHelper;
import io.github.bucket4j.distributed.serialization.SerializationStyle;
import io.github.bucket4j.distributed.versioning.BackwardCompatibilityException;
import io.github.bucket4j.distributed.versioning.Versions;
import io.github.bucket4j.grid.ignite3.internal.JobInputCodec;
import io.github.bucket4j.util.concurrent.batch.AsyncBatchHelper;
import io.github.bucket4j.util.concurrent.batch.MultiAsyncBatcherHelper;

import org.apache.ignite.compute.ComputeJob;
import org.apache.ignite.compute.JobDescriptor;
import org.apache.ignite.compute.JobExecutionContext;
import org.apache.ignite.marshalling.ByteArrayMarshaller;
import org.apache.ignite.marshalling.Marshaller;


import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static io.github.bucket4j.distributed.serialization.InternalSerializationHelper.serializeResult;
import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * The server-side part of {@link Ignite3ProxyManager}: executes bucket4j requests on the node that owns the bucket's key.
 *
 * <p>The job argument is the envelope produced by {@link JobInputCodec}, and the job result is the serialized
 * {@link CommandResult}. Both are passed as raw {@code byte[]}, so neither Ignite nor the job depend on the
 * bucket4j classes during marshalling, which is what makes rolling upgrade possible: a request or an envelope
 * whose format is newer or older than the format supported by this node is answered with the serialized
 * {@link io.github.bucket4j.distributed.versioning.BackwardCompatibilityException} result instead of failing the job.
 *
 * <p>Ignite 3 transactions are {@code SERIALIZABLE} and conflicting transactions are rolled back, so concurrent
 * requests to the same bucket must not run their own transactions. Instead, the job accumulates the requests
 * that target the same key into batches and applies each batch in a single transaction, see
 * {@link Ignite3AsyncTransaction}. Requests to different keys are batched independently.
 *
 * <p>The job is referenced by {@link #JOB_DESCRIPTOR} by class name, without deployment units, so this class
 * must be on the classpath of every server node.
 *
 * @param <K> the type of bucket key
 */
public class Ignite3ComputeJob<K> implements ComputeJob<byte[], byte[]> {

    /**
     * Describes the job for {@code IgniteCompute}: the argument and the result are marshalled as plain {@code byte[]}.
     */
    public static final JobDescriptor<byte[], byte[]> JOB_DESCRIPTOR = (JobDescriptor) JobDescriptor.builder(Ignite3ComputeJob.class.getName())
            .argumentMarshaller(ByteArrayMarshaller.create())
            .resultMarshaller(ByteArrayMarshaller.create())
            .build();

    // registry key: table_name:ignite_node_name -> per-table batcher registry keyed by bucket key
    // is used to fight with SERIALIZABLE nature of Ignite-3 transactions that rollbacks conflicting transactions
    // idea is simple - instead of allow to independent requests to fight with each other we just do accumulation independent requests into batches
    // and then execute all batch in single interaction with Ignite transaction engine
    private static final ConcurrentHashMap<String, MultiAsyncBatcherHelper<?, Request<?>, CommandResult<?>, List<Request<?>>, List<CommandResult<?>>>> batchersPerTable = new ConcurrentHashMap<>();

    @Override
    public Marshaller<byte[], byte[]> inputMarshaller() {
        return ByteArrayMarshaller.create();
    }

    @Override
    public Marshaller<byte[], byte[]> resultMarshaller() {
        return ByteArrayMarshaller.create();
    }

    @Override
    public CompletableFuture<byte[]> executeAsync(JobExecutionContext context, byte[] jobBytes) {
        // deserialize job
        JobInputCodec.JobInput<K> jobInput;
        try {
            jobInput = JobInputCodec.decode(jobBytes);
        } catch (BackwardCompatibilityException e) {
            return completedFuture(serializeResult(e.toResult(), Versions.getOldest(), SerializationStyle.BYTE_BUFFER));
        }

        // deserialize request
        byte[] requestBytes  = jobInput.requestBytes();
        Request<?> request;
        try {
            request = InternalSerializationHelper.deserializeRequest(requestBytes, SerializationStyle.BYTE_BUFFER);
        } catch (BackwardCompatibilityException e) {
            return completedFuture(serializeResult(e.toResult(), Versions.getOldest(), SerializationStyle.BYTE_BUFFER));
        }

        // find appropriate batcher
        K key = jobInput.key();
        String tableName = jobInput.tableName();
        // to avoid mixing requests to different ignite instances inside same JVM(unlikely but can be)
        String registryKey = tableName + ":" + context.ignite().name();

        // schedule async execution via batcher shared with other requests targeting the same key
        CompletableFuture<CommandResult<?>> completableFuture = scheduleViaBatcher(context, registryKey, tableName, key, request);
        return completableFuture
            .thenApply((CommandResult<?> result) -> {
                try {
                    return serializeResult(result, request.getBackwardCompatibilityVersion(), SerializationStyle.BYTE_BUFFER);
                } catch (BackwardCompatibilityException e) {
                    return serializeResult(e.toResult(), request.getBackwardCompatibilityVersion(), SerializationStyle.BYTE_BUFFER);
                }
            });
    }

    @SuppressWarnings("unchecked")
    private CompletableFuture<CommandResult<?>> scheduleViaBatcher(JobExecutionContext context, String registryKey, String tableName, K key, Request<?> request) {
        MultiAsyncBatcherHelper<K, Request<?>, CommandResult<?>, List<Request<?>>, List<CommandResult<?>>> batchers =
                (MultiAsyncBatcherHelper<K, Request<?>, CommandResult<?>, List<Request<?>>, List<CommandResult<?>>>) batchersPerTable.computeIfAbsent(registryKey, k -> new MultiAsyncBatcherHelper<>());
        return batchers.executeAsync(key, request, (K k) -> createBatcher(context, tableName, k));
    }

    private AsyncBatchHelper<Request<?>, CommandResult<?>, List<Request<?>>, List<CommandResult<?>>> createBatcher(JobExecutionContext context, String tableName, K key) {
        return AsyncBatchHelper.create(
            (List<Request<?>> requests) -> requests,
            (List<Request<?>> requests) -> context.ignite().transactions().runInTransactionAsync(new Ignite3AsyncTransaction(context.ignite(), tableName, key, requests)),
            (List<Request<?>> requests, List<CommandResult<?>> results) -> results
        );
    }

}
