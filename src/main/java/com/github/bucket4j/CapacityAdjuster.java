/*
 * Copyright 2015 Vladimir Bukhtoyarov
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.github.bucket4j;

import java.io.Serializable;

/**
 * Provider of bandwidth capacity.
 */
public interface CapacityAdjuster extends Serializable {

    /**
     * Return capacity of bandwidth which can depends from current time if needs.
     *
     * @param currentTime Cuurent time which returned by timeMeter {@link TimeMeter}
     *
     * @return cuurent capacity of bandwidth
     */
    long getCapacity(long currentTime);

    class ImmutableCapacity implements CapacityAdjuster {

        private final long value;

        public ImmutableCapacity(long value) {
            this.value = value;
        }

        @Override
        public long getCapacity(long currentTime) {
            return value;
        }

        @Override
        public String toString() {
            return "ImmutableCapacity{" +
                    "value=" + value +
                    '}';
        }

    }

}
