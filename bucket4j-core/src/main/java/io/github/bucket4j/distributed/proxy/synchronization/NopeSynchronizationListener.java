package io.github.bucket4j.distributed.proxy.synchronization;

public class NopeSynchronizationListener implements SynchronizationListener {

    public static final NopeSynchronizationListener instance = new NopeSynchronizationListener();

    @Override
    public void incrementMergeCount(int count) {

    }

    @Override
    public void incrementSkipCount(int count) {

    }

}
