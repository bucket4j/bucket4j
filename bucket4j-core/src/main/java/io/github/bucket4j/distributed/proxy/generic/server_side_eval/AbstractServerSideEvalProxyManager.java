/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2020 Vladimir Bukhtoyarov
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
package io.github.bucket4j.distributed.proxy.generic.server_side_eval;

import io.github.bucket4j.distributed.proxy.AbstractProxyManager;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AbstractCompareAndSwapBasedProxyManager;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.remote.Request;

import java.util.concurrent.CompletableFuture;

/**
 * <b>RFC skeleton — not implemented, not wired into any execution path.</b>
 * See {@code docs/rfc/server-side-execution-abstraction.md} for the full proposal,
 * the problem it responds to, and the open questions that block turning this into
 * real, mergeable code.
 *
 * <p>Structurally mirrors
 * {@link AbstractCompareAndSwapBasedProxyManager} (see
 * {@code AbstractCompareAndSwapBasedProxyManager.java:46-103} for the class this sketch
 * is modeled on) but proposes a fourth family of proxy-manager base class, alongside
 * {@code compare_and_swap}, {@code select_for_update} and {@code pessimistic_locking} in
 * {@link io.github.bucket4j.distributed.proxy.generic}, for backends that can execute a
 * whole read-compute-write operation atomically in a single round trip (e.g. a Redis Lua
 * {@code EVALSHA}), rather than requiring the optimistic
 * get-bytes / compute-in-JVM / compare-and-swap-write cycle that
 * {@code AbstractCompareAndSwapBasedProxyManager} performs.
 *
 * <p>Every method body below throws {@link UnsupportedOperationException} — there is no
 * real logic in this class, anywhere. It exists only so a reviewer has concrete method
 * signatures to react to, not a working implementation to evaluate.
 *
 * @param <K> the generic type for unique identifiers that used to point to the bucket in
 *            external storage, mirroring {@code AbstractProxyManager<K>}.
 */
public abstract class AbstractServerSideEvalProxyManager<K> extends AbstractProxyManager<K> {

    protected AbstractServerSideEvalProxyManager(ClientSideConfig clientSideConfig) {
        super(clientSideConfig);
        throw new UnsupportedOperationException(
            "RFC skeleton - not implemented, see docs/rfc/server-side-execution-abstraction.md");
    }

    /**
     * RFC sketch, mirroring {@code AbstractCompareAndSwapBasedProxyManager.execute}
     * ({@code AbstractCompareAndSwapBasedProxyManager.java:55}). A real implementation
     * would, for commands where
     * {@link ServerSideEvaluableCommand#isServerSideEvaluationSupported()} is {@code true},
     * dispatch a single atomic server-side evaluation instead of the
     * get/compute/compare-and-swap loop; for ineligible commands, this sketch assumes
     * delegation to an existing CAS/lock-based manager, though how that delegation would
     * be wired is an open question (see design doc).
     */
    @Override
    public <T> CommandResult<T> execute(K key, Request<T> request) {
        throw new UnsupportedOperationException(
            "RFC skeleton - not implemented, see docs/rfc/server-side-execution-abstraction.md");
    }

    /**
     * RFC sketch, mirroring {@code AbstractCompareAndSwapBasedProxyManager.executeAsync}
     * ({@code AbstractCompareAndSwapBasedProxyManager.java:93}).
     */
    @Override
    public <T> CompletableFuture<CommandResult<T>> executeAsync(K key, Request<T> request) {
        throw new UnsupportedOperationException(
            "RFC skeleton - not implemented, see docs/rfc/server-side-execution-abstraction.md");
    }

    /**
     * RFC sketch, mirroring {@code AbstractProxyManager.removeAsync}
     * ({@code AbstractProxyManager.java:234}).
     */
    @Override
    protected CompletableFuture<Void> removeAsync(K key) {
        throw new UnsupportedOperationException(
            "RFC skeleton - not implemented, see docs/rfc/server-side-execution-abstraction.md");
    }

    /**
     * RFC sketch of the hook a concrete backend (e.g. a Redis client integration) would
     * implement to perform the single atomic server-side evaluation for one command.
     * Mirrors the role of
     * {@code AbstractCompareAndSwapBasedProxyManager.beginCompareAndSwapOperation}
     * ({@code AbstractCompareAndSwapBasedProxyManager.java:101}), but instead of
     * returning a transaction/operation handle for a subsequent compare-and-swap step,
     * this sketch assumes the backend performs read+compute+write in one call.
     *
     * <p>Open question (see design doc): what is the right return type here? This
     * sketch has no answer — a real proposal needs a defined result envelope
     * (equivalent to what {@link RemoteCommand#execute} returns) that server-side
     * evaluations across different backends could produce consistently.
     *
     * @param key the bucket key.
     * @param request the request to evaluate; only requests whose
     *                 {@link RemoteCommand} implements
     *                 {@link ServerSideEvaluableCommand} and reports itself eligible
     *                 would reach this method in a real implementation.
     */
    protected abstract <T> CommandResult<T> evaluateServerSide(K key, Request<T> request);

    /**
     * RFC sketch of the async counterpart to {@link #evaluateServerSide(Object, Request)}.
     */
    protected abstract <T> CompletableFuture<CommandResult<T>> evaluateServerSideAsync(K key, Request<T> request);

    /**
     * RFC sketch of the eligibility gate a real implementation would need: given a
     * request, decide whether its command can be routed through
     * {@link #evaluateServerSide(Object, Request)} at all, or whether it must fall back
     * to an existing CAS/lock-based manager. Section 5.1/9.B of the design doc's source
     * research explicitly notes no such capability-detection code exists anywhere in the
     * codebase today; this method is a placeholder for where it would live, not an
     * implementation of it.
     *
     * @param request the request under consideration.
     * @return {@code true} if {@code request}'s command both implements
     *         {@link ServerSideEvaluableCommand} and reports itself eligible via
     *         {@link ServerSideEvaluableCommand#isServerSideEvaluationSupported()}.
     */
    protected <T> boolean isEligibleForServerSideEvaluation(Request<T> request) {
        throw new UnsupportedOperationException(
            "RFC skeleton - not implemented, see docs/rfc/server-side-execution-abstraction.md");
    }

}
