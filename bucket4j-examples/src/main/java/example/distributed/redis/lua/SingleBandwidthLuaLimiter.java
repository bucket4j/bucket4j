package example.distributed.redis.lua;

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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import io.lettuce.core.RedisNoScriptException;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

/**
 * A scoped, standalone, server-side ("compute inside Redis") token-bucket
 * limiter, built as a proof-of-concept for the discussion described in
 * {@code CAS-RETRY-RESEARCH-AND-CONTRIBUTION-PLAN.md} (section 9.A, "Strategy 1").
 *
 * <h2>What this is</h2>
 * bucket4j's existing Redis {@code ProxyManager}s (see {@code bucket4j-redis})
 * use optimistic compare-and-swap: {@code GET} bytes, deserialize, run the
 * token-bucket math in the JVM, serialize, then a Lua compare-and-set write.
 * Under contention on a hot key this means N concurrent callers produce 1
 * winner and N-1 retries (2 Redis round trips each), and a livelock-prone
 * retry loop can unfairly reject requests that still have tokens available.
 * <p>
 * This class instead performs the entire read -&gt; refill -&gt; consume -&gt;
 * write cycle inside a <b>single</b> Lua script, executed with one
 * {@code EVALSHA} round trip. Because Redis executes scripts
 * single-threadedly, no other client can observe or mutate the key while the
 * script runs - there is no separate compare step, and therefore no
 * possibility of a CAS conflict for this operation. The race is removed by
 * construction, not mitigated with retries/backoff.
 *
 * <h2>Why it exists</h2>
 * To produce a concrete, testable, benchmarkable example of the "server-side
 * execution" alternative discussed in the contribution plan, without touching
 * {@code bucket4j-core} or any existing {@code ProxyManager} implementation.
 * It is intentionally additive and isolated so it carries effectively zero
 * risk to the rest of the library.
 *
 * <h2>Scope and limits (read before using)</h2>
 * <ul>
 *     <li>Single bandwidth only - no multi-bandwidth composition.</li>
 *     <li>Fixed-interval integer refill only (matches
 *         {@code Refill.intervally} semantics) - no greedy refill, no
 *         floating point math anywhere.</li>
 *     <li>No verbose API, no {@code addTokens}/{@code reset}/reservation
 *         commands - only "try to consume N tokens".</li>
 *     <li>No dynamic reconfiguration: capacity/refill/interval are supplied
 *         by the caller on every call as script arguments, not stored
 *         server-side or versioned. If you change them for an existing key,
 *         the next call simply uses the new values going forward (tokens are
 *         clamped to the new capacity; the refill schedule is not reset).</li>
 *     <li>State is stored in a plain Redis {@code HASH} with named fields
 *         ({@code capacity}, {@code tokens}, {@code last_refill_ms}) -
 *         deliberately not bucket4j-core's internal {@code long[]} bucket
 *         state layout, which is undocumented and only versioned for its own
 *         format. This class owns its own wire format entirely.</li>
 *     <li>This is an example/POC, not a shipped, supported {@code ProxyManager}.
 *         It does not implement {@code ProxyManager}/{@code AsyncProxyManager}
 *         and is not wired into any {@code Bucket4j*} builder.</li>
 * </ul>
 *
 * <h2>Thread-safety</h2>
 * A single instance may be shared across threads as long as the underlying
 * {@link StatefulRedisConnection} is thread-safe (lettuce connections are).
 */
public class SingleBandwidthLuaLimiter {

    private static final String SCRIPT_RESOURCE_PATH = "/lua/single_bandwidth_token_bucket.lua";

    private static final String SCRIPT_TEXT = loadScript();

    private final RedisCommands<String, String> commands;
    private final long capacity;
    private final long refillTokens;
    private final long intervalMs;
    private final long ttlMs;

    private volatile String scriptSha;

    /**
     * @param connection   a lettuce connection using {@code String} keys/values
     * @param capacity     maximum number of tokens the bucket can hold
     * @param refillTokens number of tokens added per {@code intervalMs}
     * @param intervalMs   length of one fixed refill interval, in milliseconds
     */
    public SingleBandwidthLuaLimiter(StatefulRedisConnection<String, String> connection, long capacity, long refillTokens, long intervalMs) {
        this(connection, capacity, refillTokens, intervalMs, 2 * intervalMs);
    }

