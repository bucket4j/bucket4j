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

package io.github.bucket4j;

import io.github.bucket4j.local.LocalBucketBuilder;
import io.github.bucket4j.remote.Backend;
import io.github.bucket4j.remote.ProxyManager;
import io.github.bucket4j.remote.RemoteBucketBuilder;

import java.io.Serializable;

/**
 * This is entry point for functionality provided bucket4j library.
 */
public abstract class Bucket4j {

    /**
     * Creates the new builder of in-memory buckets.
     *
     * @return new instance of {@link LocalBucketBuilder}
     */
    public static LocalBucketBuilder builder() {
        return new LocalBucketBuilder();
    }

    // TODO javadocs
    public static ConfigurationBuilder configurationBuilder() {
        return new ConfigurationBuilder(LocalBucketBuilder.OPTIONS);
    }

    /**
     * Creates new instance of builder specific for this back-end.
     *
     * @return new builder instance
     */
    public static <K extends Serializable> RemoteBucketBuilder<K> builder(Backend<K> backend) {
        return new RemoteBucketBuilder<>(backend);
    }

    /**
     * Creates new instance of {@link ConfigurationBuilder}
     *
     * @return instance of {@link ConfigurationBuilder}
     */
    public static ConfigurationBuilder configurationBuilder(Backend<?> backend) {
        return new ConfigurationBuilder(backend.getOptions());
    }

    // TODO javadocs
    public static <K extends Serializable> ProxyManager<K> proxyManager(Backend<K> backend) {
        return new ProxyManager<>(backend);
    }

}
