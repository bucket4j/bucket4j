/*
 *
 * Copyright 2015-2020 Vladimir Bukhtoyarov
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package io.github.bucket4j.experimental;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlinx.lincheck.LinChecker;
import org.jetbrains.kotlinx.lincheck.LoggingLevel;
import org.jetbrains.kotlinx.lincheck.Options;
import org.jetbrains.kotlinx.lincheck.annotations.Operation;
import org.jetbrains.kotlinx.lincheck.annotations.Param;
import org.jetbrains.kotlinx.lincheck.paramgen.LongGen;
import org.jetbrains.kotlinx.lincheck.strategy.stress.StressCTest;
import org.jetbrains.kotlinx.lincheck.strategy.stress.StressOptions;
import org.jetbrains.kotlinx.lincheck.verifier.VerifierState;
import org.jetbrains.kotlinx.lincheck.verifier.linearizability.LinearizabilityVerifier;
import org.junit.Test;

@StressCTest(verifier = LinearizabilityVerifier.class)
@Param(name = "amount", gen = LongGen.class, conf = "1:20")
public class SomeLincheckTest extends VerifierState {

    private WrongImplementedCounter counter = new WrongImplementedCounter();

    @Operation
    public long add(@Param(name = "amount") long amount) {
        return counter.add(amount);
    }

    @Test
    public void runTest() {
        Options opts = new StressOptions()
                .iterations(10)
                .threads(3)
                .minimizeFailedScenario(false)
                .logLevel(LoggingLevel.INFO)
        ;
        LinChecker.check(SomeLincheckTest.class, opts);
    }

    @NotNull
    @Override
    protected Object extractState() {
        return counter;
    }

}
