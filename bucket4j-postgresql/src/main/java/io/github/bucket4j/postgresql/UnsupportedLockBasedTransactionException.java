package io.github.bucket4j.postgresql;

import java.text.MessageFormat;

public class UnsupportedLockBasedTransactionException extends RuntimeException {

    public UnsupportedLockBasedTransactionException(LockBasedTransactionType type) {
        super(MessageFormat.format("The type of lock-based transaction {0} is unsupported", type.toString()));
    }
}
