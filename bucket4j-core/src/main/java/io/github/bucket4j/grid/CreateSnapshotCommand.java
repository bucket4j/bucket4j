
package io.github.bucket4j.grid;

import io.github.bucket4j.BucketState;
import io.github.bucket4j.serialization.DeserializationAdapter;
import io.github.bucket4j.serialization.SerializationAdapter;
import io.github.bucket4j.serialization.SerializationHandle;

import java.io.IOException;

public class CreateSnapshotCommand implements GridCommand<BucketState> {

    private static final long serialVersionUID = 1L;

    public static SerializationHandle<CreateSnapshotCommand> SERIALIZATION_HANDLE = new SerializationHandle<CreateSnapshotCommand>() {
        @Override
        public <S> CreateSnapshotCommand deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            return new CreateSnapshotCommand();
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, CreateSnapshotCommand command) throws IOException {
            // do nothing
        }

        @Override
        public int getTypeId() {
            return 8;
        }

        @Override
        public Class<CreateSnapshotCommand> getSerializedType() {
            return CreateSnapshotCommand.class;
        }

    };

    @Override
    public BucketState execute(GridBucketState gridState, long currentTimeNanos) {
        return gridState.copyBucketState();
    }

    @Override
    public boolean isBucketStateModified() {
        return false;
    }

}
