-- This is direct analog of io.github.bucket4j.grid.jcache.JCacheEntryProcessor

local bucket4jExecute = function (key, currentTimeNanos, command)
-- TODO
end

local bucket4jInitState = function (key, currentTimeNanos, configuration)
    -- TODO
end

local bucket4jInitStateAndExecute = function (key, currentTimeNanos, configuration, command)
    -- TODO
end

-- Allow to use time related functions
redis.replicate_commands();

local commandType = ARGV[1];
local currentTimeNanos = ARGV[2];
if currentTimeNanos == "unset" then
    local time = TIME();
    currentTimeNanos = time[1] * 1000000000 + time[2] * 1000;
else
    currentTimeNanos = tonumber(currentTimeNanos);
end

local key = KEYS[1];

if commandType == "EXECUTE" then
    local commandJson = ARGV[3];
    local command = cjson.decode(commandJson);
    return bucket4jExecute(key, currentTimeNanos, command);
end

if commandType == "INIT_STATE" then
    local configurationJson = ARGV[3];
    local configuration = cjson.decode(configurationJson);
    return bucket4jInitState(key, currentTimeNanos, configuration);
end

if commandType == "INIT_STATE_AND_EXECUTE" then
    local configurationJson = ARGV[3];
    local configuration = cjson.decode(configurationJson);
    local commandJson = ARGV[3];
    local command = cjson.decode(commandJson);
    return bucket4jInitStateAndExecute(key, currentTimeNanos, configuration, command);
end

return redis.error_reply("Unknown command type [" .. commandType .. "]")


