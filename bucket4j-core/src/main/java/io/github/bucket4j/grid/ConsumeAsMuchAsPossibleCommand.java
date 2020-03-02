
package io.github.bucket4j.grid;


import io.github.bucket4j.serialization.DeserializationAdapter;
import io.github.bucket4j.serialization.SerializationAdapter;
import io.github.bucket4j.serialization.SerializationHandle;

import java.io.IOException;

public class ConsumeAsMuchAsPossibleCommand implements GridCommand<Long> {

    private static final long serialVersionUID = 1L;

    private long limit;
    private boolean bucketStateModified;

    public static SerializationHandle<ConsumeAsMuchAsPossibleCommand> SERIALIZATION_HANDLE = new SerializationHandle<ConsumeAsMuchAsPossibleCommand>() {
        @Override
        public <S> ConsumeAsMuchAsPossibleCommand deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            long limit = adapter.readLong(input);

            return new ConsumeAsMuchAsPossibleCommand(limit);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, ConsumeAsMuchAsPossibleCommand command) throws IOException {
            adapter.writeLong(output, command.limit);
        }

        @Override
        public int getTypeId() {
            return 7;
        }

        @Override
        public Class<ConsumeAsMuchAsPossibleCommand> getSerializedType() {
            return ConsumeAsMuchAsPossibleCommand.class;
        }

    };

    public ConsumeAsMuchAsPossibleCommand(long limit) {
        this.limit = limit;
    }

    @Override
    public Long execute(GridBucketState state, long currentTimeNanos) {
        state.refillAllBandwidth(currentTimeNanos);
        long availableToConsume = state.getAvailableTokens();
        long toConsume = Math.min(limit, availableToConsume);
        if (toConsume <= 0) {
            return 0L;
        }
        state.consume(toConsume);
        bucketStateModified = true;
        return toConsume;
    }

    @Override
    public boolean isBucketStateModified() {
        return bucketStateModified;
    }

    public long getLimit() {
        return limit;
    }

}
