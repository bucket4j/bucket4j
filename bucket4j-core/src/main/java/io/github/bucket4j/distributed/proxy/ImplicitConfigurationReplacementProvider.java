package io.github.bucket4j.distributed.proxy;

import java.util.Optional;

/**
 * Provider for {@link ImplicitConfigurationReplacement}
 *
 * @param <K> type of key
 */
public interface ImplicitConfigurationReplacementProvider<K> {

    ImplicitConfigurationReplacementProvider<?> DEFAULT = bucketKey -> Optional.empty();

    /**
     * Provides listener for bucket.
     *
     * <p>
     * This method is called by {@link ProxyManager} each time when TODO.
     *
     * @param bucketKey the bucketKey
     *
     * @return the optional description of implicit-configuration-replacement for bucket with specified bucketKey
     */
    Optional<ImplicitConfigurationReplacement> get(K bucketKey);

}
