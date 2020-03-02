package io.github.bucket4j;


/**
 * A builder for buckets. Builder can be reused, i.e. one builder can create multiple buckets with similar configuration.
 *
 */
public class AbstractBucketBuilder<T extends AbstractBucketBuilder> {

    private final ConfigurationBuilder configurationBuilder;

    protected AbstractBucketBuilder() {
        configurationBuilder = new ConfigurationBuilder();
    }

    /**
     * Adds limited bandwidth for all buckets which will be constructed by this builder.
     *
     * @param bandwidth limitation
     * @return this builder instance
     */
    public T addLimit(Bandwidth bandwidth) {
        configurationBuilder.addLimit(bandwidth);
        return (T) this;
    }

    protected BucketConfiguration buildConfiguration() {
        return configurationBuilder.build();
    }

}
