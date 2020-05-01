package io.github.bucket4j.distributed.remote;

import io.github.bucket4j.serialization.DeserializationAdapter;
import io.github.bucket4j.serialization.SerializationAdapter;
import io.github.bucket4j.serialization.SerializationHandle;
import io.github.bucket4j.util.ComparableByContent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MultiResult implements ComparableByContent<MultiResult> {

    private List<CommandResult<?>> results;

    public static SerializationHandle<MultiResult> SERIALIZATION_HANDLE = new SerializationHandle<MultiResult>() {
        @Override
        public <S> MultiResult deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            int size = adapter.readInt(input);
            List<CommandResult<?>> results = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                CommandResult<?> result = CommandResult.SERIALIZATION_HANDLE.deserialize(adapter, input);
                results.add(result);
            }
            return new MultiResult(results);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, MultiResult multiResult) throws IOException {
            adapter.writeInt(output, multiResult.results.size());
            for (CommandResult<?> result : multiResult.results) {
                CommandResult.SERIALIZATION_HANDLE.serialize(adapter, output, result);
            }
        }

        @Override
        public int getTypeId() {
            return 13;
        }

        @Override
        public Class<MultiResult> getSerializedType() {
            return MultiResult.class;
        }

    };

    public MultiResult(List<CommandResult<?>> results) {
        this.results = results;
    }

    public List<CommandResult<?>> getResults() {
        return results;
    }

    @Override
    public boolean equalsByContent(MultiResult other) {
        if (results.size() != other.results.size()) {
            return false;
        }
        for (int i = 0; i < results.size(); i++) {
            CommandResult<?> result1 = results.get(i);
            CommandResult<?> result2 = other.results.get(i);
            if (!ComparableByContent.equals(result1, result2)) {
                return false;
            }
        }
        return true;
    }

}
