package io.github.bucket4j.mongodb;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class MongoDBUtilitySubscriber<T, F> implements Subscriber<T> {
    private final CompletableFuture<F> future;

    public MongoDBUtilitySubscriber(CompletableFuture<F> future) {
        this.future = Objects.requireNonNull(future);
    }

    @Override
    public void onSubscribe(Subscription s) {
        s.request(1);
    }

    @Override
    public void onNext(T t) {
        future.complete(null);
    }

    @Override
    public void onError(Throwable t) {
        future.completeExceptionally(t);
    }

    @Override
    public void onComplete() {
        future.complete(null);
    }
}
