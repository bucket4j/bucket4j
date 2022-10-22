package io.github.bucket4j.redis.consts;

/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2022 Vladimir Bukhtoyarov
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
 *
 * Author: ray.chang
 * Date: 2022-10-20
 *
 *
 */
public class LunaScripts {

    public final static String SCRIPT_SET_NX_PX =
            "if redis.call('set', KEYS[1], ARGV[1], 'nx', 'px', ARGV[2]) then " +
                "return 1; " +
                "else " +
                "return 0; " +
                "end";
    public final static String SCRIPT_SET_NX =
            "if redis.call('set', KEYS[1], ARGV[1], 'nx') then " +
                "return 1; " +
                "else " +
                "return 0; " +
                "end";

    public final static String SCRIPT_COMPARE_AND_SWAP_PX =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "redis.call('psetex', KEYS[1], ARGV[3], ARGV[2]); " +
                    "return 1; " +
                    "else " +
                    "return 0; " +
                    "end";

    public final static String SCRIPT_COMPARE_AND_SWAP = (
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "redis.call('set', KEYS[1], ARGV[2]); " +
                    "return 1; " +
                    "else " +
                    "return 0; " +
                    "end");

}
