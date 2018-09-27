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


import java.util.ArrayList;
import java.util.List;

/**
 * TODO add notes for building configuration
 *
 */
public class ConfigurationBuilder {

    private final List<Bandwidth> bandwidths;
    private final BucketOptions options;
    private MathType mathType;

    public ConfigurationBuilder(BucketOptions options) {
        this.bandwidths = new ArrayList<>(1);
        this.options = options;
        this.mathType = options.getDefaultMathType();
    }

    /**
     * @return configuration that used for bucket construction.
     */
    public BucketConfiguration build() {
        return new BucketConfiguration(this.bandwidths, mathType);
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

    // TODO javadocs
    public ConfigurationBuilder withMath(MathType mathType) {
        if (!options.getSupportedMathTypes().contains(mathType)) {
            // TODO provide message and implement tests
            throw new IllegalArgumentException();
        }
        this.mathType = mathType;
        return this;
    }

    @Override
    public String toString() {
        return "ConfigurationBuilder{" +
                "bandwidths=" + bandwidths +
                ", options=" + options +
                ", mathType=" + mathType +
                '}';
    }

}