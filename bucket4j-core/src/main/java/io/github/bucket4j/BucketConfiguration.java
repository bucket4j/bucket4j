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

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class BucketConfiguration implements Serializable {

    private static final long serialVersionUID = 42L;

    private final Bandwidth[] bandwidths;
    private final MathType mathType;

    public BucketConfiguration(List<Bandwidth> bandwidths, MathType mathType) {
        this.mathType = Objects.requireNonNull(mathType);

        Objects.requireNonNull(bandwidths);
        if (bandwidths.isEmpty()) {
            throw BucketExceptions.restrictionsNotSpecified();
        }
        this.bandwidths = new Bandwidth[bandwidths.size()];
        for (int i = 0; i < bandwidths.size() ; i++) {
            this.bandwidths[i] = Objects.requireNonNull(bandwidths.get(i));
        }
    }

    public Bandwidth[] getBandwidths() {
        return bandwidths;
    }

    @Override
    public String toString() {
        return "BucketConfiguration{" +
                "bandwidths=" + Arrays.toString(bandwidths) +
                '}';
    }

    public void checkCompatibility(BucketConfiguration newConfiguration) {
        if (!isCompatible(newConfiguration)) {
            throw new IncompatibleConfigurationException(this, newConfiguration);
        }
    }

    public boolean isCompatible(BucketConfiguration newConfiguration) {
        return bandwidths.length == newConfiguration.bandwidths.length;
    }

    public MathType getMathType() {
        return mathType;
    }

}
