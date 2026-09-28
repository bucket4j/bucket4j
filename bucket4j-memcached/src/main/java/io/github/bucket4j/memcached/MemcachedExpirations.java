/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2026 Vladimir Bukhtoyarov
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
package io.github.bucket4j.memcached;

import java.util.concurrent.TimeUnit;

/**
 * Converts a relative time-to-live into the value memcached's {@code expiration} protocol field expects.
 *
 * <p>
 * Memcached interprets {@code expiration} values up to 30 days as a relative number of seconds from now,
 * and any larger value as an absolute Unix timestamp. A TTL that exceeds that threshold must therefore be
 * converted to an absolute timestamp, otherwise memcached would expire the entry immediately.
 */
public final class MemcachedExpirations {

    private static final long THIRTY_DAYS_SECONDS = TimeUnit.DAYS.toSeconds(30);

    private MemcachedExpirations() {
    }

    public static int toMemcachedExpiration(long ttlMillis) {
        if (ttlMillis <= 0) {
            // 0 means "never expire" for memcached
            return 0;
        }
        long ttlSeconds = (ttlMillis + 999) / 1000;
        if (ttlSeconds <= THIRTY_DAYS_SECONDS) {
            return (int) ttlSeconds;
        }
        long absoluteSeconds = System.currentTimeMillis() / 1000 + ttlSeconds;
        return (int) Math.min(absoluteSeconds, Integer.MAX_VALUE);
    }

}
