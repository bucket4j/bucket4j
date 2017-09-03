package io.github.bucket4j.grid.hazelcast;

import com.hazelcast.map.EntryBackupProcessor;
import com.hazelcast.map.EntryProcessor;
import io.github.bucket4j.grid.CommandResult;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.jcache.JCacheEntryProcessor;

import java.io.Serializable;
import java.util.Map;


class HazelcastEntryProcessorAdapter<K extends Serializable, T extends Serializable> implements EntryProcessor<K, GridBucketState> {

    private static final long serialVersionUID = 1L;

    private final JCacheEntryProcessor<K, T> entryProcessor;
    private EntryBackupProcessor<K, GridBucketState> backupProcessor;

    public HazelcastEntryProcessorAdapter(JCacheEntryProcessor<K, T> entryProcessor) {
        this.entryProcessor = entryProcessor;
    }

    @Override
    public Object process(Map.Entry<K, GridBucketState> entry) {
        HazelcastMutableEntryAdapter<K> entryAdapter = new HazelcastMutableEntryAdapter<>(entry);
        CommandResult<T> result = entryProcessor.process(entryAdapter);
        if (entryAdapter.isModified()) {
            GridBucketState state = entry.getValue();
            backupProcessor = new SimpleBackupProcessor<>(state);
        }
        return result;
    }

    @Override
    public EntryBackupProcessor<K, GridBucketState> getBackupProcessor() {
        return backupProcessor;
    }

}
