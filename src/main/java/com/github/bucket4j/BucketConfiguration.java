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

    private final Bandwidth[] bandwidths;
    private final TimeMeter timeMeter;

    public BucketConfiguration(List<Bandwidth> bandwidths, TimeMeter timeMeter) {
        if (timeMeter == null) {
            throw nullTimeMeter();
        }
        this.timeMeter = timeMeter;

        checkCompatibility(bandwidths);

        this.bandwidths = new Bandwidth[bandwidths.size()];
        for (int i = 0; i < bandwidths.size() ; i++) {
            this.bandwidths[i] = bandwidths.get(i);
        }
    }

    public TimeMeter getTimeMeter() {
        return timeMeter;
    }

    public Bandwidth[] getBandwidths() {
        return bandwidths;
    }

    public Bandwidth getBandwidth(int index) {
        return bandwidths[index];
    }

    public static void checkCompatibility(List<Bandwidth> bandwidths) {
        int countOfLimitedBandwidth = 0;
        int countOfGuaranteedBandwidth = 0;

        for (Bandwidth bandwidth : bandwidths) {
            if (bandwidth.isLimited()) {
                countOfLimitedBandwidth++;
            } else {
                countOfGuaranteedBandwidth++;
            }
        }

        if (countOfLimitedBandwidth == 0) {
            throw restrictionsNotSpecified();
        }

        if (countOfGuaranteedBandwidth > 1) {
            throw onlyOneGuarantedBandwidthSupported();
        }
    }

    @Override
    public String toString() {
        return "BucketConfiguration{" +
                "bandwidths=" + Arrays.toString(bandwidths) +
                ", timeMeter=" + timeMeter +
                '}';
    }

}
