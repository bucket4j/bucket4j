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

import io.github.bucket4j.serialization.DeserializationAdapter;
import io.github.bucket4j.serialization.SerializationHandle;
import io.github.bucket4j.serialization.SerializationAdapter;
import io.github.bucket4j.util.ComparableByContent;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class BucketConfiguration implements Serializable, ComparableByContent<BucketConfiguration> {

    private static final long serialVersionUID = 42L;

    private final Bandwidth[] bandwidths;

    public BucketConfiguration(List<Bandwidth> bandwidths) {
        Objects.requireNonNull(bandwidths);
        if (bandwidths.isEmpty()) {
            throw BucketExceptions.restrictionsNotSpecified();
        }
        this.bandwidths = new Bandwidth[bandwidths.size()];
        for (int i = 0; i < bandwidths.size() ; i++) {
            this.bandwidths[i] = Objects.requireNonNull(bandwidths.get(i));
        }
    }

    public static ConfigurationBuilder builder() {
        return new ConfigurationBuilder();
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

    public static SerializationHandle<BucketConfiguration> SERIALIZATION_HANDLE = new SerializationHandle<BucketConfiguration>() {
        @Override
        public <S> BucketConfiguration deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            int bandwidthAmount = adapter.readInt(input);
            List<Bandwidth> bandwidths = new ArrayList<>(bandwidthAmount);
            for (int ii = 0; ii < bandwidthAmount; ii++) {
                Bandwidth bandwidth = Bandwidth.SERIALIZATION_HANDLE.deserialize(adapter, input);
                bandwidths.add(bandwidth);
            }
            return new BucketConfiguration(bandwidths);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, BucketConfiguration configuration) throws IOException {
            adapter.writeInt(output, configuration.bandwidths.length);
            for (Bandwidth bandwidth : configuration.bandwidths) {
                Bandwidth.SERIALIZATION_HANDLE.serialize(adapter, output, bandwidth);
            }
        }

        @Override
        public int getTypeId() {
            return 2;
        }

        @Override
        public Class<BucketConfiguration> getSerializedType() {
            return BucketConfiguration.class;
        }

    };

    @Override
    public boolean equalsByContent(BucketConfiguration other) {
        if (bandwidths.length != other.bandwidths.length) {
            return false;
        }
        for (int i = 0; i < other.getBandwidths().length; i++) {
            Bandwidth bandwidth1 = bandwidths[i];
            Bandwidth bandwidth2 = other.bandwidths[i];
            if (!bandwidth1.equalsByContent(bandwidth2)) {
                return false;
            }
        }
        return true;
    }

}
