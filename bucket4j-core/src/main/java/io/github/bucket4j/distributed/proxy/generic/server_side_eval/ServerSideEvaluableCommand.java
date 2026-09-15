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

import io.github.bucket4j.distributed.remote.RemoteCommand;

import java.util.Map;

/**
 * <b>RFC skeleton — not implemented, not wired into any execution path.</b>
 * See {@code docs/rfc/server-side-execution-abstraction.md} for the full proposal.
 *
 * <p>Sketches a capability contract that a specific {@link RemoteCommand} implementation
 * (e.g. a hypothetical {@code TryConsumeCommand} or
 * {@code TryConsumeAndReturnRemainingTokensCommand}) could implement to declare that its
 * operation can be expressed, and therefore executed, entirely inside an atomic
 * server-side evaluation (a Redis Lua script, or any other stored-procedure-capable
 * backend), rather than requiring the read-modify-write-CAS round trip that
 * {@link io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AbstractCompareAndSwapBasedProxyManager}
 * performs today.
 *
 * <p>No shipping {@code RemoteCommand} implements this interface as of this RFC. The
 * method signatures below are a proposal for reviewers to react to, not a finished
 * contract — see the "Open questions" section of the design document for what is
 * intentionally left unresolved (in particular: how eligibility interacts with
 * {@link io.github.bucket4j.distributed.versioning.Versions}, and whether the
 * parameter representation below is sufficiently backend-agnostic).
 *
 * @param <T> the result type of the underlying {@link RemoteCommand}, mirrored here so a
 *            server-side-eval proxy manager could reconstruct a {@code CommandResult<T>}
 *            from the raw values returned by the backend evaluation.
 */
public interface ServerSideEvaluableCommand<T> {

    /**
     * RFC sketch of an eligibility check. The intent is for this to be evaluated at
     * bucket-build time (not per-request), so that a caller either gets the fast path
     * entirely or a clear, immediate fallback to the existing CAS/lock-based managers —
     * never a partial or runtime-surprising switch mid-request.
     *
     * <p>Open question (see design doc): should eligibility be a property of the command
     * instance (this method) or of the {@code BucketConfiguration} it was built from, or
     * both? This sketch assumes both may need to be consulted, which is left unresolved.
     *
     * @return {@code true} if this command instance describes an operation that a
     *         server-side-eval backend can execute atomically without falling back to
     *         the client-side read-modify-write path.
     */
    boolean isServerSideEvaluationSupported();

    /**
     * RFC sketch of a backend-agnostic parameter export. The intent is for this to
     * return exactly the numeric/scalar parameters a stored script (e.g. Redis Lua
     * {@code ARGV}) would need to compute and apply the operation atomically, without
     * requiring the backend to understand {@code RemoteCommand}, {@code Request}, or
     * any internal serialization format.
     *
     * <p>Open question (see design doc): this is sketched as a {@code Map<String, Object>}
     * for concreteness, but a real proposal would need a typed, versioned parameter
     * schema rather than a loosely-typed map, plus a defined mapping from each parameter
     * to how a given backend (Lua, PL/pgSQL, etc.) should bind it.
     *
     * @return a backend-agnostic description of the parameters required to evaluate this
     *         command's operation server-side.
     */
    Map<String, Object> exportServerSideEvaluationParameters();

    /**
     * RFC sketch of the inverse of {@link #exportServerSideEvaluationParameters()}:
     * given the raw scalar values a server-side evaluation returned (e.g. the fields a
     * Lua script placed in its return array), reconstruct a value of type {@code T} that
     * {@code CommandResult<T>} can carry back to the caller.
     *
     * <p>Open question (see design doc): the current {@code RemoteCommand.execute}
     * contract returns a full {@code CommandResult<T>}, including diagnostic fields
     * (e.g. state-modified flag) that a fast-path evaluation may not produce in the same
     * shape; this sketch does not resolve how those fields would be populated.
     *
     * @param rawServerSideResult the raw values returned by the backend's atomic
     *                            evaluation, in a backend-specific representation.
     * @return the reconstructed result value for this command.
     */
    T reconstructResultFromServerSideEvaluation(Object rawServerSideResult);

}
