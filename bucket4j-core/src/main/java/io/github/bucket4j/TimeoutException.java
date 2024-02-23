package io.github.bucket4j;

public class TimeoutException extends RuntimeException {

    private final long nanosElapsed;
    private final long requestTimeoutNanos;

    public TimeoutException(String message, long nanosElapsed, long requestTimeoutNanos) {
        super(message);
        this.nanosElapsed = nanosElapsed;
        this.requestTimeoutNanos = requestTimeoutNanos;
    }

    public long getNanosElapsed() {
        return nanosElapsed;
    }

    public long getRequestTimeoutNanos() {
        return requestTimeoutNanos;
    }

}
