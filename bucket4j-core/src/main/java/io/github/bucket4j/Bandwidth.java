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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.github.bucket4j.BandwidthBuilder.BandwidthBuilderCapacityStage;
import io.github.bucket4j.distributed.serialization.DeserializationAdapter;
import io.github.bucket4j.distributed.serialization.Scope;
import io.github.bucket4j.distributed.serialization.SerializationAdapter;
import io.github.bucket4j.distributed.serialization.SerializationHandle;
import io.github.bucket4j.distributed.versioning.Version;
import io.github.bucket4j.distributed.versioning.Versions;
import io.github.bucket4j.util.ComparableByContent;

import static io.github.bucket4j.distributed.versioning.Versions.v_7_0_0;

/**
 *
 */
public class Bandwidth implements ComparableByContent<Bandwidth> {

    public static final String UNDEFINED_ID = null;
    public static final long UNSPECIFIED_TIME_OF_FIRST_REFILL = Long.MIN_VALUE;

    final long capacity;
    final long initialTokens;
    final long refillPeriodNanos;
    final long refillTokens;
    final boolean refillIntervally;
    final long timeOfFirstRefillMillis;
    final boolean useAdaptiveInitialTokens;
    final String id;

    /**
     * Creates a builder for {@link Bandwidth}
     *
     * @return a builder for {@link Bandwidth}
     */
    public static BandwidthBuilderCapacityStage builder() {
        return BandwidthBuilder.builder();
    }

    Bandwidth(long capacity, long refillPeriodNanos, long refillTokens, long initialTokens, boolean refillIntervally,
              long timeOfFirstRefillMillis, boolean useAdaptiveInitialTokens, String id) {
        this.capacity = capacity;
        this.initialTokens = initialTokens;
        this.refillPeriodNanos = refillPeriodNanos;
        this.refillTokens = refillTokens;
        this.refillIntervally = refillIntervally;
        this.timeOfFirstRefillMillis = timeOfFirstRefillMillis;
        this.useAdaptiveInitialTokens = useAdaptiveInitialTokens;
        this.id = id;
    }

    public boolean isIntervallyAligned() {
        return timeOfFirstRefillMillis != UNSPECIFIED_TIME_OF_FIRST_REFILL;
    }

    public long getTimeOfFirstRefillMillis() {
        return timeOfFirstRefillMillis;
    }

    public long getCapacity() {
        return capacity;
    }

    public long getInitialTokens() {
        return initialTokens;
    }

    public long getRefillPeriodNanos() {
        return refillPeriodNanos;
    }

    public long getRefillTokens() {
        return refillTokens;
    }

    public boolean isUseAdaptiveInitialTokens() {
        return useAdaptiveInitialTokens;
    }

    public boolean isRefillIntervally() {
        return refillIntervally;
    }

    public boolean isGready() {
        return !refillIntervally;
    }

    public String getId() {
        return id;
    }

    public static final SerializationHandle<Bandwidth> SERIALIZATION_HANDLE = new SerializationHandle<>() {
        @Override
        public <S> Bandwidth deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            int formatNumber = adapter.readInt(input);
            Versions.check(formatNumber, v_7_0_0, v_7_0_0);

            long capacity = adapter.readLong(input);
            long initialTokens = adapter.readLong(input);
            long refillPeriodNanos = adapter.readLong(input);
            long refillTokens = adapter.readLong(input);
            boolean refillIntervally = adapter.readBoolean(input);
            long timeOfFirstRefillMillis = adapter.readLong(input);
            boolean useAdaptiveInitialTokens = adapter.readBoolean(input);
            boolean hasId = adapter.readBoolean(input);
            String id = hasId ? adapter.readString(input) : UNDEFINED_ID;

            return new Bandwidth(capacity, refillPeriodNanos, refillTokens, initialTokens, refillIntervally,
                timeOfFirstRefillMillis, useAdaptiveInitialTokens, id);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, Bandwidth bandwidth, Version backwardCompatibilityVersion, Scope scope) throws IOException {
            adapter.writeInt(output, v_7_0_0.getNumber());

            adapter.writeLong(output, bandwidth.capacity);
            adapter.writeLong(output, bandwidth.initialTokens);
            adapter.writeLong(output, bandwidth.refillPeriodNanos);
            adapter.writeLong(output, bandwidth.refillTokens);
            adapter.writeBoolean(output, bandwidth.refillIntervally);
            adapter.writeLong(output, bandwidth.timeOfFirstRefillMillis);
            adapter.writeBoolean(output, bandwidth.useAdaptiveInitialTokens);
            adapter.writeBoolean(output, bandwidth.id != null);
            if (bandwidth.hasId()) {
                adapter.writeString(output, bandwidth.id);
            }
        }

