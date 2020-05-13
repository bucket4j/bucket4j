package io.github.bucket4j.grid.hazelcast;

import com.hazelcast.map.EntryBackupProcessor;
import io.github.bucket4j.util.ComparableByContent;

import java.util.Arrays;
import java.util.Map;

public class SimpleBackupProcessor<K> implements EntryBackupProcessor<K, byte[]>, ComparableByContent<SimpleBackupProcessor> {

    private static final long serialVersionUID = 1L;

    private final byte[] state;

    public SimpleBackupProcessor(byte[] state) {
        this.state = state;
    }

    @Override
    public void processBackup(Map.Entry<K, byte[]> entry) {
        entry.setValue(state);
    }

    public byte[] getState() {
        return state;
    }

    @Override
    public boolean equalsByContent(SimpleBackupProcessor other) {
        return Arrays.equals(state, other.state);
    }

}
