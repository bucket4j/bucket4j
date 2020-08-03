package io.github.bucket4j.distributed.versioning;

public class BackwardCompatibilityException extends RuntimeException {

    public BackwardCompatibilityException(String message) {
        super(message);
    }

}
