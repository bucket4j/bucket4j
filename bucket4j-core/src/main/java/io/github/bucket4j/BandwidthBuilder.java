package io.github.bucket4j;

import java.time.Duration;
import java.time.Instant;

import io.github.bucket4j.Bandwidth.BandwidthBuilderBuildStage;
import io.github.bucket4j.Bandwidth.BandwidthBuilderCapacityStage;
import io.github.bucket4j.Bandwidth.BandwidthBuilderRefillStage;

/**
 * @author Vladimir Bukhtoyarov
 */
class BandwidthBuilder implements BandwidthBuilderCapacityStage, BandwidthBuilderRefillStage, BandwidthBuilderBuildStage {

    BandwidthBuilder() {

    }

    @Override
    public BandwidthBuilderRefillStage capacity(long tokens) {
        return this;
    }

    @Override
    public BandwidthBuilderBuildStage refillGreedy(long capacity, Duration period) {
        return this;
    }

    @Override
    public BandwidthBuilderBuildStage refillIntervally(long tokens, Duration period) {
        return this;
    }

    @Override
    public BandwidthBuilderBuildStage refillIntervallyAligned(long tokens, Duration period, Instant timeOfFirstRefill) {
        return this;
    }

    @Override
    public BandwidthBuilderBuildStage refillIntervallyAlignedWithAdaptiveInitialTokens(long tokens, Duration period, Instant timeOfFirstRefill) {
        return this;
    }

    @Override
    public Bandwidth build() {
        return null;
    }

    @Override
    public BandwidthBuilderBuildStage id(String id) {
        return this;
    }

    @Override
    public BandwidthBuilderBuildStage initialTokens(long initialTokens) {
        return this;
    }

}
