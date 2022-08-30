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
package io.github.bucket4j.distributed.versioning;

import java.text.MessageFormat;

public class UsageOfObsoleteApiException extends BackwardCompatibilityException {

    private final int requestedFormatNumber;
    private final int minSupportedFormatNumber;

    public UsageOfObsoleteApiException(int requestedFormatNumber, int minSupportedFormatNumber) {
        super(formatMessage(requestedFormatNumber, minSupportedFormatNumber));
        this.requestedFormatNumber = requestedFormatNumber;
        this.minSupportedFormatNumber = minSupportedFormatNumber;
    }

    public int getRequestedFormatNumber() {
        return requestedFormatNumber;
    }

    public int getMinSupportedFormatNumber() {
        return minSupportedFormatNumber;
    }

    private static String formatMessage(int formatNumber, int minFormatNumber) {
        String fmt = "Command cannot be executed, because it encoded in {0} format number, when minimum supported by backend is {1}";
        return MessageFormat.format(fmt, formatNumber, minFormatNumber);
    }

}
