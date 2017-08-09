package io.github.bucket4j.local;

import io.github.bucket4j.Extension;

/**
 *
 */
public class InMemoryExtension implements Extension<LocalBucketBuilder> {

    public static InMemoryExtension INSTANCE = new InMemoryExtension();

    @Override
    public LocalBucketBuilder builder() {
        return new LocalBucketBuilder();
    }

    @Override
    public boolean isAsyncModeSupported() {
        return true;
    }

    @Override
    public boolean isCustomTimeMeasurementSupported() {
        return true;
    }

}
