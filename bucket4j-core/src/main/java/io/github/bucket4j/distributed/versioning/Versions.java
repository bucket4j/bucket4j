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

public enum Versions implements Version {

    v_7_0_0(1);

    private final int number;

    private Versions(int number) {
        this.number = number;
    }

    public static void check(int formatNumber, Versions min, Versions max) {
        if (formatNumber < min.getNumber()) {
            throw new UsageOfObsoleteApiException(formatNumber, min.number);
        }
        if (formatNumber > max.getNumber()) {
            throw new UsageOfUnsupportedApiException(formatNumber, max.number);
        }
    }

    public static void checkMin(int formatNumber, Versions min) {
        if (formatNumber < min.getNumber()) {
            throw new UsageOfObsoleteApiException(formatNumber, min.number);
        }
    }

    public static void checkMax(int formatNumber, Versions max) {
        if (formatNumber > max.getNumber()) {
            throw new UsageOfUnsupportedApiException(formatNumber, max.number);
        }
    }

    @Override
    public int getNumber() {
        return number;
    }

    public static Version getLatest() {
        return v_7_0_0;
    }

    public static Version getOldest() {
        return v_7_0_0;
    }

    public static Version byNumber(int number) {
        for (Versions version : values()) {
            if (version.number == number) {
                return version;
            }
        }
        return new UnknownVersion(number);
    }

    public static class UnknownVersion implements Version {

        private final int number;

        public UnknownVersion(int number) {
            this.number = number;
        }

        @Override
        public int getNumber() {
            return number;
        }
    }

}
