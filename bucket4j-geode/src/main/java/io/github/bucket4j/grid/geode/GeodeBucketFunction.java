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

import java.io.Serializable;

import org.apache.geode.cache.CacheTransactionManager;
import org.apache.geode.cache.CommitConflictException;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.execute.Function;
import org.apache.geode.cache.execute.FunctionContext;
import org.apache.geode.cache.execute.RegionFunctionContext;

import io.github.bucket4j.distributed.remote.AbstractBinaryTransaction;
import io.github.bucket4j.distributed.remote.RemoteBucketState;

/**
 * Runs the whole compare-and-swap retry loop colocated with the data, inside a single {@code Function}
 * invocation - so a racing caller never has to make another client-server round-trip just to retry after
 * losing a race, unlike {@link GeodeProxyManager} where every retry is a fresh round-trip.
 *
 * <p>{@code FunctionService.onRegion(region).withFilter(singleKeySet)} only *routes* this function to the
 * member owning the key's bucket - exactly as documented on {@link GeodeProxyManager} - it does not
 * serialize two invocations of this function that race on the same key on that member. So this function
 * still relies on {@link CacheTransactionManager} for conflict detection, the same primitive
 * {@link GeodeProxyManager} uses, just invoked locally in a loop instead of once per client round-trip.
 *
 * @param <K> type of the key
 */
class GeodeBucketFunction<K> implements Function<byte[]>, Serializable {

    @Override
    public void execute(FunctionContext<byte[]> context) {
        RegionFunctionContext regionContext = (RegionFunctionContext) context;
        Region<K, byte[]> region = regionContext.getDataSet();
        @SuppressWarnings("unchecked")
        K key = (K) regionContext.getFilter().iterator().next();
        byte[] requestBytes = context.getArguments();

        CacheTransactionManager transactionManager = ((GemFireCache) region.getRegionService()).getCacheTransactionManager();

        byte[] resultBytes;
        while (true) {
            transactionManager.begin();
            try {
                byte[] currentBytes = region.get(key);
                resultBytes = new AbstractBinaryTransaction(requestBytes) {
                    @Override
                    public boolean exists() {
                        return currentBytes != null;
                    }

                    @Override
                    protected byte[] getRawState() {
                        return currentBytes;
                    }

                    @Override
                    protected void setRawState(byte[] newStateBytes, RemoteBucketState newState) {
                        region.put(key, newStateBytes);
                    }
                }.execute();
                transactionManager.commit();
                break;
            } catch (CommitConflictException e) {
                // another invocation of this function - racing on the same key, on this same member -
                // committed first; retry entirely against the freshly committed state, purely locally
            } finally {
                // commit() already rolls back internally on CommitConflictException, so exists() guards
                // against calling rollback() on a transaction that Geode already terminated
                if (transactionManager.exists()) {
                    transactionManager.rollback();
                }
            }
        }

        context.<byte[]>getResultSender().lastResult(resultBytes);
    }

    @Override
    public boolean optimizeForWrite() {
        // hints Geode to route to the primary copy of the key's bucket, since this function writes
        return true;
    }

    @Override
    public boolean isHA() {
        // Geode's default (true) would let it transparently re-run this whole function on another member
        // if the result never reached the caller (e.g. this member died right after commit() succeeded but
        // before lastResult() was delivered). That window is unavoidable in any client-server protocol, but
        // an automatic *silent* re-execution here would double-apply an already-committed, non-idempotent
        // bucket mutation. Disabling HA turns that ambiguity into a visible exception to the caller instead
        // of a silent double-consume - the same failure mode GeodeProxyManager already has for a lost ack.
        return false;
    }

}
