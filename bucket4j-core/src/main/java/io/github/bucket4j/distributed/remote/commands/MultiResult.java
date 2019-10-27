package io.github.bucket4j.distributed.remote.commands;

import io.github.bucket4j.distributed.remote.CommandResult;

import java.io.Serializable;
import java.util.List;

public class MultiResult implements Serializable {

    private static final long serialVersionUID = 42;

    private List<CommandResult<?>> results;

    public MultiResult(List<CommandResult<?>> results) {
        this.results = results;
    }

    public List<CommandResult<?>> getResults() {
        return results;
    }

}
