package io.github.bucket4j;


import java.util.ArrayList;
import java.util.List;

/**
 * A builder for buckets. Builder can be reused, i.e. one builder can create multiple buckets with similar configuration.
 *
 */
public class ConfigurationBuilder {

    private List<Bandwidth> bandwidths;

    protected ConfigurationBuilder() {
        this.bandwidths = new ArrayList<>(1);
    }

    /**
     * @return configuration which used for bucket construction.
     */
    public BucketConfiguration build() {
        return new BucketConfiguration(this.bandwidths);
    }

    @Deprecated
    public BucketConfiguration buildConfiguration() {
        return build();
    }

    /**
     * Adds limited bandwidth for all buckets which will be constructed by this builder instance.
     *
     * @param bandwidth limitation
     * @return this builder instance
     */
    public ConfigurationBuilder addLimit(Bandwidth bandwidth) {
        if (bandwidth == null) {
            throw BucketExceptions.nullBandwidth();
        }
        bandwidths.add(bandwidth);
        return this;
    }

    @Override
    public String toString() {
        return "ConfigurationBuilder{" +
                ", bandwidths=" + bandwidths +
                '}';
    }

}
