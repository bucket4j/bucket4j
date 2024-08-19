package io.github.bucket4j.distributed.proxy.synchronization.batch.mock;

import java.util.concurrent.CountDownLatch;

public class SingleMockCommand implements MockCommand<Long> {

    private final long amount;
    private final RuntimeException error;
    private final boolean useSignaling;

    public final CountDownLatch arriveSignal = new CountDownLatch(1);
    public final CountDownLatch executePermit = new CountDownLatch(1);

    public SingleMockCommand(long amount) {
        this.amount = amount;
        this.error = null;
        this.useSignaling = false;
    }

    public SingleMockCommand(RuntimeException error) {
        this.amount = 0;
        this.error = error;
        this.useSignaling = false;
    }

    public SingleMockCommand(long amount, boolean useSignaling) {
        this.amount = amount;
        this.error = null;
        this.useSignaling = useSignaling;
    }

    public SingleMockCommand(RuntimeException error, boolean useSignaling) {
        this.amount = 0;
        this.error = error;
        this.useSignaling = useSignaling;
    }

    @Override
    public Long apply(MockState state) {
        if (!useSignaling) {
            return applyImpl(state);
        } else {
            String previousName = Thread.currentThread().getName();
            Thread.currentThread().setName(previousName + "-SingleMockCommand-" + amount);
            try {
                return applyImpl(state);
            } finally {
                Thread.currentThread().setName(previousName);
            }
        }
    }

    private Long applyImpl(MockState state) {
        if (useSignaling) {
            arriveSignal.countDown();
            try {
                executePermit.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        if (error != null) {
            throw error;
        }
        state.setSum(state.getSum() + amount);
        return state.getSum();
    }

}
