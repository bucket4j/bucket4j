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

public class UsageOfUnsupportedApiException extends BackwardCompatibilityException {

    private final int requestedFormatNumber;
    private final int maxSupportedFormatNumber;

    public UsageOfUnsupportedApiException(int requestedFormatNumber, int maxSupportedFormatNumber) {
        super(formatMessage(requestedFormatNumber, maxSupportedFormatNumber));
        this.requestedFormatNumber = requestedFormatNumber;
        this.maxSupportedFormatNumber = maxSupportedFormatNumber;
    }

    public int getRequestedFormatNumber() {
        return requestedFormatNumber;
    }

    public int getMaxSupportedFormatNumber() {
        return maxSupportedFormatNumber;
    }

    private static String formatMessage(int formatNumber, int maxFormatNumber) {
        String fmt = "Command cannot be executed, because it encoded in {0} format number, when maximum supported by backend is {1}";
        return MessageFormat.format(fmt, formatNumber, maxFormatNumber);
    }

}
