package io.github.bucket4j.distributed.remote.commands;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.BucketState;
import io.github.bucket4j.MathType;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.MutableBucketEntry;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.remote.RemoteStat;
import io.github.bucket4j.distributed.remote.RemoteVerboseResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("VerboseCommand Specification")
class VerboseCommandTest {

    private final BucketConfiguration configuration = new BucketConfiguration(List.of(
        Bandwidth.simple(10, Duration.ofSeconds(42))
    ));

    @Test
    void executeShouldReturnACopyOfTheStateThatIsNotAffectedByLaterMutationsOfTheEntry() {
        long currentTimeNanos = System.nanoTime();
        BucketState state = BucketState.createInitialState(configuration, MathType.INTEGER_64_BITS, currentTimeNanos);
        MutableBucketEntry entry = new MutableBucketEntry(new RemoteBucketState(state, new RemoteStat(0), null));

        VerboseCommand<Boolean> command = VerboseCommand.from(TryConsumeCommand.create(3));
        CommandResult<RemoteVerboseResult<Boolean>> result = command.execute(entry, currentTimeNanos);
        RemoteVerboseResult<Boolean> verboseResult = result.getData();

        // the captured state must not be the same mutable instance as the one held by the entry,
        // because other commands can be batched against the same entry after this one
        assertThat(verboseResult.getState()).isNotSameAs(entry.get());
        assertThat(verboseResult.getValue()).isTrue();
        assertThat(verboseResult.getState().getAvailableTokens()).isEqualTo(7);

        // simulate a subsequent command executed against the same entry within the same batch
        entry.get().consume(7);

        assertThat(entry.get().getAvailableTokens()).isEqualTo(0);
        assertThat(verboseResult.getState().getAvailableTokens()).isEqualTo(7);
    }

    @Test
    void executeShouldReturnBucketNotFoundWhenEntryDoesNotExist() {
        MutableBucketEntry entry = new MutableBucketEntry((RemoteBucketState) null);

        VerboseCommand<Boolean> command = VerboseCommand.from(TryConsumeCommand.create(3));
        CommandResult<RemoteVerboseResult<Boolean>> result = command.execute(entry, System.nanoTime());

        assertThat(result.isBucketNotFound()).isTrue();
    }

}
