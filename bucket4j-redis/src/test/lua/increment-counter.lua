local value = redis.call('get', KEYS[1]);

local nextValue = 0;

if (value == nil or (type(value) == "boolean")) then
    nextValue = 1
else
    nextValue = value + 1
end

redis.call('set', KEYS[1], nextValue)
return nextValue