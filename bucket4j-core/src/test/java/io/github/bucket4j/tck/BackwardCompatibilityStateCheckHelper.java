package io.github.bucket4j.tck;

public interface BackwardCompatibilityStateCheckHelper<K> {

    byte[] getRawState(K key);

    void setRawState(K key, byte[] state);

}
