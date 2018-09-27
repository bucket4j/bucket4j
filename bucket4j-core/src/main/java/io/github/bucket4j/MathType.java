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

package io.github.bucket4j;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

// TODO javadocs
public enum MathType {

    // TODO javadocs
    INTEGER_64_BITS,

    // TODO javadocs
    IEEE_754;

    public static Set<MathType> ALL = Collections.unmodifiableSet(EnumSet.allOf(MathType.class));

}
