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

package io.github.bucket4j.annotation;

import java.lang.instrument.Instrumentation;

public class Bucket4jAnnotationAgent {

    public static void premain(String arguments, Instrumentation instrumentation) {
        // TODO
//        new AgentBuilder.Default()
//                .type(ElementMatchers.nameEndsWith("Timed"))
//                .transform((builder, type, classLoader, module) ->
//                        builder.method(ElementMatchers.any())
//                                .intercept(MethodDelegation.to(TimingInterceptor.class))
//                ).installOn(instrumentation);
    }

    public static void agentmain(String agentArgs, Instrumentation instrumentation) {
        // TODO
    }

    public static void install() {
        // TODO
    }

}
