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
package io.github.bucket4j;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import io.github.bucket4j.BandwidthBuilder.BandwidthBuilderBuildStage;
import io.github.bucket4j.BandwidthBuilder.BandwidthBuilderCapacityStage;

/**
 * The builder for {@link BucketConfiguration}
 *
 */
public class ConfigurationBuilder {

    private final List<Bandwidth> bandwidths;

    public ConfigurationBuilder() {
        this.bandwidths = new ArrayList<>(1);
    }

    /**
     * @return configuration that used for bucket construction.
     */
    public BucketConfiguration build() {
        return new BucketConfiguration(this.bandwidths);
    }

    /**
     * Adds limited bandwidth for all buckets which will be constructed by this builder instance.
     *
     * @param bandwidth limitation
     * @return this builder instance
     */
    public ConfigurationBuilder addLimit(Bandwidth bandwidth) {
        if (bandwidth == null) {
            throw BucketExceptions.nullBandwidth();
        }
        bandwidths.add(bandwidth);
        return this;
    }

    public ConfigurationBuilder addLimit(Function<BandwidthBuilderCapacityStage, BandwidthBuilderBuildStage> bandwidthConfigurator) {
        if (bandwidthConfigurator == null) {
            throw BucketExceptions.nullBuilder();
        }
        BandwidthBuilderBuildStage builder = bandwidthConfigurator.apply(Bandwidth.builder());
        Bandwidth bandwidth = builder.build();
        return addLimit(bandwidth);
    }

    @Override
    public String toString() {
        return "ConfigurationBuilder{" +
                "bandwidths=" + bandwidths +
                '}';
    }

}