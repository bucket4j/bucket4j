package io.github.bucket4j.distributed.remote.commands;

import io.github.bucket4j.distributed.remote.CommandResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ConsumeAsMuchAsPossibleCommand Specification")
class ConsumeAsMuchAsPossibleCommandTest {

    private record UnwrapOneResultCase(int n, int consumedTokens, int indice, CommandResult expectedResult) {

        @Override
        public String toString() {
            return "#" + n;
        }
    }

    static Stream<UnwrapOneResultCase> unwrapOneResultCases() {
        return Stream.of(
            new UnwrapOneResultCase(1, 0, 0, CommandResult.FALSE),
            new UnwrapOneResultCase(2, 0, 1, CommandResult.FALSE),
            new UnwrapOneResultCase(3, 1, 0, CommandResult.TRUE),
            new UnwrapOneResultCase(4, 1, 1, CommandResult.FALSE),
            new UnwrapOneResultCase(5, 2, 1, CommandResult.TRUE)
        );
    }

    @ParameterizedTest
    @MethodSource("unwrapOneResultCases")
    void testUnwrapOneResult(UnwrapOneResultCase testCase) throws Exception {
        ConsumeAsMuchAsPossibleCommand command = new ConsumeAsMuchAsPossibleCommand(Long.MAX_VALUE);
        assertThat(command.unwrapOneResult((long) testCase.consumedTokens(), testCase.indice())).isEqualTo(testCase.expectedResult());
    }

}
