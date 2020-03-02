
package io.github.bucket4j.grid;

import io.github.bucket4j.EstimationProbe;
import io.github.bucket4j.serialization.DeserializationAdapter;
import io.github.bucket4j.serialization.SerializationAdapter;
import io.github.bucket4j.serialization.SerializationHandle;

import java.io.IOException;


public class EstimateAbilityToConsumeCommand implements GridCommand<EstimationProbe> {

    private static final long serialVersionUID = 1L;

    private long tokensToConsume;

    public static SerializationHandle<EstimateAbilityToConsumeCommand> SERIALIZATION_HANDLE = new SerializationHandle<EstimateAbilityToConsumeCommand>() {
        @Override
        public <S> EstimateAbilityToConsumeCommand deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            long tokensToConsume = adapter.readLong(input);

            return new EstimateAbilityToConsumeCommand(tokensToConsume);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, EstimateAbilityToConsumeCommand command) throws IOException {
            adapter.writeLong(output, command.tokensToConsume);
        }

        @Override
        public int getTypeId() {
            return 10;
        }

        @Override
        public Class<EstimateAbilityToConsumeCommand> getSerializedType() {
            return EstimateAbilityToConsumeCommand.class;
        }

    };

    public EstimateAbilityToConsumeCommand(long tokensToEstimate) {
        this.tokensToConsume = tokensToEstimate;
    }

    @Override
    public EstimationProbe execute(GridBucketState state, long currentTimeNanos) {
        state.refillAllBandwidth(currentTimeNanos);
        long availableToConsume = state.getAvailableTokens();
        if (tokensToConsume <= availableToConsume) {
            return EstimationProbe.canBeConsumed(availableToConsume);
        } else {
            long nanosToWaitForRefill = state.calculateDelayNanosAfterWillBePossibleToConsume(tokensToConsume, currentTimeNanos);
            return EstimationProbe.canNotBeConsumed(availableToConsume, nanosToWaitForRefill);
        }
    }

    @Override
    public boolean isBucketStateModified() {
        return false;
    }

    public long getTokensToConsume() {
        return tokensToConsume;
    }

}
