/*
 *
 *   Copyright 2015-2017 Vladimir Bukhtoyarov
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.github.bucket4j.redis;

import compatibility_test.LuaScriptIsolationTest;
import org.classdump.luna.StateContext;
import org.classdump.luna.Table;
import org.classdump.luna.Variable;
import org.classdump.luna.compiler.CompilerChunkLoader;
import org.classdump.luna.env.RuntimeEnvironments;
import org.classdump.luna.exec.CallException;
import org.classdump.luna.exec.CallPausedException;
import org.classdump.luna.exec.DirectCallExecutor;
import org.classdump.luna.impl.StateContexts;
import org.classdump.luna.lib.StandardLibrary;
import org.classdump.luna.load.ChunkLoader;
import org.classdump.luna.load.LoaderException;
import org.classdump.luna.runtime.LuaFunction;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;


import static org.junit.Assert.assertEquals;

public class LuaTestBase {

    protected LuaFunction bucket4jScript;
    protected StateContext context;
    protected Table environment;

    @Before
    public void initLuaEnvironment() throws LoaderException {
        String program = readScript("/bucket4j.lua");

        // initialise context
        this.context = StateContexts.newDefaultInstance();
        this.environment = StandardLibrary.in(RuntimeEnvironments.system()).installInto(context);

        // compile
        ChunkLoader loader = CompilerChunkLoader.of("Bucket4j");
        bucket4jScript = loader.loadTextChunk(new Variable(environment), "bucket4j", program);
    }

    @Test
    public void testThatScriptIsExecutable() throws InterruptedException, CallPausedException, CallException {
        Object[] result = DirectCallExecutor.newExecutor().call(context, bucket4jScript);
        assertEquals("Main script should not return any value", 0, result.length);
    }

    public static String readScript(String scriptResource) {
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