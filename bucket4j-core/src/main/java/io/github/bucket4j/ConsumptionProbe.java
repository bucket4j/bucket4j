package io.github.bucket4j;

import io.github.bucket4j.serialization.DeserializationAdapter;
import io.github.bucket4j.serialization.SerializationAdapter;
import io.github.bucket4j.serialization.SerializationHandle;
import java.io.IOException;
import java.io.Serializable;

/**
 * Describes tokens consumed, tokens remaining, time required for token regeneration to occur, and
 * the current bucket configuration after consumption.
 *
 * @see Bucket#tryConsumeAndReturnRemaining(long)
 * @see AsyncBucket#tryConsumeAndReturnRemaining(long)
 */
public class ConsumptionProbe implements Serializable {

    private static final long serialVersionUID = 42L;

    private final boolean consumed;
    private final long remainingTokens;
    private final long nanosToWaitForRefill;

    public static SerializationHandle<ConsumptionProbe> SERIALIZATION_HANDLE = new SerializationHandle<ConsumptionProbe>() {
        @Override
        public <S> ConsumptionProbe deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            boolean consumed = adapter.readBoolean(input);
            long remainingTokens = adapter.readLong(input);
            long nanosToWaitForRefill = adapter.readLong(input);

            return new ConsumptionProbe(consumed, remainingTokens, nanosToWaitForRefill);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, ConsumptionProbe probe) throws IOException {
            adapter.writeBoolean(output, probe.consumed);
            adapter.writeLong(output, probe.remainingTokens);
            adapter.writeLong(output, probe.nanosToWaitForRefill);
        }

        @Override
        public int getTypeId() {
            return 15;
        }

        @Override
        public Class<ConsumptionProbe> getSerializedType() {
            return ConsumptionProbe.class;
        }

    };

    public static ConsumptionProbe consumed(long remainingTokens) {
        return new ConsumptionProbe(true, remainingTokens, 0);
    }

    public static ConsumptionProbe rejected(long remainingTokens, long nanosToWaitForRefill) {
        return new ConsumptionProbe(false, remainingTokens, nanosToWaitForRefill);
    }

    private ConsumptionProbe(boolean consumed, long remainingTokens, long nanosToWaitForRefill) {
        this.consumed = consumed;
        this.remainingTokens = Math.max(0L, remainingTokens);
        this.nanosToWaitForRefill = nanosToWaitForRefill;
    }

    /**
     * Flag describes result of consumption operation.
     *
     * @return true if tokens was consumed
     */
    public boolean isConsumed() {
        return consumed;
    }

    /**
     * Return the tokens remaining in the bucket
     *
     * @return the tokens remaining in the bucket
     */
    public long getRemainingTokens() {
        return remainingTokens;
    }

    /**
     * Returns zero if {@link #isConsumed()} returns true, else time in nanos which need to wait until requested amount of tokens will be refilled
     *
     * @return Zero if {@link #isConsumed()} returns true, else time in nanos which need to wait until requested amount of tokens will be refilled
     */
    public long getNanosToWaitForRefill() {
        return nanosToWaitForRefill;
    }

    @Override
    public String toString() {
        return "ConsumptionResult{" +
                "consumed=" + consumed +
                ", remainingTokens=" + remainingTokens +
                ", nanosToWaitForRefill=" + nanosToWaitForRefill +
                '}';
    }

}
