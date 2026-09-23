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

import io.github.bucket4j.distributed.remote.AbstractBinaryTransaction;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.grid.ignite3.internal.ByteArrayListCodec;
import io.github.bucket4j.grid.ignite3.internal.JobInputCodec;
import org.apache.ignite.Ignite;
import org.apache.ignite.compute.ComputeJob;
import org.apache.ignite.compute.JobDescriptor;
import org.apache.ignite.compute.JobExecutionContext;
import org.apache.ignite.marshalling.ByteArrayMarshaller;
import org.apache.ignite.marshalling.Marshaller;
import org.apache.ignite.table.KeyValueView;
import org.apache.ignite.table.Table;
import org.apache.ignite.tx.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Server-side compute job that applies a batch of bucket4j requests, coalesced client-side for the same key
 * by {@link io.github.bucket4j.grid.ignite3.internal.BatchingRegistry}, against a single row of an Ignite 3 table
 * inside one transaction - so that N coalesced client calls cost one transaction and one network round trip
 * instead of N of each.
 */
public class Ignite3ComputeJob implements ComputeJob<byte[], byte[]> {

    public static final JobDescriptor<byte[], byte[]> JOB_DESCRIPTOR = JobDescriptor.builder(Ignite3ComputeJob.class)
            .argumentMarshaller(ByteArrayMarshaller.create())
            .resultMarshaller(ByteArrayMarshaller.create())
            .build();

    @Override
    public Marshaller<byte[], byte[]> inputMarshaller() {
        return ByteArrayMarshaller.create();
    }

    @Override
    public Marshaller<byte[], byte[]> resultMarshaller() {
        return ByteArrayMarshaller.create();
    }

    @Override
    public CompletableFuture<byte[]> executeAsync(JobExecutionContext context, byte[] input) {
        JobInputCodec.JobInput<Object> jobInput = JobInputCodec.decode(input);
        List<byte[]> requests = ByteArrayListCodec.decode(jobInput.requestBytes());

        Ignite ignite = context.ignite();
        Table table = ignite.tables().table(jobInput.tableName());
        Object key = jobInput.key();
        @SuppressWarnings({"unchecked", "rawtypes"})
        KeyValueView<Object, byte[]> keyValueView = (KeyValueView<Object, byte[]>) (KeyValueView) table.keyValueView(key.getClass(), byte[].class);

        Function<Transaction, byte[]> transactionBody = tx -> applyBatch(tx, keyValueView, key, requests);
        byte[] resultBytes = ignite.transactions().runInTransaction(transactionBody);
        return CompletableFuture.completedFuture(resultBytes);
    }

    private static byte[] applyBatch(Transaction tx, KeyValueView<Object, byte[]> keyValueView, Object key, List<byte[]> requests) {
        byte[] state = keyValueView.get(tx, key);
        boolean exists = state != null;

        List<byte[]> resultsBytes = new ArrayList<>(requests.size());
        for (byte[] requestBytes : requests) {
            SingleRequestTransaction transaction = new SingleRequestTransaction(requestBytes, state, exists);
            resultsBytes.add(transaction.execute());
            state = transaction.state;
            exists = transaction.exists;
        }

        if (exists) {
            keyValueView.put(tx, key, state);
        }

        return ByteArrayListCodec.encode(resultsBytes);
    }

    /**
     * Applies one bucket4j request against an in-memory holder of the row's raw state; the holder is threaded
     * across all requests in the batch and only read from / written to the real {@link KeyValueView} once per batch.
     */
    private static final class SingleRequestTransaction extends AbstractBinaryTransaction {

        private byte[] state;
        private boolean exists;

        SingleRequestTransaction(byte[] requestBytes, byte[] state, boolean exists) {
            super(requestBytes);
            this.state = state;
            this.exists = exists;
        }

        @Override
        protected byte[] getRawState() {
            return state;
        }

        @Override
        protected void setRawState(byte[] newStateBytes, RemoteBucketState newState) {
            this.state = newStateBytes;
            this.exists = true;
        }

        @Override
        public boolean exists() {
            return exists;
        }

    }

}
