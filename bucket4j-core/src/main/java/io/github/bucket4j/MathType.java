/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2020 Vladimir Bukhtoyarov
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */

package io.github.bucket4j;

@Experimental
public enum MathType {

    /**
     * The default math precision that uses integer arithmetic with 64 bits numbers.
     */
    INTEGER_64_BITS,

    /**
     * Experimental math precision that uses IEEE-754 arithmetic.
     *
     * <p>
     * <b>Warning: </b> you should not use this precision in production, because intention of this precision is the testing purpose for backends written in Lua or JS,
     * in other words for testing backends that do not provide 64-bit integer arithmetic.
     */
    @Experimental
    IEEE_754;

}
