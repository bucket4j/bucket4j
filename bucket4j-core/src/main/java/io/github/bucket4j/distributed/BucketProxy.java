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
package io.github.bucket4j.distributed;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.BucketListener;
import io.github.bucket4j.TokensInheritanceStrategy;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import io.github.bucket4j.distributed.proxy.optimization.Optimization;

import java.time.Duration;

/**
 * Provides a light-weight proxy to bucket which state actually stored in external storage, like in-memory jvm or relational database.
 */
public interface BucketProxy extends Bucket {

    @Override
    BucketProxy toListenable(BucketListener listener);

    /**
     * Returns optimization controller for this proxy.
     *
     * <p>
     * This method is actual only if an optimization was applied during bucket construction via {@link RemoteBucketBuilder#withOptimization(Optimization)}
     * otherwise returned controller will do nothing.
     *
     * @return optimization controller for this proxy
     */
    OptimizationController getOptimizationController();

}
