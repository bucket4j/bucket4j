package io.github.bucket4j.postgresql;

/**
 * @author Maxim Bartkov
 * Strategy of transaction for the table, which uses as a Buckets storage
 */
public enum LockBasedTransactionType {

    /**
     * Based on pg_advisory_xact_lock
     * locks an application-defined resource, which can be identified either by a single 64-bit key value or two 32-bit key values (note that these two key spaces do not overlap).
     * If another session already holds a lock on the same resource identifier, this function will wait until the resource becomes available.
     * The lock is exclusive.
     * Multiple lock requests stack so that if the same resource is locked three times it must then be unlocked three times to be released for other sessions use.
     * The lock is automatically released at the end of the current transaction and cannot be released explicitly.
     */
    ADVISORY,

    /**
     * Based on Select For Update
     * This prevents them from being modified or deleted by other transactions until the current transaction ends.
     * That is, other transactions that attempt UPDATE, DELETE, or SELECT FOR UPDATE of these rows will be blocked until the current transaction ends.
     * Also, if an UPDATE, DELETE, or SELECT FOR UPDATE from another transaction has already locked a selected row or rows, SELECT FOR UPDATE will wait for the other transaction to complete, and will then lock and return the updated row (or no row, if the row was deleted).
     * Within a SERIALIZABLE transaction, however, an error will be thrown if a row to be locked has changed since the transaction started.
     */
    SELECT_FOR_UPDATE;

}
