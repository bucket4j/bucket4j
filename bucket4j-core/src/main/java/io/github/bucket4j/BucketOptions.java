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

package io.github.bucket4j;

import io.github.bucket4j.MathType;

import java.util.Set;

// todo javadocs
public class BucketOptions {

    private final boolean asyncModeSupported;
    private final Set<MathType> supportedMathTypes;
    private final MathType defaultMathType;

    public BucketOptions(boolean asyncModeSupported, Set<MathType> supportedMathTypes, MathType defaultMathType) {
        this.asyncModeSupported = asyncModeSupported;
        this.supportedMathTypes = supportedMathTypes;
        this.defaultMathType = defaultMathType;
    }

    // todo javadocs
    public boolean isAsyncModeSupported() {
        return asyncModeSupported;
    }

    // todo javadocs
    public Set<MathType> getSupportedMathTypes() {
        return supportedMathTypes;
    }

    // todo javadocs
    public MathType getDefaultMathType() {
        return defaultMathType;
    }

}
