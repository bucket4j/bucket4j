
package io.github.bucket4j.grid;

import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.serialization.DeserializationAdapter;
import io.github.bucket4j.serialization.SerializationAdapter;
import io.github.bucket4j.serialization.SerializationHandle;

import java.io.IOException;


public class TryConsumeAndReturnRemainingTokensCommand implements GridCommand<ConsumptionProbe> {

    private static final long serialVersionUID = 1L;

    private long tokensToConsume;
    private boolean bucketStateModified = false;

    public static SerializationHandle<TryConsumeAndReturnRemainingTokensCommand> SERIALIZATION_HANDLE = new SerializationHandle<TryConsumeAndReturnRemainingTokensCommand>() {
        @Override
        public <S> TryConsumeAndReturnRemainingTokensCommand deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            long tokensToConsume = adapter.readLong(input);

            return new TryConsumeAndReturnRemainingTokensCommand(tokensToConsume);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, TryConsumeAndReturnRemainingTokensCommand command) throws IOException {
            adapter.writeLong(output, command.tokensToConsume);
        }

        @Override
        public int getTypeId() {
            return 12;
        }

        @Override
        public Class<TryConsumeAndReturnRemainingTokensCommand> getSerializedType() {
            return TryConsumeAndReturnRemainingTokensCommand.class;
        }

    };

    public TryConsumeAndReturnRemainingTokensCommand(long tokensToConsume) {
        this.tokensToConsume = tokensToConsume;
    }

    @Override
    public ConsumptionProbe execute(GridBucketState state, long currentTimeNanos) {
        state.refillAllBandwidth(currentTimeNanos);
        long availableToConsume = state.getAvailableTokens();
        if (tokensToConsume <= availableToConsume) {
            state.consume(tokensToConsume);
            bucketStateModified = true;
            return ConsumptionProbe.consumed(availableToConsume - tokensToConsume);
        } else {
            long nanosToWaitForRefill = state.calculateDelayNanosAfterWillBePossibleToConsume(tokensToConsume, currentTimeNanos);
            return ConsumptionProbe.rejected(availableToConsume, nanosToWaitForRefill);
        }
    }

    @Override
    public boolean isBucketStateModified() {
        return bucketStateModified;
    }

    public long getTokensToConsume() {
        return tokensToConsume;
    }

}
