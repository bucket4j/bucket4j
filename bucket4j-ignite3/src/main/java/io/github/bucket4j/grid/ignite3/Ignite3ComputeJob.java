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
import io.github.bucket4j.distributed.remote.MutableBucketEntry;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.remote.Request;
import io.github.bucket4j.distributed.serialization.InternalSerializationHelper;
import io.github.bucket4j.distributed.serialization.SerializationStyle;
import io.github.bucket4j.distributed.versioning.UnsupportedTypeException;
import io.github.bucket4j.distributed.versioning.UsageOfObsoleteApiException;
import io.github.bucket4j.distributed.versioning.UsageOfUnsupportedApiException;
import io.github.bucket4j.distributed.versioning.Versions;
import io.github.bucket4j.grid.ignite3.internal.JobInputCodec;
import io.github.bucket4j.util.concurrent.batch.AsyncBatchHelper;
import io.github.bucket4j.util.concurrent.batch.MultiAsyncBatcherHelper;

import org.apache.ignite.Ignite;
import org.apache.ignite.compute.ComputeJob;
import org.apache.ignite.compute.JobDescriptor;
import org.apache.ignite.compute.JobExecutionContext;
import org.apache.ignite.marshalling.ByteArrayMarshaller;
import org.apache.ignite.marshalling.Marshaller;
import org.apache.ignite.table.KeyValueView;
import org.apache.ignite.table.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static io.github.bucket4j.distributed.serialization.InternalSerializationHelper.serializeResult;
import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * TODO
 */
public class Ignite3ComputeJob<K> implements ComputeJob<byte[], byte[]> {

    public static final JobDescriptor<byte[], byte[]> JOB_DESCRIPTOR = (JobDescriptor) JobDescriptor.builder(Ignite3ComputeJob.class.getName())
            .argumentMarshaller(ByteArrayMarshaller.create())
            .resultMarshaller(ByteArrayMarshaller.create())
            .build();

    // registry key: ignite-instance-id+table_name -> per-table batcher registry keyed by bucket key
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
        JobInputCodec.JobInput<K> jobInput = JobInputCodec.decode(jobBytes);
        byte[] requestBytes  = jobInput.requestBytes();

        // deserialize request
        Request<?> request;
        try {
            request = InternalSerializationHelper.deserializeRequest(requestBytes, SerializationStyle.BYTE_BUFFER);
        } catch (UnsupportedTypeException e) {
            return completedFuture(serializeResult(CommandResult.unsupportedType(e.getTypeId()), Versions.getOldest(), SerializationStyle.BYTE_BUFFER));
        } catch (UsageOfUnsupportedApiException e) {
            return completedFuture(serializeResult(CommandResult.usageOfUnsupportedApiException(e.getRequestedFormatNumber(), e.getMaxSupportedFormatNumber()), Versions.getOldest(), SerializationStyle.BYTE_BUFFER));
        } catch (UsageOfObsoleteApiException e) {
            return completedFuture(serializeResult(CommandResult.usageOfObsoleteApiException(e.getRequestedFormatNumber(), e.getMinSupportedFormatNumber()), Versions.getOldest(), SerializationStyle.BYTE_BUFFER));
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
                } catch (UnsupportedTypeException e) {
                    return serializeResult(CommandResult.unsupportedType(e.getTypeId()), request.getBackwardCompatibilityVersion(), SerializationStyle.BYTE_BUFFER);
                } catch (UsageOfUnsupportedApiException e) {
                    return serializeResult(CommandResult.usageOfUnsupportedApiException(e.getRequestedFormatNumber(), e.getMaxSupportedFormatNumber()), request.getBackwardCompatibilityVersion(), SerializationStyle.BYTE_BUFFER);
                } catch (UsageOfObsoleteApiException e) {
                    return serializeResult(CommandResult.usageOfObsoleteApiException(e.getRequestedFormatNumber(), e.getMinSupportedFormatNumber()), request.getBackwardCompatibilityVersion(), SerializationStyle.BYTE_BUFFER);
                }
            });
    }

    @SuppressWarnings("unchecked")
    private CompletableFuture<CommandResult<?>> scheduleViaBatcher(JobExecutionContext context, String registryKey, String tableName, K key, Request<?> request) {
        MultiAsyncBatcherHelper<K, Request<?>, CommandResult<?>, List<Request<?>>, List<CommandResult<?>>> batchers =
                (MultiAsyncBatcherHelper<K, Request<?>, CommandResult<?>, List<Request<?>>, List<CommandResult<?>>>) (MultiAsyncBatcherHelper<?, Request<?>, CommandResult<?>, List<Request<?>>, List<CommandResult<?>>>)
                        batchersPerTable.computeIfAbsent(registryKey, k -> new MultiAsyncBatcherHelper<>());
        return batchers.executeAsync(key, request, (K k) -> createBatcher(context, tableName, k));
    }

    private AsyncBatchHelper<Request<?>, CommandResult<?>, List<Request<?>>, List<CommandResult<?>>> createBatcher(JobExecutionContext context, String tableName, K key) {
        return AsyncBatchHelper.create(
            (List<Request<?>> requests) -> requests,
            (List<Request<?>> requests) -> executeBatchAsync(context, tableName, key, requests),
            (List<Request<?>> requests, List<CommandResult<?>> results) -> results
        );
    }

    private CompletableFuture<List<CommandResult<?>>> executeBatchAsync(JobExecutionContext context, String tableName, K key, List<Request<?>> requests) {
        Ignite ignite = context.ignite();
        return ignite.transactions().runInTransactionAsync((tx) -> {
            Table table = ignite.tables().table(tableName);
            KeyValueView<K, byte[]> keyValueView = (KeyValueView<K, byte[]>) table.keyValueView(key.getClass(), byte[].class);
            return keyValueView.getAsync(tx, key).thenCompose(((byte[] stateBytes) -> {
                List<CommandResult<?>> results = new ArrayList<>(requests.size());
                MutableBucketEntry entryWrapper = new MutableBucketEntry(stateBytes);
                for (Request<?> request : requests) {
                    long currentTimeNanos = request.getClientSideTime() != null? request.getClientSideTime(): System.currentTimeMillis() * 1_000_000;
                    RemoteCommand<?> command = request.getCommand();
                    CommandResult<?> result = command.execute(entryWrapper, currentTimeNanos);
                    results.add(result);
                }
                if (!entryWrapper.isStateModified()) {
                    return CompletableFuture.completedFuture(results);
                } else {
                    Request<?> lastRequest = requests.get(requests.size() - 1);
                    byte[] finalState = entryWrapper.getStateBytes(lastRequest.getBackwardCompatibilityVersion());
                    return keyValueView.putAsync(tx, key, finalState).thenApply((Void v) -> results);
                }
            }));
        });

    }

}