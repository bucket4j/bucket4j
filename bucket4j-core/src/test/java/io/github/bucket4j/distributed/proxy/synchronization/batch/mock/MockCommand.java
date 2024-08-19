package io.github.bucket4j.distributed.proxy.synchronization.batch.mock;

public interface MockCommand<R> {

    R apply(MockState state);

}
