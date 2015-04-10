package com.github.bandwidthlimiter.bucket.grid;

import com.github.bandwidthlimiter.bucket.AbstractBucket;
import com.github.bandwidthlimiter.bucket.BucketConfiguration;
import com.github.bandwidthlimiter.bucket.BucketState;

import java.io.Serializable;

public abstract class AbstractGridBucket extends AbstractBucket {

    protected AbstractGridBucket(BucketConfiguration configuration) {
        super(configuration);
    }

    @Override
    protected long consumeAsMuchAsPossibleImpl(long limit) {
        return execute(new ConsumeAsMuchAsPossibleCommand(limit));
    }

    @Override
    protected boolean tryConsumeImpl(long tokensToConsume) {
        return execute(new TryConsumeCommand(tokensToConsume));
    }

    @Override
    protected boolean consumeOrAwaitImpl(long tokensToConsume, long waitIfBusyTimeLimit) throws InterruptedException {
        final boolean isWaitingLimited = waitIfBusyTimeLimit > 0;
        final ConsumeOrCalculateTimeToCloseDeficitCommand consumeCommand = new ConsumeOrCalculateTimeToCloseDeficitCommand(tokensToConsume);
        final long methodStartTime = isWaitingLimited? configuration.getTimeMeter().currentTime(): 0;

        while (true) {
            long timeToCloseDeficit = execute(consumeCommand);
            if (timeToCloseDeficit == 0) {
                return true;
            }

            if (isWaitingLimited) {
                long currentTime = configuration.getTimeMeter().currentTime();
                long methodDuration = currentTime - methodStartTime;
                if (methodDuration >= waitIfBusyTimeLimit) {
                    return false;
                }
                long sleepingTimeLimit = waitIfBusyTimeLimit - methodDuration;
                if (timeToCloseDeficit >= sleepingTimeLimit) {
                    return false;
                }
            }
            configuration.getTimeMeter().sleep(timeToCloseDeficit);
        }
    }

    @Override
    public BucketState createSnapshot() {
        long[] snapshotBytes = execute(new CreateSnapshotCommand());
        return new BucketState(snapshotBytes);
    }

    protected abstract <T extends Serializable> T execute(GridCommand<T> command);

}
