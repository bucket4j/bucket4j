
package compatibility_test;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;


public class LuaScriptIsolationTest {

    private static final String KEY = "42";

    private static final String incrementScript = readScript("/increment-counter.lua");

    @Rule
    public GenericContainer redis = new GenericContainer("redis:3.0.6")
            .withExposedPorts(6379);

    private RedisCommands<String, String> commands;

    @Before
    public void init() {
        RedisURI url = RedisURI.Builder
                .redis(redis.getContainerIpAddress(), redis.getMappedPort(6379))
                .build();

        RedisClient client = RedisClient.create(url);
        StatefulRedisConnection<String, String> connection = client.connect();

        this.commands = connection.sync();
    }

    @Test
    public void test() throws InterruptedException {
        int threadCount = 4;
        int invocationsPerThread = 2500;

        CountDownLatch startLatch = new CountDownLatch(threadCount);
        Thread threads[] = new Thread[threadCount];

        for (int t = 0; t < threadCount; t++) {
            threads[t] = new Thread(() -> {
                startLatch.countDown();
                try {
                    startLatch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                for (int i = 0; i < invocationsPerThread; i++) {
                    commands.eval(incrementScript, ScriptOutputType.INTEGER, KEY);
                }
            });
            threads[t].start();
        }
        for (Thread thread : threads) {
            thread.join();
        }

        long result = commands.eval(incrementScript, ScriptOutputType.INTEGER, KEY);
        System.err.println(result);
        assertEquals(result, threadCount * invocationsPerThread + 1);
    }

    private static String readScript(String scriptResource) {
        String file = LuaScriptIsolationTest.class.getResource(scriptResource).getFile();
        byte[] scriptBytes;
        try {
            scriptBytes = Files.readAllBytes(new File(file).toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new String(scriptBytes, StandardCharsets.UTF_8);
    }

}
