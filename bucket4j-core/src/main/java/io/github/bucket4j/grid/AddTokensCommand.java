
package io.github.bucket4j.grid;

import io.github.bucket4j.Nothing;
import io.github.bucket4j.serialization.DeserializationAdapter;
import io.github.bucket4j.serialization.SerializationAdapter;
import io.github.bucket4j.serialization.SerializationHandle;

import java.io.IOException;

public class AddTokensCommand implements GridCommand<Nothing> {

    private static final long serialVersionUID = 1L;

    private long tokensToAdd;

    public static SerializationHandle<AddTokensCommand> SERIALIZATION_HANDLE = new SerializationHandle<AddTokensCommand>() {
        @Override
        public <S> AddTokensCommand deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            long tokensToAdd = adapter.readLong(input);

            return new AddTokensCommand(tokensToAdd);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, AddTokensCommand command) throws IOException {
            adapter.writeLong(output, command.tokensToAdd);
        }

        @Override
        public int getTypeId() {
            return 6;
        }

        @Override
        public Class<AddTokensCommand> getSerializedType() {
            return AddTokensCommand.class;
        }

    };

    public AddTokensCommand(long tokensToAdd) {
        this.tokensToAdd = tokensToAdd;
    }

    @Override
    public Nothing execute(GridBucketState state, long currentTimeNanos) {
        state.refillAllBandwidth(currentTimeNanos);
        state.addTokens(tokensToAdd);
        return Nothing.INSTANCE;
    }

    @Override
    public boolean isBucketStateModified() {
        return true;
    }

    public long getTokensToAdd() {
        return tokensToAdd;
    }

}
