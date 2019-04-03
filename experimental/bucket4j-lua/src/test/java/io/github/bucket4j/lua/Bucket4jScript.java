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

package io.github.bucket4j.lua;

import io.github.bucket4j.BucketConfiguration;
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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;


public class Bucket4jScript {

    private final DirectCallExecutor executor = DirectCallExecutor.newExecutor();
    private final StateContext context;
    private final Table globalScope;
    private final Table bucket4jMetaTable;

    public Bucket4jScript() {
        String program = readScript("/bucket4j.lua");
        program += "\n return Bucket4j;";

        // initialise context
        this.context = StateContexts.newDefaultInstance();
        this.globalScope = StandardLibrary.in(RuntimeEnvironments.system()).installInto(context);

        // compile
        ChunkLoader loader = CompilerChunkLoader.of("Bucket4j");
        try {
            LuaFunction bucket4jScript = loader.loadTextChunk(new Variable(globalScope), "bucket4j", program);
            Object[] result = executor.call(context, bucket4jScript);
            this.bucket4jMetaTable = (Table) result[0];
        } catch (LoaderException | CallException | CallPausedException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String readScript(String scriptResource) {
        String file = Bucket4jScript.class.getResource(scriptResource).getFile();
        byte[] scriptBytes;
        try {
            scriptBytes = Files.readAllBytes(new File(file).toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new String(scriptBytes, StandardCharsets.UTF_8);
    }

    public Table getBucket4jMetaTable() {
        return bucket4jMetaTable;
    }

    public Table createState(BucketConfiguration configuration, long currentTimeNanos) {
        // TODO
        return null;
    }

}