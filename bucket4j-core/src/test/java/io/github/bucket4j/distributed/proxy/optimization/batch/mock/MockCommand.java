package io.github.bucket4j.distributed.proxy.optimization.batch.mock;

public interface MockCommand<R> {

    public R apply(MockState state);

}
