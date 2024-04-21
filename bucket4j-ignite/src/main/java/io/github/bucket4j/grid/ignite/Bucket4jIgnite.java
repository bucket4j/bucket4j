/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2024 Vladimir Bukhtoyarov
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
package io.github.bucket4j.grid.ignite;

import io.github.bucket4j.grid.ignite.thick.Bucket4jIgniteThick;
import io.github.bucket4j.grid.ignite.thin.Bucket4jIgniteThin;

/**
 * Entry point for Apache Ignite integration
 */
public class Bucket4jIgnite {

    /**
     * @return entry points for Ignite integration based on top of Thick client.
     */
    public static Bucket4jIgniteThick thick() {
        return Bucket4jIgniteThick.INSTANCE;
    }

    /**
     * @return entry points for Ignite integration based on top of Thin client.
     */
    public static Bucket4jIgniteThin thin() {
        return Bucket4jIgniteThin.INSTANCE;
    }

}
