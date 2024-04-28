/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2020 Vladimir Bukhtoyarov
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package io.github.bucket4j.distributed.remote.commands;

import io.github.bucket4j.distributed.remote.*;
import io.github.bucket4j.distributed.serialization.DeserializationAdapter;
import io.github.bucket4j.distributed.serialization.Scope;
import io.github.bucket4j.distributed.serialization.SerializationAdapter;
import io.github.bucket4j.distributed.serialization.SerializationHandle;
import io.github.bucket4j.distributed.versioning.Version;
import io.github.bucket4j.distributed.versioning.Versions;
import io.github.bucket4j.util.ComparableByContent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.github.bucket4j.distributed.versioning.Versions.v_7_0_0;

public class MultiCommand implements RemoteCommand<MultiResult>, ComparableByContent<MultiCommand> {

    public static final int MERGING_THRESHOLD = Integer.getInteger("bucket4j.batching.merging-threshold", 5);
    private final List<RemoteCommand<?>> commands;
    private int mergedCommands;

    public static SerializationHandle<MultiCommand> SERIALIZATION_HANDLE = new SerializationHandle<>() {
        @Override
        public <S> MultiCommand deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            int formatNumber = adapter.readInt(input);
            Versions.check(formatNumber, v_7_0_0, v_7_0_0);

            int size = adapter.readInt(input);
            List<RemoteCommand<?>> results = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                RemoteCommand<?> result = RemoteCommand.deserialize(adapter, input);
                results.add(result);
            }
            return new MultiCommand(results);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, MultiCommand multiCommand, Version backwardCompatibilityVersion, Scope scope) throws IOException {
            adapter.writeInt(output, v_7_0_0.getNumber());

            adapter.writeInt(output, multiCommand.commands.size());
            for (RemoteCommand<?> command : multiCommand.commands) {
                RemoteCommand.serialize(adapter, output, command, backwardCompatibilityVersion, scope);
            }
        }

        @Override
        public int getTypeId() {
            return 22;
        }

        @Override
        public Class<MultiCommand> getSerializedType() {
            return MultiCommand.class;
        }

        @Override
        public MultiCommand fromJsonCompatibleSnapshot(Map<String, Object> snapshot) throws IOException {
            int formatNumber = readIntValue(snapshot, "version");
            Versions.check(formatNumber, v_7_0_0, v_7_0_0);

            List<Map<String, Object>> commandSnapshots = (List<Map<String, Object>>) snapshot.get("commands");
            List<RemoteCommand<?>> commands = new ArrayList<>(commandSnapshots.size());
            for (Map<String, Object> commandSnapshot : commandSnapshots) {
                RemoteCommand<?> targetCommand = RemoteCommand.fromJsonCompatibleSnapshot(commandSnapshot);
                commands.add(targetCommand);
            }

            return new MultiCommand(commands);
        }

        @Override
        public Map<String, Object> toJsonCompatibleSnapshot(MultiCommand command, Version backwardCompatibilityVersion, Scope scope) throws IOException {
            Map<String, Object> result = new HashMap<>();
            result.put("version", v_7_0_0.getNumber());

            List<Map<String, Object>> commandSnapshots = new ArrayList<>(command.commands.size());
            for (RemoteCommand<?> remoteCommand : command.commands) {
                commandSnapshots.add(RemoteCommand.toJsonCompatibleSnapshot(remoteCommand, backwardCompatibilityVersion, scope));
            }

            result.put("commands", commandSnapshots);
            return result;
        }

        @Override
        public String getTypeName() {
            return "MultiCommand";
        }

    };

    public MultiCommand(List<RemoteCommand<?>> commands) {
        this.commands = commands;
    }

    public static MultiCommand merge(List<RemoteCommand<?>> commands) {
        if (commands.size() < MERGING_THRESHOLD) {
            return new MultiCommand(commands);
        }

        // try to merge TryConsume(tokens=1) or Verbose(TryConsume(tokens=1))  commands into one ConsumeAsMuchAsPossibleCommand in order to reduce network utilization
        int mergedCommandsCount = 0;
        List<RemoteCommand<?>> mergedCommands = null; // lazy in order to avoid allocation if there is nothing to merge
        for (int size = commands.size(), i = 0; i < size; i++) {
            RemoteCommand<?> command = commands.get(i);
            RemoteCommand<?> nextCommand = i + 1 < size ? commands.get(i + 1) : null;
            boolean canBeMerged = nextCommand != null && command.canBeMerged(nextCommand);
            if (!canBeMerged) {
                if (mergedCommands != null) {
                    mergedCommands.add(command);
                }
                continue;
            }
            if (mergedCommands == null) {
                mergedCommands = new ArrayList<>(commands.size());
                if (i > 0) {
                    mergedCommands.addAll(commands.subList(0, i));
                }
            }
            RemoteCommand<?> mergedCommand = command.toMergedCommand();
            while (canBeMerged) {
                nextCommand.mergeInto(mergedCommand);
                mergedCommandsCount++;
                i++;
                nextCommand = i + 1 < size ? commands.get(i + 1) : null;
                canBeMerged = nextCommand != null && command.canBeMerged(nextCommand);
            }
            mergedCommands.add(mergedCommand);
        }
        if (mergedCommands == null) {
            return new MultiCommand(commands);
        }

        MultiCommand multiCommand = new MultiCommand(mergedCommands);
        multiCommand.mergedCommands = mergedCommandsCount;
        return multiCommand;
    }

    public List<CommandResult<?>> unwrap(CommandResult<MultiResult> multiResult) {
        List<CommandResult<?>> results = multiResult.getData().getResults();
        if (!isMerged()) {
            return results;
        }
        List<CommandResult<?>> unwrappedResults = new ArrayList<>(results.size() + this.mergedCommands);
        for (int i = 0; i < commands.size(); i++) {
            RemoteCommand<?> command = commands.get(i);
            CommandResult<?> result = results.get(i);
            if (!command.isMerged()) {
                unwrappedResults.add(result);
                continue;
            }
            int mergedCommands = command.getMergedCommandsCount();
            for (int j = 0; j < mergedCommands; j++) {
                if (result.isError()) {
                    unwrappedResults.add(result);
                } else {
                    CommandResult<?> unwrapped = ((RemoteCommand)command).unwrapOneResult(result.getData(), j);
                    unwrappedResults.add(unwrapped);
                }
            }
        }
        return unwrappedResults;
    }

    @Override
    public boolean isMerged() {
        return this.mergedCommands > 0;
    }

    @Override
    public CommandResult<MultiResult> execute(MutableBucketEntry mutableEntry, long currentTimeNanos) {
        List<CommandResult<?>> singleResults = new ArrayList<>(commands.size());
        for (RemoteCommand<?> singleCommand : commands) {
            singleResults.add(singleCommand.execute(mutableEntry, currentTimeNanos));
        }
        return CommandResult.success(new MultiResult(singleResults), MultiResult.SERIALIZATION_HANDLE);
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

    @Override
    public SerializationHandle getSerializationHandle() {
        return SERIALIZATION_HANDLE;
    }

    public int getMergedCommandsCount() {
        return mergedCommands;
    }

    @Override
    public boolean equalsByContent(MultiCommand other) {
        if (commands.size() != other.commands.size()) {
            return false;
        }
        for (int i = 0; i < commands.size(); i++) {
            RemoteCommand<?> command1 = commands.get(i);
            RemoteCommand<?> command2 = other.commands.get(i);
            if (!ComparableByContent.equals(command1, command2)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isImmediateSyncRequired(long unsynchronizedTokens, long nanosSinceLastSync) {
        for (RemoteCommand<?> command : commands) {
            if (command.isImmediateSyncRequired(unsynchronizedTokens, nanosSinceLastSync)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public long estimateTokensToConsume() {
        long sum = 0;
        for (RemoteCommand<?> command : commands) {
            sum += command.estimateTokensToConsume();
            if (sum < 0L) {
                // math overflow
                return Long.MAX_VALUE;
            }
        }
        return sum;
    }

    @Override
    public long getConsumedTokens(MultiResult multiResult) {
        long sum = 0;
        int count = commands.size();
        for (int i = 0; i < count; i++) {
            RemoteCommand command = commands.get(i);
            CommandResult result = multiResult.getResults().get(i);
            sum += result.isError()? 0: command.getConsumedTokens(result.getData());
            if (sum < 0L) {
                // math overflow
                return Long.MAX_VALUE;
            }
        }
        return sum;
    }

    @Override
    public Version getRequiredVersion() {
        Version max = v_7_0_0;
        for (RemoteCommand<?> command : commands) {
            max = Versions.max(max, command.getRequiredVersion());
        }
        return max;
    }

}
