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


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A builder for buckets. Builder can be reused, i.e. one builder can create multiple buckets with similar configuration.
 *
 */
public class ConfigurationBuilder<T extends ConfigurationBuilder> {

    private final Extension extension;
    private TimeMeter timeMeter;
    private List<BandwidthDefinition> bandwidths;

    protected ConfigurationBuilder(Extension extension) {
        this.extension = Objects.requireNonNull(extension); // TODO unit tests for null checking
        this.timeMeter = TimeMeter.SYSTEM_MILLISECONDS;
        this.bandwidths = new ArrayList<>(1);
    }

    /**
     * @return configuration which used for bucket construction.
     */
    public BucketConfiguration buildConfiguration() {
        return new BucketConfiguration(this.bandwidths, timeMeter);
    }

    /**
     * Adds limited bandwidth for all buckets which will be constructed by this builder instance.
     *
     * @param bandwidth limitation
     * @return this builder instance
     */
    public T addLimit(Bandwidth bandwidth) {
        bandwidths.add(BandwidthDefinition.unspecifiedInitialTokens(bandwidth));
        return (T) this;
    }

    /**
     * Adds limited bandwidth for all buckets which will be constructed by this builder instance.
     *
     * @param bandwidth limitation
     * @param initialTokens the count of initial tokens
     *
     * @return this builder instance
     */
    public T addLimit(long initialTokens, Bandwidth bandwidth) {
        bandwidths.add(BandwidthDefinition.withInitialTokens(bandwidth, initialTokens));
        return (T) this;
    }

    /**
     * Creates instance of {@link ConfigurationBuilder} which will create buckets with {@link TimeMeter#SYSTEM_NANOTIME} as time meter.
     *
     * @return this builder instance
     *
     * @throws UnsupportedOperationException if particular extension behind the bucket does not support the time measurement customization.
     * @see Extension#isCustomTimeMeasurementSupported()
     */
    public T withNanosecondPrecision() throws UnsupportedOperationException {
        if (!extension.isCustomTimeMeasurementSupported()) {
            // TODO add unit test
            throw new UnsupportedOperationException();
        }
        this.timeMeter = TimeMeter.SYSTEM_NANOTIME;
        return (T) this;
    }

    /**
     * Creates instance of {@link ConfigurationBuilder} which will create buckets with {@link TimeMeter#SYSTEM_MILLISECONDS} as time meter.
     *
     * @return this builder instance
     * @throws UnsupportedOperationException if particular extension behind the bucket does not support the time measurement customization.
     * @see Extension#isCustomTimeMeasurementSupported()
     */
    public T withMillisecondPrecision() throws UnsupportedOperationException {
        if (!extension.isCustomTimeMeasurementSupported()) {
            // TODO add unit test
            throw new UnsupportedOperationException();
        }
        this.timeMeter = TimeMeter.SYSTEM_MILLISECONDS;
        return (T) this;
    }

    /**
     * Creates instance of {@link ConfigurationBuilder} which will create buckets with {@code customTimeMeter} as time meter.
     *
     * @param customTimeMeter object which will measure time.
     *
     * @return this builder instance
     * @throws UnsupportedOperationException if particular extension behind the bucket does not support the time measurement customization.
     * @see Extension#isCustomTimeMeasurementSupported()
     */
    public T withCustomTimePrecision(TimeMeter customTimeMeter) throws UnsupportedOperationException {
        if (!extension.isCustomTimeMeasurementSupported()) {
            // TODO add unit test
            throw new UnsupportedOperationException();
        }
        if (customTimeMeter == null) {
            throw BucketExceptions.nullTimeMeter();
        }
        this.timeMeter = customTimeMeter;
        return (T) this;
    }

    @Override
    public String toString() {
        return "AbstractBucketBuilder{" +
                "timeMeter=" + timeMeter +
                ", bandwidths=" + bandwidths +
                '}';
    }

}
