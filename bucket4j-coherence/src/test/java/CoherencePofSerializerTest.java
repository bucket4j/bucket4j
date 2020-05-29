
import com.tangosol.io.Serializer;
import com.tangosol.io.pof.ConfigurablePofContext;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import io.github.bucket4j.*;
import io.github.bucket4j.distributed.remote.commands.AddTokensCommand;
import io.github.bucket4j.grid.coherence.CoherenceProcessor;
import io.github.bucket4j.util.ComparableByContent;
import org.junit.BeforeClass;
import org.junit.Test;

import static io.github.bucket4j.Bandwidth.simple;
import static java.time.Duration.*;
import static org.junit.Assert.assertTrue;

public class CoherencePofSerializerTest {

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
        AddTokensCommand command = new AddTokensCommand(42);

        testSerialization(new CoherenceProcessor(command));
    }

    private void testSerialization(Object object) {
        Object object2 = serializeAndDeserialize(object);
        assertTrue(ComparableByContent.equals(object, object2));
    }

    private <T> T serializeAndDeserialize(T object) {
        Binary binaryObject = ExternalizableHelper.toBinary(object, pofSerializer);
        return ExternalizableHelper.fromBinary(binaryObject, pofSerializer);
    }

}
