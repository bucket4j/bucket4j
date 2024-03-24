package io.github.bucket4j.distributed.jdbc;

/**
 * Used by some inheritors of {@link io.github.bucket4j.distributed.proxy.generic.pessimistic_locking.AbstractLockBasedProxyManager}
 * when it needs to calculate numeric value of locks.
 *
 * @param <K> type of key
 */
public interface LockIdSupplier<K> {

    LockIdSupplier<?> DEFAULT = (LockIdSupplier<Object>) key -> (key instanceof Number) ? ((Number) key).longValue(): key.hashCode();

    /**
     * Returns the lock-id specified with the key.
     *
     * @param key the key of bucket
     *
     * @return id of lock specified with key
     */
    long toLockId(K key);

}