package io.github.bucket4j.distributed.proxy;

import io.github.bucket4j.TokensInheritanceStrategy;

public class ImplicitConfigurationReplacement {

    private final long desiredConfigurationVersion;
    private final TokensInheritanceStrategy tokensInheritanceStrategy;

    public ImplicitConfigurationReplacement(long desiredConfigurationVersion, TokensInheritanceStrategy tokensInheritanceStrategy) {
        this.desiredConfigurationVersion = desiredConfigurationVersion;
        this.tokensInheritanceStrategy = tokensInheritanceStrategy;
    }

    public long getDesiredConfigurationVersion() {
        return desiredConfigurationVersion;
    }

    public TokensInheritanceStrategy getTokensInheritanceStrategy() {
        return tokensInheritanceStrategy;
    }

}
