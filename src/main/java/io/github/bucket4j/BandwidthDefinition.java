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


class BandwidthDefinition {

    private final Bandwidth bandwidth;
    private final long initialTokens;

    private BandwidthDefinition(Bandwidth bandwidth, long initialTokens) {
        this.bandwidth = bandwidth;
        this.initialTokens = initialTokens;
    }

    public static BandwidthDefinition withInitialTokens(Bandwidth bandwidth, long initialTokens) {
        if (bandwidth == null) {
            throw BucketExceptions.nullBandwidth();
        }
        if (initialTokens < 0) {
            throw BucketExceptions.nonPositiveInitialTokens(initialTokens);
        }
        return new BandwidthDefinition(bandwidth, initialTokens);
    }

    public static BandwidthDefinition unspecifiedInitialTokens(Bandwidth bandwidth) {
        if (bandwidth == null) {
            throw BucketExceptions.nullBandwidth();
        }
        return new BandwidthDefinition(bandwidth, BucketConfiguration.INITIAL_TOKENS_UNSPECIFIED);
    }

    public Bandwidth getBandwidth() {
        return bandwidth;
    }

    public long getInitialTokens() {
        return initialTokens;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("BandwidthDefinition{");
        sb.append("bandwidth=").append(bandwidth);
        sb.append(", initialTokens=").append(initialTokens);
        sb.append('}');
        return sb.toString();
    }

}
