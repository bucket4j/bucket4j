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

    private final Bandwidth[] limitedBandwidths;
    private final Bandwidth guaranteedBandwidth;
    private final TimeMeter timeMeter;

    public BucketConfiguration(List<Bandwidth> limitedBandwidths, Bandwidth guaranteedBandwidth, TimeMeter timeMeter) {
        if (timeMeter == null) {
            throw nullTimeMeter();
        }
        this.timeMeter = timeMeter;

        if (limitedBandwidths.isEmpty()) {
            throw restrictionsNotSpecified();
        }
        this.limitedBandwidths = new Bandwidth[limitedBandwidths.size()];
        for (int i = 0; i < limitedBandwidths.size() ; i++) {
            this.limitedBandwidths[i] = limitedBandwidths.get(i);
        }

        this.guaranteedBandwidth = guaranteedBandwidth;
    }

    public TimeMeter getTimeMeter() {
        return timeMeter;
    }

    public Bandwidth[] getLimitedBandwidths() {
        return limitedBandwidths;
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
