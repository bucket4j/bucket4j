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

import java.text.MessageFormat;

public class IncompatibleConfigurationException extends IllegalArgumentException {

    private static final long serialVersionUID = 42;

    private final BucketConfiguration previousConfiguration;
    private final BucketConfiguration newConfiguration;

    public IncompatibleConfigurationException(BucketConfiguration previousConfiguration, BucketConfiguration newConfiguration) {
        super(generateMessage(previousConfiguration, newConfiguration));
        this.previousConfiguration = previousConfiguration;
        this.newConfiguration = newConfiguration;
    }

    public BucketConfiguration getNewConfiguration() {
        return newConfiguration;
    }

    public BucketConfiguration getPreviousConfiguration() {
        return previousConfiguration;
    }

    private static String generateMessage(BucketConfiguration previousConfiguration, BucketConfiguration newConfiguration) {
        String format = "New configuration {0} incompatible with previous configuration {1}";
        return MessageFormat.format(format, newConfiguration, previousConfiguration);
    }

}
