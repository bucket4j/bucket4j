package com.github.bandwidthlimiter.bucket;

import java.io.Serializable;

public interface BandwidthAdjuster extends Serializable {

    long getCapacity(long currentTime);

    public static class ImmutableCapacity implements BandwidthAdjuster {

        private final long value;

        public ImmutableCapacity(long value) {
            this.value = value;
        }

        @Override
        public long getCapacity(long currentTime) {
            return value;
        }

        @Override
        public String toString() {
            return "ImmutableCapacity{" +
                    "value=" + value +
                    '}';
        }

    }

}
