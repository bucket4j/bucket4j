local LONG_MAX_VALUE = 9223372036854775807;

local Bucket4j = {};

Bucket4j.createNewState = function(configuration, currentTimeNanos)
    local state = {};
    local tokens = {};
    state.tokens = tokens;
    local lastRefillTime = {};
    state.lastRefillTime = lastRefillTime;

    for i, bandwidth in ipairs(configuration.bandwidths) do
        tokens[i] = bandwidth.initialTokens;
        lastRefillTime[i] = currentTimeNanos;
    end

    Bucket4j.addBehaviorToState(state);
    return state;
end

Bucket4j.addBehaviorToState = function(state)
    function state:getAvailableTokens(bandwidths)
        local availableTokens = nil;
        for i, tokens in ipairs(self.tokens) do
            if availableTokens == nil or availableTokens > tokens then
                availableTokens = tokens;
            end
        end
        return availableTokens;
    end

    function state:consume(bandwidths, toConsume)
        for i, tokens in ipairs(self.tokens) do
            self.tokens[i] = tokens - toConsume
        end
    end

    function state:addTokens(bandwidths, tokensToAdd)
        for i, bandwidth in ipairs(bandwidths) do
            local currentSize = self.tokens[i];
            local newSize = currentSize + tokensToAdd;
            if newSize >= bandwidth.capacity then
                self.tokens[i] = bandwidth.capacity;
            else
                self.tokens[i] = newSize;
            end
        end
    end

    function state:refillAllBandwidth(bandwidths, currentTimeNanos)
        for i, bandwidth in ipairs(bandwidths) do
            self:refill(i, bandwidth, currentTimeNanos);
        end
    end

    function state:refill(bandwidthIndex, bandwidth, currentTimeNanos)
        local previousRefillNanos = self.lastRefillTime[bandwidthIndex];
        if currentTimeNanos <= previousRefillNanos then
            return;
        end

        if bandwidth.refillIntervally then
            local incompleteIntervalCorrection = (currentTimeNanos - previousRefillNanos) % bandwidth.refillPeriodNanos;
            currentTimeNanos = currentTimeNanos - incompleteIntervalCorrection;
        end
        if currentTimeNanos <= previousRefillNanos then
            return;
        else
            self.lastRefillTime[bandwidthIndex] = currentTimeNanos;
        end

        local capacity = bandwidth.capacity;
        local refillPeriodNanos = bandwidth.refillPeriodNanos;
        local refillTokens = bandwidth.refillTokens;
        local newSize = self.tokens[bandwidthIndex];

        local durationSinceLastRefillNanos = currentTimeNanos - previousRefillNanos;
        if durationSinceLastRefillNanos > refillPeriodNanos then
            local elapsedPeriods = durationSinceLastRefillNanos / refillPeriodNanos;
            local calculatedRefill = elapsedPeriods * refillTokens;
            newSize = newSize + calculatedRefill;
            if newSize > capacity then
                self.tokens[bandwidthIndex] = capacity;
                return;
            end
            durationSinceLastRefillNanos = durationSinceLastRefillNanos % refillPeriodNanos;
        end

        local calculatedRefill = durationSinceLastRefillNanos / refillPeriodNanos * refillTokens;
        newSize = newSize + calculatedRefill;
        if newSize >= capacity then
            newSize = capacity;
        end
        self.tokens[bandwidthIndex] = newSize;
    end

    function state:calculateDelayNanosAfterWillBePossibleToConsume(bandwidths, tokensToConsume, currentTimeNanos)
        local delayAfterWillBePossibleToConsume = nil;
        for i, bandwidth in ipairs(bandwidths) do
            local delay = self:calculateDelayNanosAfterWillBePossibleToConsumeForBandwidth(i, bandwidth, tokensToConsume, currentTimeNanos);
            if delayAfterWillBePossibleToConsume == nil or delayAfterWillBePossibleToConsume < delay then
                delayAfterWillBePossibleToConsume = delay;
            end
        end
        return delayAfterWillBePossibleToConsume;
    end

    function state:calculateDelayNanosAfterWillBePossibleToConsumeForBandwidth(bandwidthIndex, bandwidth, tokensToConsume, currentTimeNanos)
        local currentSize = self.tokens[bandwidthIndex];
        if (tokensToConsume <= currentSize) then
            return 0;
        end
        local deficit = tokensToConsume - currentSize;

        local nanosToWait;
        if bandwidth.refillIntervally then
            nanosToWait = self:calculateDelayNanosAfterWillBePossibleToConsumeForIntervalBandwidth(bandwidthIndex, bandwidth, deficit, currentTimeNanos);
        else
            nanosToWait = self:calculateDelayNanosAfterWillBePossibleToConsumeForGreedyBandwidth(bandwidth, deficit);
        end
        if nanosToWait < LONG_MAX_VALUE then
            return nanosToWait;
        else
            return LONG_MAX_VALUE;
        end
    end

    function state:calculateDelayNanosAfterWillBePossibleToConsumeForGreedyBandwidth(bandwidth, deficit)
        return bandwidth.refillPeriodNanos * deficit / bandwidth.refillTokens;
    end

    function state:calculateDelayNanosAfterWillBePossibleToConsumeForIntervalBandwidth(bandwidthIndex, bandwidth, deficit, currentTimeNanos)
        local refillPeriodNanos = bandwidth.refillPeriodNanos;
        local refillTokens = bandwidth.refillTokens;
        local previousRefillNanos = self.lastRefillTime[bandwidthIndex];

        local timeOfNextRefillNanos = previousRefillNanos + refillPeriodNanos;
        local waitForNextRefillNanos = timeOfNextRefillNanos - currentTimeNanos;
        if deficit <= refillTokens then
            return waitForNextRefillNanos;
        end

        deficit = deficit - refillTokens;
        local deficitPeriodsAsDouble = math.ceil(deficit / refillTokens);

        local deficitNanos = deficitPeriodsAsDouble * refillPeriodNanos;
        return deficitNanos + waitForNextRefillNanos;
    end