        @Override
        public int getTypeId() {
            return 1;
        }

        @Override
        public Class<Bandwidth> getSerializedType() {
            return Bandwidth.class;
        }

        @Override
        public Bandwidth fromJsonCompatibleSnapshot(Map<String, Object> snapshot) {
            int formatNumber = readIntValue(snapshot, "version");
            Versions.check(formatNumber, v_7_0_0, v_7_0_0);

            long capacity = readLongValue(snapshot, "capacity");
            long initialTokens = readLongValue(snapshot, "initialTokens");
            long refillPeriodNanos = readLongValue(snapshot, "refillPeriodNanos");
            long refillTokens = readLongValue(snapshot, "refillTokens");
            boolean refillIntervally = (boolean) snapshot.get("refillIntervally");
            long timeOfFirstRefillMillis = readLongValue(snapshot, "timeOfFirstRefillMillis");
            boolean useAdaptiveInitialTokens = (boolean) snapshot.get("useAdaptiveInitialTokens");
            String id = (String) snapshot.get("id");

            return new Bandwidth(capacity, refillPeriodNanos, refillTokens, initialTokens, refillIntervally,
                timeOfFirstRefillMillis, useAdaptiveInitialTokens, id);
        }

        @Override
        public Map<String, Object> toJsonCompatibleSnapshot(Bandwidth bandwidth, Version backwardCompatibilityVersion, Scope scope) {
            Map<String, Object> result = new HashMap<>();
            result.put("version", v_7_0_0.getNumber());

            result.put("capacity", bandwidth.capacity);
            result.put("initialTokens", bandwidth.initialTokens);
            result.put("refillPeriodNanos", bandwidth.refillPeriodNanos);
            result.put("refillTokens", bandwidth.refillTokens);
            result.put("refillIntervally", bandwidth.refillIntervally);
            result.put("timeOfFirstRefillMillis", bandwidth.timeOfFirstRefillMillis);
            result.put("useAdaptiveInitialTokens", bandwidth.useAdaptiveInitialTokens);
            if (bandwidth.id != null) {
                result.put("id", bandwidth.id);
            }
            return result;
        }

        @Override
        public String getTypeName() {
            return "Bandwidth";
        }

    };

    public boolean hasId() {
        return id != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        Bandwidth bandwidth = (Bandwidth) o;

        if (capacity != bandwidth.capacity) { return false; }
        if (initialTokens != bandwidth.initialTokens) { return false; }
        if (refillPeriodNanos != bandwidth.refillPeriodNanos) { return false; }
        if (refillTokens != bandwidth.refillTokens) { return false; }
        if (refillIntervally != bandwidth.refillIntervally) { return false; }
        if (timeOfFirstRefillMillis != bandwidth.timeOfFirstRefillMillis) { return false; }
        return useAdaptiveInitialTokens == bandwidth.useAdaptiveInitialTokens;
    }

    @Override
    public int hashCode() {
        int result = (int) (capacity ^ (capacity >>> 32));
        result = 31 * result + (int) (initialTokens ^ (initialTokens >>> 32));
        result = 31 * result + (int) (refillPeriodNanos ^ (refillPeriodNanos >>> 32));
        result = 31 * result + (int) (refillTokens ^ (refillTokens >>> 32));
        result = 31 * result + (refillIntervally ? 1 : 0);
        result = 31 * result + (int) (timeOfFirstRefillMillis ^ (timeOfFirstRefillMillis >>> 32));
        result = 31 * result + (useAdaptiveInitialTokens ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Bandwidth{" + "capacity=" + capacity +
            ", initialTokens=" + initialTokens +
            ", refillPeriodNanos=" + refillPeriodNanos +
            ", refillTokens=" + refillTokens +
            ", refillIntervally=" + refillIntervally +
            ", timeOfFirstRefillMillis=" + timeOfFirstRefillMillis +
            ", useAdaptiveInitialTokens=" + useAdaptiveInitialTokens +
            '}';
    }

    @Override
    public boolean equalsByContent(Bandwidth other) {
        return capacity == other.capacity &&
                initialTokens == other.initialTokens &&
                refillPeriodNanos == other.refillPeriodNanos &&
                refillTokens == other.refillTokens &&
                refillIntervally == other.refillIntervally &&
                timeOfFirstRefillMillis == other.timeOfFirstRefillMillis &&
                useAdaptiveInitialTokens == other.useAdaptiveInitialTokens;
    }

}
