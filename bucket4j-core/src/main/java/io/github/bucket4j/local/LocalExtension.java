package io.github.bucket4j.local;

import io.github.bucket4j.Extension;

/**
 * TODO
 */
public class LocalExtension implements Extension<LocalBucketBuilder> {

    public static LocalExtension INSTANCE = new LocalExtension();

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
