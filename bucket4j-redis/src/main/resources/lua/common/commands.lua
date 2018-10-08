local LONG_MAX_VALUE = 9223372036854775807;

-- This is Lua re-implementation of io.github.bucket4j.remote.commands java package
local addBehaviorToCommand = function(command)
    command.bucketStateModified = false;
    local cmdName = command.name;

    if cmdName == 'AddTokens' then
        function command:execute(state, currentTimeNanos)
            state.refillAllBandwidth(currentTimeNanos);
            state.addTokens(self.tokensToAdd);
            self.bucketStateModified = true;
            return nil;
        end
    elseif cmdName == 'ConsumeAsMuchAsPossible' then
        function command:execute(state, currentTimeNanos)
            state.refillAllBandwidth(currentTimeNanos);
            local availableToConsume = state.getAvailableTokens();
            local toConsume = math.min(self.limit, availableToConsume);
            if toConsume <= 0 then
                return 0;
            end
            state.consume(toConsume);
            self.bucketStateModified = true;
            return toConsume;
        end
    elseif cmdName == 'ConsumeAsMuchAsPossible' then
        function command:execute(state, currentTimeNanos)
            state.refillAllBandwidth(currentTimeNanos);
            return state.copyBucketState();
        end
    elseif cmdName == 'CreateSnapshot' then
        function command:execute(state, currentTimeNanos)
            state.refillAllBandwidth(currentTimeNanos);
            return state.copyBucketState();
        end
    elseif cmdName == 'GetAvailableTokens' then
        function command:execute(state, currentTimeNanos)
            state.refillAllBandwidth(currentTimeNanos);
            return state.getAvailableTokens();
        end
    elseif cmdName == 'ReplaceConfigurationOrReturnPrevious' then
        function command:execute(state, currentTimeNanos)
            state.refillAllBandwidth(currentTimeNanos);
            local previousConfiguration = state.replaceConfigurationOrReturnPrevious(self.newConfiguration);
            if previousConfiguration ~= nil then
                return previousConfiguration;
            else
                self.bucketStateModified = true;
                return nil;
            end
        end
    elseif cmdName == 'ReserveAndCalculateTimeToSleep' then
        function command:execute(state, currentTimeNanos)
            state.refillAllBandwidth(currentTimeNanos);
            local nanosToCloseDeficit = state.calculateDelayNanosAfterWillBePossibleToConsume(self.tokensToConsume, currentTimeNanos);
            if nanosToCloseDeficit == LONG_MAX_VALUE or nanosToCloseDeficit > self.waitIfBusyNanosLimit then
                return LONG_MAX_VALUE;
            else
                state.consume(self.tokensToConsume);
                self.bucketStateModified = true;
                return nanosToCloseDeficit;
            end
        end
    elseif cmdName == 'TryConsumeAndReturnRemainingTokens' then
        function command:execute(state, currentTimeNanos)
            state.refillAllBandwidth(currentTimeNanos);
            local availableToConsume = state.getAvailableTokens();
            if self.tokensToConsume <= availableToConsume then
                state.consume(self.tokensToConsume);
                self.bucketStateModified = true;
                local remainingTokens = availableToConsume - self.tokensToConsume
                return {
                    ["consumed"] = true,
                    ["remainingTokens"] = remainingTokens,
                    ["nanosToWaitForRefill"] = 0
                };
            else
                local nanosToWaitForRefill = state.calculateDelayNanosAfterWillBePossibleToConsume(self.tokensToConsume, currentTimeNanos);
                return {
                    ["consumed"] = false,
                    ["remainingTokens"] = availableToConsume,
                    ["nanosToWaitForRefill"] = nanosToWaitForRefill
                };
            end
        end
    elseif cmdName == 'TryConsume' then
        function command:execute(state, currentTimeNanos)
            state.refillAllBandwidth(currentTimeNanos);
            local availableToConsume = state.getAvailableTokens();
            if self.tokensToConsume <= availableToConsume then
                state.consume(self.tokensToConsume);
                self.bucketStateModified = true;
                return true;
            else
                return false;
            end
        end
    else
       error("Unknown command " .. cmdName .. "]")
    end
end