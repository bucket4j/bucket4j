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

/**
 * <b>Experimental RFC skeleton. Not wired into any execution path.</b>
 *
 * <p>This package sketches a possible fourth family of proxy-manager base classes,
 * alongside the existing {@code compare_and_swap}, {@code select_for_update} and
 * {@code pessimistic_locking} packages in
 * {@link io.github.bucket4j.distributed.proxy.generic}. It is intended purely to make a
 * design discussion concrete and reviewable.
 *
 * <p><b>None of the classes in this package are referenced from, or reachable via, any
 * shipping code path.</b> Nothing in {@code bucket4j-core} constructs, extends, or calls
 * these types. No existing {@code RemoteCommand} implementation implements the interface
 * declared here. Method bodies (where present) throw {@link UnsupportedOperationException}
 * rather than performing real work.
 *
 * <p>See {@code docs/rfc/server-side-execution-abstraction.md} at the repository root for
 * the full proposal, the problem statement it responds to, and the open questions that
 * must be resolved with maintainer input before any of this could become real,
 * mergeable, functional code.
 */
package io.github.bucket4j.distributed.proxy.generic.server_side_eval;
