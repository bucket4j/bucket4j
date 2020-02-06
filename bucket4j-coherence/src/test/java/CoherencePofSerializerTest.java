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

import com.tangosol.io.Serializer;
import com.tangosol.io.pof.ConfigurablePofContext;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import io.github.bucket4j.*;
import io.github.bucket4j.grid.AddTokensCommand;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.GridCommand;
import io.github.bucket4j.grid.coherence.CoherenceEntryProcessorAdapter;
import io.github.bucket4j.grid.jcache.ExecuteProcessor;
import io.github.bucket4j.grid.jcache.InitStateAndExecuteProcessor;
import io.github.bucket4j.grid.jcache.InitStateProcessor;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;

import static io.github.bucket4j.Bandwidth.classic;
import static io.github.bucket4j.Bandwidth.simple;
import static io.github.bucket4j.Refill.greedy;
import static io.github.bucket4j.Refill.intervallyAligned;
import static java.time.Duration.*;
import static org.junit.Assert.assertTrue;

public class CoherencePofSerializerTest {

    static {
        EqualityUtils.registerComparator(ExecuteProcessor.class, (processor1, processor2) -> {
            return EqualityUtils.equals(processor1.getTargetCommand(), processor2.getTargetCommand());
        });

        EqualityUtils.registerComparator(InitStateProcessor.class, (processor1, processor2) -> {
            return EqualityUtils.equals(processor1.getConfiguration(), processor2.getConfiguration());
        });

        EqualityUtils.registerComparator(InitStateAndExecuteProcessor.class, (processor1, processor2) -> {
            return EqualityUtils.equals(processor1.getTargetCommand(), processor2.getTargetCommand())
                    && EqualityUtils.equals(processor1.getConfiguration(), processor2.getConfiguration());
        });

        EqualityUtils.registerComparator(CoherenceEntryProcessorAdapter.class, (processor1, processor2) -> {
            return EqualityUtils.equals(processor1.getEntryProcessor(), processor2.getEntryProcessor());
        });
    }

    private static Serializer pofSerializer;

    @BeforeClass
    public static void initializeSerializer() {
        pofSerializer = new ConfigurablePofContext("bucket4j-pof-config-example.xml");
    }

    @Test
    public void testSerializationOfEntryProcessors() {
        BucketConfiguration configuration = Bucket4j.configurationBuilder()
                .addLimit(simple(10, ofSeconds(1)))
                .build();
        GridCommand command = new AddTokensCommand(42);

        testSerialization(new CoherenceEntryProcessorAdapter<>(new ExecuteProcessor<>(command)));
    }

    @Test
    public void testSerializationOfBucketState() throws IOException {
        Bandwidth[] bandwidths = new Bandwidth[] {
                simple(10, ofSeconds(42)),
                classic(20, greedy(300, ofHours(2))),
                classic(400, intervallyAligned(1000, ofDays(2), Instant.now(), false))
        };
        BucketConfiguration bucketConfiguration = new BucketConfiguration(Arrays.asList(bandwidths));
        BucketState bucketState = new BucketState(bucketConfiguration, System.nanoTime());

        bucketState.addTokens(bandwidths, 42);
        GridBucketState gridBucketState = new GridBucketState(bucketConfiguration, bucketState);

        testSerialization(gridBucketState);
    }

    private void testSerialization(Object object) {
        Object object2 = serializeAndDeserialize(object);
        assertTrue(EqualityUtils.equals(object, object2));
    }

    private <T> T serializeAndDeserialize(T object) {
        Binary binaryObject = ExternalizableHelper.toBinary(object, pofSerializer);
        return ExternalizableHelper.fromBinary(binaryObject, pofSerializer);
    }

}
