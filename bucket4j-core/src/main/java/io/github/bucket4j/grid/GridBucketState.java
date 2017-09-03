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

package io.github.bucket4j.grid;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.BucketState;
import io.github.bucket4j.TimeMeter;

import java.io.Serializable;

public class GridBucketState implements Serializable {

    private static final long serialVersionUID = 1L;

    private final BucketConfiguration bucketConfiguration;
    private final BucketState bucketState;

    public GridBucketState() {
        bucketConfiguration = null;
        bucketState = null;
    }

    public GridBucketState(BucketConfiguration bucketConfiguration, BucketState bucketState) {
        this.bucketConfiguration = bucketConfiguration;
        this.bucketState = bucketState;
    }

    public BucketConfiguration getBucketConfiguration() {
        return bucketConfiguration;
    }

    public BucketState getBucketState() {
        return bucketState;
    }



}
