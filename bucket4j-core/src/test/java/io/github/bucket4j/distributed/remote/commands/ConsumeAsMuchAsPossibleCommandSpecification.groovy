package io.github.bucket4j.distributed.remote.commands

import io.github.bucket4j.distributed.remote.CommandResult
import spock.lang.Specification
import spock.lang.Unroll

class ConsumeAsMuchAsPossibleCommandSpecification extends Specification {

    @Unroll
    def "#n test unwrapOneResult"(int n, int consumedTokens, int indice, CommandResult expectedResult) {
        expect:
            ConsumeAsMuchAsPossibleCommand command = new ConsumeAsMuchAsPossibleCommand(Long.MAX_VALUE)
            assert expectedResult == command.unwrapOneResult(consumedTokens, indice)
        where:
        [n, consumedTokens, indice, expectedResult] << [
                [1, 0, 0, CommandResult.FALSE],
                [2, 0, 1, CommandResult.FALSE],
                [3, 1, 0, CommandResult.TRUE],
                [4, 1, 1, CommandResult.FALSE],
                [5, 2, 1, CommandResult.TRUE],
        ]
    }
}
