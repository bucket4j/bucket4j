-- Allow to use time related functions
redis.replicate_commands();

local key = KEYS[1];
local stateJson = ARGV[1];
local commandJson = ARGV[2];