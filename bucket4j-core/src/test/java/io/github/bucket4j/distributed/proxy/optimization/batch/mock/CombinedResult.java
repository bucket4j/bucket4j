package io.github.bucket4j.distributed.proxy.optimization.batch.mock;

import java.util.List;

public class CombinedResult {

    private final List<?> results;

    public CombinedResult(List<?> results) {
        this.results = results;
    }

    public <T> List<T> getResults() {
        return (List<T>) results;
    }

}
