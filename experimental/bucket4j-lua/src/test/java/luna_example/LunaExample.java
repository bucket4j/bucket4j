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

package luna_example;

import org.classdump.luna.StateContext;
import org.classdump.luna.Table;
import org.classdump.luna.Variable;
import org.classdump.luna.compiler.CompilerChunkLoader;
import org.classdump.luna.env.RuntimeEnvironment;
import org.classdump.luna.env.RuntimeEnvironments;
import org.classdump.luna.exec.CallException;
import org.classdump.luna.exec.CallPausedException;
import org.classdump.luna.exec.DirectCallExecutor;
import org.classdump.luna.impl.DefaultTable;
import org.classdump.luna.impl.NonsuspendableFunctionException;
import org.classdump.luna.impl.StateContexts;
import org.classdump.luna.lib.StandardLibrary;
import org.classdump.luna.load.ChunkLoader;
import org.classdump.luna.load.LoaderException;
import org.classdump.luna.runtime.AbstractFunction0;
import org.classdump.luna.runtime.ExecutionContext;
import org.classdump.luna.runtime.LuaFunction;
import org.classdump.luna.runtime.ResolvedControlThrowable;

import java.util.Arrays;

public class LunaExample {

    static String script =
            "local something = \"42\"" +
            "local Scope = {};                          \n" +
            "Scope.my_function = function(t)            \n" +
            "    local r = \"\";                        \n" +
            "    for key, value in pairs(t) do          \n" +
            "        r =  r .. key .. value;            \n" +
            "    end                                    \n" +
            "    return r .. something;                 \n" +
            "end                                        \n" +
            "return Scope;                              \n";

    public static void main(String[] args) throws LoaderException, InterruptedException, CallPausedException, CallException {

        // initialise state
        StateContext state = StateContexts.newDefaultInstance();
        RuntimeEnvironment runtime = RuntimeEnvironments.system();
        StandardLibrary standardLibrary = StandardLibrary.in(runtime);
        Table env = standardLibrary.installInto(state);

        DefaultTable table = new DefaultTable();
        table.rawset("eniki", "baniki");
        table.rawset("eli", "vareniky");
        env.rawset("my_table", table);


        // compile
        ChunkLoader loader = CompilerChunkLoader.of("hello_world");
        LuaFunction compiledChunk = loader.loadTextChunk(new Variable(env), "", script);

        // execute
        DirectCallExecutor directCallExecutor = DirectCallExecutor.newExecutor();
        Object[] result = directCallExecutor.call(state, compiledChunk);

        //Object f = env.rawget("my_function");

        System.out.println(Arrays.toString(result));
        Table scope = (Table) result[0];
        LuaFunction f = (LuaFunction) scope.rawget("my_function");

        Object[] result2 = directCallExecutor.call(state, f, table);
        System.out.println(Arrays.toString(result2));
    }


}
