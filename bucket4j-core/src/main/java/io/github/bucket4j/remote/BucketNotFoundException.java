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

package io.github.bucket4j.remote;

/**
 * Exception which thrown each time when {@link BucketProxy} found that bucket state has been lost,
 * and {@link BucketProxy} is unable to repair bucket state or recovery strategy is {@link RecoveryStrategy#THROW_BUCKET_NOT_FOUND_EXCEPTION}.
 */
public class BucketNotFoundException extends IllegalStateException {

    private static final long serialVersionUID = 42;

    private final Object bucketId;

    public BucketNotFoundException(Object bucketId) {
        super(createErrorMessage(bucketId));
        this.bucketId = bucketId;
    }

    private static String createErrorMessage(Object bucketId) {
        return "Bucket with key [" + bucketId + "] does not exist";
    }

    public Object getBucketId() {
        return bucketId;
    }

}
