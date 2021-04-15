package io.github.bucket4j.grid.ignite.thin;

import org.apache.ignite.client.IgniteClientFuture;

import java.util.concurrent.CompletableFuture;

public class ThinClientUtils {

    public static <T> CompletableFuture<T> convertFuture(IgniteClientFuture<T> igniteFuture) {
        CompletableFuture<T> completableFuture = new CompletableFuture<>();
        igniteFuture.whenComplete((T result, Throwable error) -> {
            if (error != null) {
                completableFuture.completeExceptionally(error);
            } else {
                completableFuture.complete(result);
            }
        });
        return completableFuture;
    }

}
