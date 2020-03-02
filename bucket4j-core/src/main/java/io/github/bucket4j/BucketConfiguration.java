package io.github.bucket4j;

import io.github.bucket4j.serialization.DeserializationAdapter;
import io.github.bucket4j.serialization.SerializationHandle;
import io.github.bucket4j.serialization.SerializationAdapter;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class BucketConfiguration implements Serializable {

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
                Bandwidth bandwidth = adapter.readObject(input, Bandwidth.class);
                bandwidths.add(bandwidth);
            }
            return new BucketConfiguration(bandwidths);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, BucketConfiguration configuration) throws IOException {
            adapter.writeInt(output, configuration.bandwidths.length);
            for (Bandwidth bandwidth : configuration.bandwidths) {
                adapter.writeObject(output, bandwidth);
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

}
