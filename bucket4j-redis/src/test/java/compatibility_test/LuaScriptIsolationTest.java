/*
 *
 * Copyright 2015-2018 Vladimir Bukhtoyarov
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

package compatibility_test;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
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
    public GenericContainer redis = new GenericContainer("redis:4.0.11")
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
        RedisAsyncCommands<String, String> p = connection.async();
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
