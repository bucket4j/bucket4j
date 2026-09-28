package io.github.bucket4j.distributed.remote.commands;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.BucketState;
import io.github.bucket4j.MathType;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.MutableBucketEntry;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.remote.RemoteStat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CreateSnapshotCommand Specification")
class CreateSnapshotCommandTest {

    private final BucketConfiguration configuration = new BucketConfiguration(List.of(
        Bandwidth.simple(10, Duration.ofSeconds(42))
    ));

    @Test
    void executeShouldReturnACopyOfTheStateThatIsNotAffectedByLaterMutationsOfTheEntry() {
        long currentTimeNanos = System.nanoTime();
        BucketState state = BucketState.createInitialState(configuration, MathType.INTEGER_64_BITS, currentTimeNanos);
        MutableBucketEntry entry = new MutableBucketEntry(new RemoteBucketState(state, new RemoteStat(0), null));

        CommandResult<RemoteBucketState> result = new CreateSnapshotCommand().execute(entry, currentTimeNanos);
        RemoteBucketState snapshot = result.getData();

        // the snapshot must not be the same mutable instance as the one held by the entry,
        // because other commands can be batched against the same entry after this one
        assertThat(snapshot).isNotSameAs(entry.get());
        assertThat(snapshot.getAvailableTokens()).isEqualTo(10);

        // simulate a subsequent command executed against the same entry within the same batch
        entry.get().consume(10);

        assertThat(entry.get().getAvailableTokens()).isEqualTo(0);
        assertThat(snapshot.getAvailableTokens()).isEqualTo(10);
    }

    @Test
    void executeShouldReturnBucketNotFoundWhenEntryDoesNotExist() {
        MutableBucketEntry entry = new MutableBucketEntry((RemoteBucketState) null);

        CommandResult<RemoteBucketState> result = new CreateSnapshotCommand().execute(entry, System.nanoTime());

        assertThat(result.isBucketNotFound()).isTrue();
    }

}
