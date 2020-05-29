package io.github.bucket4j.grid.hazelcast;

import com.hazelcast.map.EntryProcessor;
import io.github.bucket4j.util.ComparableByContent;

import java.util.Arrays;
import java.util.Map;

public class SimpleBackupProcessor<K> implements EntryProcessor<K, byte[], byte[]>, ComparableByContent<SimpleBackupProcessor> {

    private static final long serialVersionUID = 1L;

    private final byte[] state;

    public SimpleBackupProcessor(byte[] state) {
        this.state = state;
    }

    public byte[] getState() {
        return state;
    }

    @Override
    public boolean equalsByContent(SimpleBackupProcessor other) {
        return Arrays.equals(state, other.state);
    }

    @Override
    public byte[] process(Map.Entry<K, byte[]> entry) {
        entry.setValue(state);
        return null; // return value from backup processor is ignored, see https://github.com/hazelcast/hazelcast/pull/14995
    }

}
