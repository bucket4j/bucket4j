/*
 * Copyright 2015 Vladimir Bukhtoyarov
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.github.bucket4j;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import static com.github.bucket4j.BucketExceptions.*;

public final class BucketConfiguration implements Serializable {

    public static final long INITIAL_TOKENS_UNSPECIFIED = -1;

    private final Bandwidth[] limitedBandwidths;
    private final long[] limitedBandwidthsInitialTokens;
    private final Bandwidth guaranteedBandwidth;
    private final long guaranteedBandwidthInitialTokens;
    private final TimeMeter timeMeter;

    public BucketConfiguration(List<BandwidthDefinition> limitedBandwidths, BandwidthDefinition guaranteedBandwidth, TimeMeter timeMeter) {
        if (timeMeter == null) {
            throw nullTimeMeter();
        }
        this.timeMeter = timeMeter;

        if (limitedBandwidths.isEmpty()) {
            throw restrictionsNotSpecified();
        }
        this.limitedBandwidths = new Bandwidth[limitedBandwidths.size()];
        this.limitedBandwidthsInitialTokens = new long[limitedBandwidths.size()];
        for (int i = 0; i < limitedBandwidths.size() ; i++) {
            this.limitedBandwidths[i] = limitedBandwidths.get(i).getBandwidth();
            this.limitedBandwidthsInitialTokens[i] = limitedBandwidths.get(i).getInitialTokens();
        }

        if (guaranteedBandwidth == null) {
            this.guaranteedBandwidth = null;
            this.guaranteedBandwidthInitialTokens = INITIAL_TOKENS_UNSPECIFIED;
        } else {
            this.guaranteedBandwidth = guaranteedBandwidth.getBandwidth();
            this.guaranteedBandwidthInitialTokens = guaranteedBandwidth.getInitialTokens();
        }
    }

    public TimeMeter getTimeMeter() {
        return timeMeter;
    }

    public Bandwidth[] getLimitedBandwidths() {
        return limitedBandwidths;
    }

    public long[] getLimitedBandwidthsInitialTokens() {
        return limitedBandwidthsInitialTokens;
    }

    public long getGuaranteedBandwidthInitialTokens() {
        return guaranteedBandwidthInitialTokens;
    }

    public Bandwidth getGuaranteedBandwidth() {
        return guaranteedBandwidth;
    }

    @Override
    public String toString() {
        return "BucketConfiguration{" +
                "limitedBandwidths=" + Arrays.toString(limitedBandwidths) +
                ", timeMeter=" + timeMeter +
                '}';
    }

}