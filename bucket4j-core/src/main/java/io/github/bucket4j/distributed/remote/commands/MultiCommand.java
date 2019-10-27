package io.github.bucket4j.distributed.remote.commands;

import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.MutableBucketEntry;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.remote.RemoteCommand;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MultiCommand implements RemoteCommand<MultiResult>, Serializable {

    private static final long serialVersionUID = 42;

    private List<RemoteCommand<?>> commands;

    public MultiCommand(List<RemoteCommand<?>> commands) {
        this.commands = commands;
    }

    @Override
    public CommandResult<MultiResult> execute(MutableBucketEntry mutableEntry, long currentTimeNanos) {
        RemoteBucketState startingState = mutableEntry.exists() ? mutableEntry.get() : null;
        BucketEntryWrapper entryWrapper = new BucketEntryWrapper(startingState);

        List<CommandResult<?>> singleResults = new ArrayList<>(commands.size());
        for (RemoteCommand<?> singleCommand : commands) {
            singleResults.add(singleCommand.execute(entryWrapper, currentTimeNanos));
        }

        if (entryWrapper.stateModified) {
            mutableEntry.set(entryWrapper.state);
        }

        return CommandResult.success(new MultiResult(singleResults));
    }

    @Override
    public boolean isInitializationCommand() {
        for (RemoteCommand command : commands) {
            if (command.isInitializationCommand()) {
                return true;
            }
        }
        return false;
    }

    public List<RemoteCommand<?>> getCommands() {
        return commands;
    }

    private static class BucketEntryWrapper implements MutableBucketEntry {

        private RemoteBucketState state;
        private boolean stateModified;

        public BucketEntryWrapper(RemoteBucketState state) {
            this.state = state;
        }

        @Override
        public boolean exists() {
            return state != null;
        }

        @Override
        public void set(RemoteBucketState state) {
            this.state = Objects.requireNonNull(state);
            this.stateModified = true;
        }

        @Override
        public RemoteBucketState get() {
            if (state == null) {
                throw new IllegalStateException("'exists' must be called before 'get'");
            }
            return state;
        }

    }

}
