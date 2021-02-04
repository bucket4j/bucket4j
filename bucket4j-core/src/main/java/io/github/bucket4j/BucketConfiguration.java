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

import io.github.bucket4j.distributed.serialization.DeserializationAdapter;
import io.github.bucket4j.distributed.serialization.SerializationHandle;
import io.github.bucket4j.distributed.serialization.SerializationAdapter;
import io.github.bucket4j.distributed.versioning.Version;
import io.github.bucket4j.distributed.versioning.Versions;
import io.github.bucket4j.util.ComparableByContent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static io.github.bucket4j.distributed.versioning.Versions.v_5_0_0;

public final class BucketConfiguration implements ComparableByContent<BucketConfiguration> {

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
        for (int i = 0; i < this.bandwidths.length; i++) {
            if (this.bandwidths[i].getId() != Bandwidth.UNDEFINED_ID) {
                for (int j = i + 1; j < this.bandwidths.length; j++) {
                    if (Objects.equals(this.bandwidths[i].getId(), this.bandwidths[j].getId())) {
                        // TODO add unit test
                        throw BucketExceptions.foundTwoBandwidthsWithSameId(i, j, this.bandwidths[i].getId());
                    }
                }
            }
        }
    }

    public static ConfigurationBuilder builder() {
        return new ConfigurationBuilder();
    }

    public Bandwidth[] getBandwidths() {
        return bandwidths;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        BucketConfiguration that = (BucketConfiguration) o;

        return Arrays.equals(bandwidths, that.bandwidths);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bandwidths);
    }

    @Override
    public String toString() {
        return "BucketConfiguration{" +
                "bandwidths=" + Arrays.toString(bandwidths) +
                '}';
    }

    public static final SerializationHandle<BucketConfiguration> SERIALIZATION_HANDLE = new SerializationHandle<BucketConfiguration>() {
        @Override
        public <S> BucketConfiguration deserialize(DeserializationAdapter<S> adapter, S input, Version backwardCompatibilityVersion) throws IOException {
            int formatNumber = adapter.readInt(input);
            Versions.check(formatNumber, v_5_0_0, v_5_0_0);

            int bandwidthAmount = adapter.readInt(input);
            List<Bandwidth> bandwidths = new ArrayList<>(bandwidthAmount);
            for (int ii = 0; ii < bandwidthAmount; ii++) {
                Bandwidth bandwidth = Bandwidth.SERIALIZATION_HANDLE.deserialize(adapter, input, backwardCompatibilityVersion);
                bandwidths.add(bandwidth);
            }
            return new BucketConfiguration(bandwidths);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, BucketConfiguration configuration, Version backwardCompatibilityVersion) throws IOException {
            adapter.writeInt(output, v_5_0_0.getNumber());

            adapter.writeInt(output, configuration.bandwidths.length);
            for (Bandwidth bandwidth : configuration.bandwidths) {
                Bandwidth.SERIALIZATION_HANDLE.serialize(adapter, output, bandwidth, backwardCompatibilityVersion);
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
