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


public class ConsumeAsMuchAsPossibleCommand implements GridCommand<Long> {

    private static final long serialVersionUID = 1L;

    private long limit;
    private boolean bucketStateModified;

    public ConsumeAsMuchAsPossibleCommand(long limit) {
        this.limit = limit;
    }

    @Override
    public Long execute(GridBucketState state, long currentTimeNanos) {
        state.refillAllBandwidth(currentTimeNanos);
        long availableToConsume = state.getAvailableTokens();
        long toConsume = Math.min(limit, availableToConsume);
        if (toConsume <= 0) {
            return 0L;
        }
        state.consume(toConsume);
        bucketStateModified = true;
        return toConsume;
    }

    @Override
    public boolean isBucketStateModified() {
        return bucketStateModified;
    }

}
