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

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public final class BucketConfiguration implements Serializable {

    public static final long INITIAL_TOKENS_UNSPECIFIED = -1;
    private static final long serialVersionUID = 42L;

    private final Bandwidth[] bandwidths;
    private final long[] bandwidthsInitialTokens;

    public BucketConfiguration(List<BandwidthDefinition> bandwidths) {
        if (bandwidths.isEmpty()) {
            throw BucketExceptions.restrictionsNotSpecified();
        }
        this.bandwidths = new Bandwidth[bandwidths.size()];
        this.bandwidthsInitialTokens = new long[bandwidths.size()];
        for (int i = 0; i < bandwidths.size() ; i++) {
            this.bandwidths[i] = bandwidths.get(i).getBandwidth();
            this.bandwidthsInitialTokens[i] = bandwidths.get(i).getInitialTokens();
        }
    }

    public Bandwidth[] getBandwidths() {
        return bandwidths;
    }

    public long[] getBandwidthsInitialTokens() {
        return bandwidthsInitialTokens;
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

}
