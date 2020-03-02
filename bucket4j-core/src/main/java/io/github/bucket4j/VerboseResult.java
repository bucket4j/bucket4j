package io.github.bucket4j;

import java.io.Serializable;

/**
 * TODO write javadocs
 */
public class VerboseResult<T extends Serializable> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long operationTimeNanos;
    private final T value;
    private final BucketConfiguration configuration;
    private final BucketState state;

    public VerboseResult(long operationTimeNanos, T value, BucketConfiguration configuration, BucketState state) {
        this.operationTimeNanos = operationTimeNanos;
        this.value = value;
        this.configuration = configuration;
        this.state = state;
    }

    /**
     * TODO write javadocs
     */
    public T getValue() {
        return value;
    }

    /**
     * TODO write javadocs
     */
    public BucketConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * TODO write javadocs
     */
    public BucketState getState() {
        return state;
    }

    /**
     * TODO write javadocs
     */
    public long getOperationTimeNanos() {
        return operationTimeNanos;
    }

}
