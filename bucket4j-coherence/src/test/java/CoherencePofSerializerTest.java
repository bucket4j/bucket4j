import com.tangosol.io.Serializer;
import com.tangosol.io.pof.ConfigurablePofContext;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import io.github.bucket4j.distributed.remote.Request;
import io.github.bucket4j.distributed.remote.commands.AddTokensCommand;
import io.github.bucket4j.distributed.versioning.Versions;
import io.github.bucket4j.grid.coherence.CoherenceProcessor;
import io.github.bucket4j.util.ComparableByContent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CoherencePofSerializerTest {

    private static Serializer pofSerializer;

    @BeforeAll
    public static void initializeSerializer() {
        pofSerializer = new ConfigurablePofContext("bucket4j-pof-config-example.xml");
    }

    @Test
    public void testSerializationOfEntryProcessors() {
        AddTokensCommand command = new AddTokensCommand(42);
        Request request = new Request(command, Versions.getLatest(), null, null);
        testSerialization(new CoherenceProcessor(request));
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
