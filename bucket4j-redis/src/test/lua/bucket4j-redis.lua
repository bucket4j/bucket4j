-- deserialize request
local key = KEYS[1];
local requestJson = ARGV[1];
local request = cjson.decode(requestJson);

-- get time
local currentTimeNanos = request.clientSideTime;
if currentTimeNanos == nil then
    local time = TIME();
    currentTimeNanos = time[1] * 1000000000 + time[2] * 1000;
end

-- get current bucket state
local stateJson = redis.call('GET', key);
local state;
if stateJson ~= nil then
    state = cjson.decode(stateJson);
end

-- init bucketEntry
local mutableBucketEntry = Bucket4j.createBucketEntryWrapper(state);

-- execute
local result = Bucket4j.execute.execute(request, mutableBucketEntry, currentTimeNanos);
if mutableBucketEntry.isStateModified() then
    local newState = mutableBucketEntry.get()
    local newStateJson = cjson.serialize(newState)
    redis.call('SET', key, newStateJson)
end

resultJson = cjson.serialize(result)
return resultJson


