
package io.github.bucket4j.distributed.remote;

/**
 * TODO javadocs
 */
public interface MutableBucketEntry {

    boolean exists();

    void set(RemoteBucketState state);

    RemoteBucketState get();

}
