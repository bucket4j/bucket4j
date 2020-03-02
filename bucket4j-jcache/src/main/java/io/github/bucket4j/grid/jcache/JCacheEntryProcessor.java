
package io.github.bucket4j.grid.jcache;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Nothing;
import io.github.bucket4j.grid.CommandResult;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.GridCommand;

import javax.cache.processor.EntryProcessor;
import java.io.Serializable;


public interface JCacheEntryProcessor<K extends Serializable, T extends Serializable> extends Serializable, EntryProcessor<K, GridBucketState, CommandResult<T>> {

    static <K extends Serializable> JCacheEntryProcessor<K, Nothing> initStateProcessor(BucketConfiguration configuration) {
        return new InitStateProcessor<>(configuration);
    }

    static <K extends Serializable, T extends Serializable> JCacheEntryProcessor<K, T> executeProcessor(GridCommand<T> targetCommand) {
        return new ExecuteProcessor<>(targetCommand);
    }

    static <K extends Serializable, T extends Serializable> JCacheEntryProcessor<K, T> initStateAndExecuteProcessor(GridCommand<T> targetCommand, BucketConfiguration configuration) {
        return new InitStateAndExecuteProcessor<>(targetCommand, configuration);
    }

    default long currentTimeNanos() {
        return System.currentTimeMillis() * 1_000_000;
    }

}
