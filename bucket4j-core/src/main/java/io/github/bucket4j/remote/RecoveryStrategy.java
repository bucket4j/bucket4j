/*
 *
 * Copyright 2015-2018 Vladimir Bukhtoyarov
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package io.github.bucket4j.remote;

/**
 * Specifies the reaction which should be applied in case of previously saved state of bucket has been lost.
 *
 * The state of bucket can be lost by many reasons, for example:
 * <ul>
 *     <li>Split-brain happen.</li>
 *     <li>The bucket state was stored on single jvm node without replication strategy and this node was crashed.</li>
 *     <li>Wrong cache configuration.</li>
 *     <li>Pragmatically errors introduced by vendor.</li>
 *     <li>Human mistake.</li>
 * </ul>
 *
 * Each time when {@link BucketProxy} detects that bucket state is missed, it applies this strategy to react.
 */
public enum RecoveryStrategy {

    /**
     * Initialize bucket yet another time.
     * Use this strategy if availability is more preferred than consistency.
     */
    RECONSTRUCT,

    /**
     * Throw {@link BucketNotFoundException}.
     * Use this strategy if consistency is more preferred than availability.
     */
    THROW_BUCKET_NOT_FOUND_EXCEPTION

}