end

Bucket4j.addBehaviorToConfiguration = function(configuration)

    function configuration:isCompatible(otherConfiguration)
        return table.maxn(self.bandwidths) == table.maxn(otherConfiguration.bandwidths);
    end

end

-- This is Lua re-implementation of io.github.bucket4j.remote.RemoteBucketState
Bucket4j.addBehaviorToStateWithConfiguration = function(stateWithConfiguration)
    Bucket4j.addBehaviorToState(stateWithConfiguration.state);
    Bucket4j.addBehaviorToConfiguration(stateWithConfiguration.configuration);

    function stateWithConfiguration:refillAllBandwidth(currentTimeNanos)
        self.state:refillAllBandwidth(self.configuration.bandwidths, currentTimeNanos);
    end

    function stateWithConfiguration:getAvailableTokens()
        self.state:getAvailableTokens(self.configuration.bandwidths);
    end

    function stateWithConfiguration:consume(tokensToConsume)
        self.state:consume(self.configuration.bandwidths, tokensToConsume);
    end

    function stateWithConfiguration:calculateDelayNanosAfterWillBePossibleToConsume(tokensToConsume, currentTimeNanos)
        self.state:calculateDelayNanosAfterWillBePossibleToConsume(self.configuration.bandwidths, tokensToConsume, currentTimeNanos);
    end

    function stateWithConfiguration:addTokens(tokensToAdd)
        self.state:addTokens(self.configuration.bandwidths, tokensToAdd);
    end

    function stateWithConfiguration:replaceConfigurationOrReturnPrevious(newConfiguration)
        if self.configuration:isCompatible(newConfiguration) == false then
            return self.configuration;
        else
            self.configuration = newConfiguration;
            return nil;
        end
    end
end

-- This is Lua re-implementation of io.github.bucket4j.remote.commands java package
Bucket4j.addBehaviorToCommand = function(command)
    command.bucketStateModified = false;
    local cmdName = command.name;

    if cmdName == 'AddTokens' then
        function command:execute(state, currentTimeNanos)
            state:refillAllBandwidth(currentTimeNanos);
            state:addTokens(self.tokensToAdd);
            self.bucketStateModified = true;
            return nil;
        end
    elseif cmdName == 'ConsumeAsMuchAsPossible' then
        function command:execute(state, currentTimeNanos)
            state:refillAllBandwidth(currentTimeNanos);
            local availableToConsume = state:getAvailableTokens();
            local toConsume = math.min(self.limit, availableToConsume);
            if toConsume <= 0 then
                return 0;
            end
            state:consume(toConsume);
            self.bucketStateModified = true;
            return toConsume;
        end
    elseif cmdName == 'CreateSnapshot' then
        function command:execute(state, currentTimeNanos)
            state:refillAllBandwidth(currentTimeNanos);
            return state.state;
        end
    elseif cmdName == 'GetAvailableTokens' then
        function command:execute(state, currentTimeNanos)
            state:refillAllBandwidth(currentTimeNanos);
            return state:getAvailableTokens();
        end
    elseif cmdName == 'ReplaceConfigurationOrReturnPrevious' then
        function command:execute(state, currentTimeNanos)
            state:refillAllBandwidth(currentTimeNanos);
            local previousConfiguration = state:replaceConfigurationOrReturnPrevious(self.newConfiguration);
            if previousConfiguration ~= nil then
                return previousConfiguration;
            else
                self.bucketStateModified = true;
                return nil;
            end
        end
    elseif cmdName == 'ReserveAndCalculateTimeToSleep' then
        function command:execute(state, currentTimeNanos)
            state:refillAllBandwidth(currentTimeNanos);
            local nanosToCloseDeficit = state:calculateDelayNanosAfterWillBePossibleToConsume(self.tokensToConsume, currentTimeNanos);
            if nanosToCloseDeficit == LONG_MAX_VALUE or nanosToCloseDeficit > self.waitIfBusyNanosLimit then
                return LONG_MAX_VALUE;
            else
                state:consume(self.tokensToConsume);
                self.bucketStateModified = true;
                return nanosToCloseDeficit;
            end
        end
    elseif cmdName == 'TryConsumeAndReturnRemainingTokens' then
        function command:execute(state, currentTimeNanos)
            state:refillAllBandwidth(currentTimeNanos);
            local availableToConsume = state.getAvailableTokens();
            if self.tokensToConsume <= availableToConsume then
                state:consume(self.tokensToConsume);
                self.bucketStateModified = true;
                local remainingTokens = availableToConsume - self.tokensToConsume
                return {
                    ["consumed"] = true,
                    ["remainingTokens"] = remainingTokens,
                    ["nanosToWaitForRefill"] = 0
                };
            else
                local nanosToWaitForRefill = state:calculateDelayNanosAfterWillBePossibleToConsume(self.tokensToConsume, currentTimeNanos);
                return {
                    ["consumed"] = false,
                    ["remainingTokens"] = availableToConsume,
                    ["nanosToWaitForRefill"] = nanosToWaitForRefill
                };
            end
        end
    elseif cmdName == 'TryConsume' then
        function command:execute(state, currentTimeNanos)
            state:refillAllBandwidth(currentTimeNanos);
            local availableToConsume = state:getAvailableTokens();
            if self.tokensToConsume <= availableToConsume then
                state:consume(self.tokensToConsume);
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