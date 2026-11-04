-- Single-bandwidth, fixed-interval integer refill token bucket.
--
-- This script performs the entire read -> refill -> consume -> write cycle
-- atomically inside Redis. Because Redis executes scripts single-threadedly,
-- no other client can observe or mutate the key while this script runs, so
-- there is no compare-and-swap step and therefore no possibility of a CAS
-- conflict/retry for this operation - concurrent callers are simply
-- serialized by Redis itself.
--
-- State is stored in a Redis HASH (not bucket4j-core's internal long[]
-- layout) with the fields:
--   capacity        - configured bucket capacity (informational, refreshed every call)
--   tokens           - tokens currently available
--   last_refill_ms   - epoch millis of the last refill boundary this bucket advanced to
--
-- KEYS[1] = bucket key
-- ARGV[1] = capacity (integer)
-- ARGV[2] = refill_tokens (integer tokens added per interval)
-- ARGV[3] = interval_ms (integer length of one refill period, in milliseconds)
-- ARGV[4] = requested (integer tokens requested by this call)
-- ARGV[5] = ttl_ms (integer key TTL (re)applied after this call, in milliseconds)
--
-- Returns { allowed(0|1), remaining_tokens, retry_after_ms }
--   allowed         - 1 if `requested` tokens were consumed, 0 otherwise
--   remaining_tokens - tokens left in the bucket after this call
--   retry_after_ms   - 0 when allowed; otherwise the number of milliseconds
--                      until the next scheduled refill boundary, in (0, interval_ms]

if redis.replicate_commands then
    -- Present on Redis >= 3.2; keeps this script safe on clusters/replicas
    -- that still expect the legacy "effects replication opt-in" behaviour.
    -- It is a no-op on modern Redis where effects replication is the default.
    redis.replicate_commands()
end

local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local refillTokens = tonumber(ARGV[2])
local intervalMs = tonumber(ARGV[3])
local requested = tonumber(ARGV[4])
local ttlMs = tonumber(ARGV[5])

-- Redis TIME is the single source of truth for "now" across every caller,
-- so all concurrent clients agree on elapsed time regardless of their own
-- clock skew.
local time = redis.call('TIME')
local nowMs = math.floor(tonumber(time[1]) * 1000 + tonumber(time[2]) / 1000)

local tokens
local lastRefillMs

local existing = redis.call('HMGET', key, 'tokens', 'last_refill_ms')
if existing[1] == false or existing[2] == false then
    -- Fresh bucket: start full, anchored to now.
    tokens = capacity
    lastRefillMs = nowMs
else
    tokens = tonumber(existing[1])
    lastRefillMs = tonumber(existing[2])

    local elapsed = nowMs - lastRefillMs
    if elapsed > 0 then
        local periods = math.floor(elapsed / intervalMs)
        if periods > 0 then
            -- Advance by whole periods (not "reset to now") so the refill
            -- schedule never drifts, matching Refill.intervally semantics.
            tokens = math.min(capacity, tokens + periods * refillTokens)
            lastRefillMs = lastRefillMs + periods * intervalMs
        end
    end

    if tokens > capacity then
        -- Defensive clamp, e.g. if capacity was lowered by the caller between calls.
        tokens = capacity
    end
end

local allowed
local retryAfterMs

if tokens >= requested then
    tokens = tokens - requested
    allowed = 1
    retryAfterMs = 0
else
    allowed = 0
    local sinceLastRefill = nowMs - lastRefillMs
    retryAfterMs = intervalMs - sinceLastRefill
    if retryAfterMs <= 0 then
        retryAfterMs = intervalMs
    end
end

redis.call('HSET', key, 'capacity', capacity, 'tokens', tokens, 'last_refill_ms', lastRefillMs)
redis.call('PEXPIRE', key, ttlMs)

return { allowed, tokens, retryAfterMs }
