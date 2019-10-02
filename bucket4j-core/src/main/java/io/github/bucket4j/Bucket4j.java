/*
 *
 *   Copyright 2015-2017 Vladimir Bukhtoyarov
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.github.bucket4j;

import io.github.bucket4j.distributed.proxy.*;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.commands.GetConfigurationCommand;
import io.github.bucket4j.local.LocalBucketBuilder;

import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * This is entry point for functionality provided bucket4j library.
 */
public class Bucket4j {

    private Bucket4j() {
        // to avoid initialization of utility class
    }

    /**
     * Creates the new builder of in-memory buckets.
     *
     * @return new instance of {@link LocalBucketBuilder}
     */
    public static LocalBucketBuilder builder() {
        return new LocalBucketBuilder();
    }

    /**
     * Creates new instance of {@link ConfigurationBuilder}
     *
     * @return instance of {@link ConfigurationBuilder}
     */
    public static ConfigurationBuilder configurationBuilder() {
        return new ConfigurationBuilder();
    }

}

