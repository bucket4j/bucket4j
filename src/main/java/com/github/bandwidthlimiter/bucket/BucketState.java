package com.github.bandwidthlimiter.bucket;

import java.util.Arrays;

public class BucketState {

    protected final long[] state;

    public BucketState(BucketConfiguration configuration) {
        this.state = new long[configuration.getStateSize()];
        long currentTime = configuration.getTimeMeter().currentTime();
        BandwidthAlgorithms.setupInitialState(configuration.getBandwidths(), this, currentTime);
    }

    public BucketState(BucketState previousState) {
        this.state = Arrays.copyOf(previousState.state, previousState.state.length);
    }

    public BucketState(long[] snapshot) {
        this.state = snapshot;
    }

    public long[] createSnapshot() {
        return Arrays.copyOf(state, state.length);
    }

    public long getValue(int offset) {
        return state[offset];
    }

    public void setValue(int offset, long value) {
        state[offset] = value;
    }

    @Override
    public BucketState clone() {
        return new BucketState(this);
    }

    public void copyState(BucketState state) {
        System.arraycopy(state.state, 0, this.state, 0, this.state.length);
    }

    public void copyState(long[] state) {
        System.arraycopy(state, 0, this.state, 0, this.state.length);
    }

}
