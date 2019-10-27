package io.github.bucket4j.distributed.proxy.optimizers.batch;

public class BatchFailedException extends IllegalStateException {

    public BatchFailedException(Throwable cause) {
        super(cause);
    }

}
