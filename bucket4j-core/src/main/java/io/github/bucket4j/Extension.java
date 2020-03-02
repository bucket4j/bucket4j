
package io.github.bucket4j;

import io.github.bucket4j.serialization.SerializationHandle;

import java.util.Collection;
import java.util.Collections;

/**
 * Represents an extension point of bucket4j library.
 *
 * @param <T> type of builder for buckets
 */
public interface Extension<T extends AbstractBucketBuilder<T>> {

    /**
     * Creates new instance of builder specific for this particular extension.
     *
     * @return new builder instance
     */
    T builder();


    /**
     * @return serializers
     */
    default Collection<SerializationHandle<?>> getSerializers() {
        return Collections.emptyList();
    }

}