    /**
     * @param connection   a lettuce connection using {@code String} keys/values
     * @param capacity     maximum number of tokens the bucket can hold
     * @param refillTokens number of tokens added per {@code intervalMs}
     * @param intervalMs   length of one fixed refill interval, in milliseconds
     * @param ttlMs        TTL (re)applied to the bucket's Redis key on every call
     */
    public SingleBandwidthLuaLimiter(StatefulRedisConnection<String, String> connection, long capacity, long refillTokens, long intervalMs, long ttlMs) {
        if (connection == null) {
            throw new IllegalArgumentException("connection must not be null");
        }
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive, got " + capacity);
        }
        if (refillTokens <= 0) {
            throw new IllegalArgumentException("refillTokens must be positive, got " + refillTokens);
        }
        if (intervalMs <= 0) {
            throw new IllegalArgumentException("intervalMs must be positive, got " + intervalMs);
        }
        if (ttlMs <= 0) {
            throw new IllegalArgumentException("ttlMs must be positive, got " + ttlMs);
        }
        this.commands = connection.sync();
        this.capacity = capacity;
        this.refillTokens = refillTokens;
        this.intervalMs = intervalMs;
        this.ttlMs = ttlMs;
        this.scriptSha = commands.scriptLoad(SCRIPT_TEXT);
    }

    /**
     * Attempts to consume a single token.
     *
     * @param key bucket key
     * @return {@code true} if the token was consumed
     */
    public boolean tryConsume(String key) {
        return tryConsume(key, 1L);
    }

    /**
     * Attempts to consume {@code tokens} tokens.
     *
     * @param key    bucket key
     * @param tokens number of tokens requested, must be positive
     * @return {@code true} if the tokens were consumed
     */
    public boolean tryConsume(String key, long tokens) {
        return tryConsumeDetailed(key, tokens).isAllowed();
    }

    /**
     * Attempts to consume {@code tokens} tokens, returning the full detailed
     * outcome (allowed/denied, remaining tokens, suggested retry delay).
     *
     * @param key    bucket key
     * @param tokens number of tokens requested, must be positive
     */
    public LimitResult tryConsumeDetailed(String key, long tokens) {
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        if (tokens <= 0) {
            throw new IllegalArgumentException("tokens must be positive, got " + tokens);
        }

        String[] keys = { key };
        String[] args = {
            Long.toString(capacity),
            Long.toString(refillTokens),
            Long.toString(intervalMs),
            Long.toString(tokens),
            Long.toString(ttlMs)
        };

        List<Object> reply;
        try {
            reply = evalSha(keys, args);
        } catch (RedisNoScriptException e) {
            // The script cache was flushed (e.g. SCRIPT FLUSH, or a failover to a
            // replica/new primary that never saw SCRIPT LOAD). Reload once and
            // retry with a direct EVAL so this call still completes without a
            // second round trip's worth of extra latency next time.
            synchronized (this) {
                this.scriptSha = commands.scriptLoad(SCRIPT_TEXT);
            }
            reply = evalDirect(keys, args);
        }

        return LimitResult.fromScriptReply(reply);
    }

    private List<Object> evalSha(String[] keys, String[] args) {
        List<Object> reply = commands.evalsha(scriptSha, ScriptOutputType.MULTI, keys, args);
        return reply;
    }

    private List<Object> evalDirect(String[] keys, String[] args) {
        List<Object> reply = commands.eval(SCRIPT_TEXT, ScriptOutputType.MULTI, keys, args);
        return reply;
    }

    private static String loadScript() {
        try (InputStream in = SingleBandwidthLuaLimiter.class.getResourceAsStream(SCRIPT_RESOURCE_PATH)) {
            if (in == null) {
                throw new IllegalStateException("Lua script resource not found on classpath: " + SCRIPT_RESOURCE_PATH);
            }
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int read;
            while ((read = in.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load " + SCRIPT_RESOURCE_PATH, e);
        }
    }

    /**
     * Outcome of a {@link #tryConsumeDetailed(String, long)} call.
     */
    public static final class LimitResult {

        private final boolean allowed;
        private final long remainingTokens;
        private final long retryAfterMillis;

        LimitResult(boolean allowed, long remainingTokens, long retryAfterMillis) {
            this.allowed = allowed;
            this.remainingTokens = remainingTokens;
            this.retryAfterMillis = retryAfterMillis;
        }

        static LimitResult fromScriptReply(List<Object> reply) {
            long allowedFlag = ((Number) reply.get(0)).longValue();
            long remaining = ((Number) reply.get(1)).longValue();
            long retryAfter = ((Number) reply.get(2)).longValue();
            return new LimitResult(allowedFlag == 1L, remaining, retryAfter);
        }

        /**
         * @return {@code true} if the requested tokens were consumed
         */
        public boolean isAllowed() {
            return allowed;
        }

        /**
         * @return tokens remaining in the bucket immediately after this call
         */
        public long getRemainingTokens() {
            return remainingTokens;
        }

        /**
         * @return milliseconds until the next scheduled refill boundary; {@code 0}
         *         when {@link #isAllowed()} is {@code true}, otherwise a value in
         *         {@code (0, intervalMs]}
         */
        public long getRetryAfterMillis() {
            return retryAfterMillis;
        }

        @Override
        public String toString() {
            return "LimitResult{" +
                "allowed=" + allowed +
                ", remainingTokens=" + remainingTokens +
                ", retryAfterMillis=" + retryAfterMillis +
                '}';
        }
    }
}
