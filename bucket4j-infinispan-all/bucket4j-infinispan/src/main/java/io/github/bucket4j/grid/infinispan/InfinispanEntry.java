package io.github.bucket4j.grid.infinispan;

import io.github.bucket4j.distributed.remote.MutableBucketEntry;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import org.infinispan.functional.EntryView;

import static io.github.bucket4j.serialization.InternalSerializationHelper.deserializeState;
import static io.github.bucket4j.serialization.InternalSerializationHelper.serializeState;

public class InfinispanEntry<K> implements MutableBucketEntry {

    private final EntryView.ReadWriteEntryView<K, byte[]> entryView;

    public InfinispanEntry(EntryView.ReadWriteEntryView<K, byte[]> entryView) {
        this.entryView = entryView;
    }

    @Override
    public RemoteBucketState get() {
        byte[] stateBytes = entryView.get();
        return deserializeState(stateBytes);
    }

    @Override
    public boolean exists() {
        return entryView.find().isPresent();
    }

    @Override
    public void set(RemoteBucketState value) {
        byte[] stateBytes = serializeState(value);
        entryView.set(stateBytes);
    }

}
