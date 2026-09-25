package io.github.bucket4j;

import io.github.bucket4j.distributed.AsyncBucketProxy;
import io.github.bucket4j.mock.BucketType;
import io.github.bucket4j.mock.TimeMeterMock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ConfigurationReplacement Specification")
class ConfigurationReplacementTest {

    private record BucketTypeCase(BucketType bucketType) {

        @Override
        public String toString() {
            return "bucketType=" + bucketType;
        }
    }

    static Stream<BucketTypeCase> bucketTypes() {
        return Stream.of(BucketType.values()).map(BucketTypeCase::new);
    }

    @Nested
    @DisplayName("Sync bucket")
    class SyncBucket {

        @ParameterizedTest
        @MethodSource("io.github.bucket4j.ConfigurationReplacementTest#bucketTypes")
        void shouldForgetAboutPreviouslyConsumedTokensWhenReplaceConfigurationInResetMode(BucketTypeCase testCase) {
            for (boolean verbose : new boolean[]{true, false}) {
                TimeMeterMock clock = new TimeMeterMock(0);
                BucketConfiguration configuration = BucketConfiguration.builder()
                    .addLimit(it -> it.capacity(3).refillGreedy(3, Duration.ofNanos(5)).initialTokens(0))
                    .build();
                BucketConfiguration newConfiguration = BucketConfiguration.builder()
                    .addLimit(it -> it.capacity(60).refillGreedy(60, Duration.ofNanos(1000)))
                    .build();
                Bucket bucket = testCase.bucketType().createBucket(configuration, clock);
                clock.addTime(3); // 1.8
                if (!verbose) {
                    bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.RESET);
                } else {
                    bucket.asVerbose().replaceConfiguration(newConfiguration, TokensInheritanceStrategy.RESET);
                }
                assertThat(bucket.getAvailableTokens()).isEqualTo(60);
            }
        }

        @ParameterizedTest
        @MethodSource("io.github.bucket4j.ConfigurationReplacementTest#bucketTypes")
        void shouldForgetAboutPreviouslyConsumedTokensWhenReplaceConfigurationForBandwidthWhichNotMatchedById(BucketTypeCase testCase) {
            for (boolean verbose : new boolean[]{true, false}) {
                TimeMeterMock clock = new TimeMeterMock(0);
                BucketConfiguration configuration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(3, Duration.ofNanos(5)).withInitialTokens(0).withId("x"))
                    .build();
                BucketConfiguration newConfiguration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(60, Duration.ofNanos(1000)).withId("y"))
                    .build();
                Bucket bucket = testCase.bucketType().createBucket(configuration, clock);
                clock.addTime(3); // 1.8
                if (!verbose) {
                    bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.AS_IS);
                } else {
                    bucket.asVerbose().replaceConfiguration(newConfiguration, TokensInheritanceStrategy.AS_IS);
                }
                assertThat(bucket.getAvailableTokens()).isEqualTo(60);
            }
        }

        @ParameterizedTest
        @MethodSource("io.github.bucket4j.ConfigurationReplacementTest#bucketTypes")
        void shouldMatchBandwidthByIdDuringConfigurationReplacement(BucketTypeCase testCase) {
            for (boolean verbose : new boolean[]{true, false}) {
                TimeMeterMock clock = new TimeMeterMock(0);
                BucketConfiguration configuration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(3, Duration.ofNanos(5)).withInitialTokens(0).withId("x"))
                    .addLimit(Bandwidth.simple(1000, Duration.ofNanos(1000)).withInitialTokens(1).withId("z"))
                    .build();
                BucketConfiguration newConfiguration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(60, Duration.ofNanos(1000)).withId("y"))
                    .addLimit(Bandwidth.simple(1000, Duration.ofNanos(1000)).withId("z"))
                    .build();
                Bucket bucket = testCase.bucketType().createBucket(configuration, clock);
                if (!verbose) {
                    bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.AS_IS);
                } else {
                    bucket.asVerbose().replaceConfiguration(newConfiguration, TokensInheritanceStrategy.AS_IS);
                }
                assertThat(bucket.getAvailableTokens()).isEqualTo(1);
                BucketState snapshot = bucket.asVerbose().getAvailableTokens().getState();
                assertThat(snapshot.getCurrentSize(0)).isEqualTo(60);
                assertThat(snapshot.getCurrentSize(1)).isEqualTo(1);
            }
        }

        @ParameterizedTest
        @MethodSource("io.github.bucket4j.ConfigurationReplacementTest#bucketTypes")
        void testReplaceConfigurationAsIsFromGreedyRefillToGreedyRefill(BucketTypeCase testCase) {
            for (boolean verbose : new boolean[]{true, false}) {
                TimeMeterMock clock = new TimeMeterMock(0);
                BucketConfiguration configuration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(3, Duration.ofNanos(5)).withInitialTokens(0))
                    .build();
                BucketConfiguration newConfiguration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(60, Duration.ofNanos(1000)))
                    .build();
                Bucket bucket = testCase.bucketType().createBucket(configuration, clock);
                assertThat(bucket.getAvailableTokens()).isEqualTo(0);

                clock.addTime(3); // 1.8
                if (!verbose) {
                    bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.AS_IS);
                } else {
                    bucket.asVerbose().replaceConfiguration(newConfiguration, TokensInheritanceStrategy.AS_IS);
                }
                assertThat(bucket.getAvailableTokens()).isEqualTo(1);

                clock.addTime(4);
                assertThat(bucket.getAvailableTokens()).isEqualTo(2);
            }
        }

        @ParameterizedTest
        @MethodSource("io.github.bucket4j.ConfigurationReplacementTest#bucketTypes")
        void testReplaceConfigurationAsIsFromGreedyRefillToIntervallyRefill(BucketTypeCase testCase) {
            for (boolean verbose : new boolean[]{true, false}) {
                TimeMeterMock clock = new TimeMeterMock(0);
                BucketConfiguration configuration = BucketConfiguration.builder()
                    .addLimit(it -> it.capacity(3).refillGreedy(3, Duration.ofNanos(5)).initialTokens(0))
                    .build();
                BucketConfiguration newConfiguration = BucketConfiguration.builder()
                    .addLimit(it -> it.capacity(60).refillIntervally(60, Duration.ofNanos(1000)))
                    .build();
                Bucket bucket = testCase.bucketType().createBucket(configuration, clock);
                bucket.getAvailableTokens();
                clock.addTime(3); // 1.8
                if (!verbose) {
                    bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.AS_IS);
                } else {
                    bucket.asVerbose().replaceConfiguration(newConfiguration, TokensInheritanceStrategy.AS_IS);
                }
                assertThat(bucket.getAvailableTokens()).isEqualTo(1);

                clock.addTime(999);
                assertThat(bucket.getAvailableTokens()).isEqualTo(1); // 0.8 tokens from previous bucket should not be copied after config replacement

                clock.addTime(1);
                assertThat(bucket.getAvailableTokens()).isEqualTo(60);
            }
        }

        @ParameterizedTest
        @MethodSource("io.github.bucket4j.ConfigurationReplacementTest#bucketTypes")
        void shouldDecreaseAvailableTokensWhenReducingCapacityAndCopyingTokensAsIs(BucketTypeCase testCase) {
            TimeMeterMock clock = new TimeMeterMock(0);
            BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(500, Refill.greedy(100, Duration.ofNanos(100))))
                .build();
            BucketConfiguration newConfiguration = BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(200, Refill.greedy(100, Duration.ofNanos(100))))
                .build();
            Bucket bucket = testCase.bucketType().createBucket(configuration, clock);
            bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.AS_IS);
            assertThat(bucket.getAvailableTokens()).isEqualTo(200);
        }

        @ParameterizedTest
        @MethodSource("io.github.bucket4j.ConfigurationReplacementTest#bucketTypes")
        void testReplaceConfigurationAdditiveFromGreedyRefillToGreedyRefillWhenCapacityIncreased(BucketTypeCase testCase) {
            for (boolean verbose : new boolean[]{true, false}) {
                TimeMeterMock clock = new TimeMeterMock(0);
                BucketConfiguration configuration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(3, Duration.ofNanos(5)).withInitialTokens(0))
                    .build();
                BucketConfiguration newConfiguration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(60, Duration.ofNanos(1000)))
                    .build();
                Bucket bucket = testCase.bucketType().createBucket(configuration, clock);
                bucket.getAvailableTokens();
                clock.addTime(3); // 1.8
                if (!verbose) {
                    bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.ADDITIVE);
                } else {
                    bucket.asVerbose().replaceConfiguration(newConfiguration, TokensInheritanceStrategy.ADDITIVE);
                }
                assertThat(bucket.getAvailableTokens()).isEqualTo(58);

                clock.addTime(4);
                assertThat(bucket.getAvailableTokens()).isEqualTo(59);
            }
        }

        @ParameterizedTest
        @MethodSource("io.github.bucket4j.ConfigurationReplacementTest#bucketTypes")
        void testReplaceConfigurationAdditiveFromGreedyRefillToIntervallyRefillWhenCapacityIncreased(BucketTypeCase testCase) {
            for (boolean verbose : new boolean[]{true, false}) {
                TimeMeterMock clock = new TimeMeterMock(0);
                BucketConfiguration configuration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(3, Duration.ofNanos(5)).withInitialTokens(0))
                    .build();
                Refill refill = Refill.intervally(60, Duration.ofNanos(1000));
                BucketConfiguration newConfiguration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.classic(60, refill))
                    .build();
                Bucket bucket = testCase.bucketType().createBucket(configuration, clock);
                bucket.getAvailableTokens();
                clock.addTime(3); // 1.8
                if (!verbose) {
                    bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.ADDITIVE);
                } else {
                    bucket.asVerbose().replaceConfiguration(newConfiguration, TokensInheritanceStrategy.ADDITIVE);
                }
                assertThat(bucket.getAvailableTokens()).isEqualTo(58);

                clock.addTime(999);
                assertThat(bucket.getAvailableTokens()).isEqualTo(58); // 0.8 tokens from previous bucket should not be copied after config replacement

                clock.addTime(1);
                assertThat(bucket.getAvailableTokens()).isEqualTo(60);
            }
        }

        @ParameterizedTest
        @MethodSource("io.github.bucket4j.ConfigurationReplacementTest#bucketTypes")
        void shouldDecreaseAvailableTokensWhenReducingCapacityAndCopyingTokensAdditive(BucketTypeCase testCase) {
            TimeMeterMock clock = new TimeMeterMock(0);
            BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(500, Refill.greedy(100, Duration.ofNanos(100))))
                .build();
            BucketConfiguration newConfiguration = BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(200, Refill.greedy(100, Duration.ofNanos(100))))
                .build();
            Bucket bucket = testCase.bucketType().createBucket(configuration, clock);
            bucket.getAvailableTokens();
            bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.ADDITIVE);
            assertThat(bucket.getAvailableTokens()).isEqualTo(200);
        }

        @ParameterizedTest
        @MethodSource("io.github.bucket4j.ConfigurationReplacementTest#bucketTypes")
        void shouldIncreaseAvailableTokensWhenReducingCapacityAndCopyingTokensAdditive(BucketTypeCase testCase) {
            TimeMeterMock clock = new TimeMeterMock(0);

            BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(500, Refill.greedy(100, Duration.ofNanos(100))).withInitialTokens(200)).build();
            BucketConfiguration newConfiguration = BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(900, Refill.greedy(100, Duration.ofNanos(100))))
                .build();
            Bucket bucket = testCase.bucketType().createBucket(configuration, clock);
            bucket.getAvailableTokens();
            bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.ADDITIVE);
            assertThat(bucket.getAvailableTokens()).isEqualTo(600);
        }

        @ParameterizedTest
        @MethodSource("io.github.bucket4j.ConfigurationReplacementTest#bucketTypes")
        void shouldDecreaseAvailableTokensWhenReducingCapacityAndCopyingTokensProportionally(BucketTypeCase testCase) {
            TimeMeterMock clock = new TimeMeterMock(0);
            BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(500, Refill.greedy(100, Duration.ofNanos(100))))
                .build();
            BucketConfiguration newConfiguration = BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(200, Refill.greedy(100, Duration.ofNanos(100))))
                .build();
            Bucket bucket = testCase.bucketType().createBucket(configuration, clock);
            bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.PROPORTIONALLY);
            assertThat(bucket.getAvailableTokens()).isEqualTo(200);
        }

        @ParameterizedTest
        @MethodSource("io.github.bucket4j.ConfigurationReplacementTest#bucketTypes")
        void testReplaceConfigurationProportionallyFromGreedyRefillToGreedyRefill(BucketTypeCase testCase) {
            for (boolean verbose : new boolean[]{true, false}) {
                TimeMeterMock clock = new TimeMeterMock(0);
                BucketConfiguration configuration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(3, Duration.ofNanos(5)).withInitialTokens(0))
                    .build();
                BucketConfiguration newConfiguration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(60, Duration.ofNanos(1000)))
                    .build();
                Bucket bucket = testCase.bucketType().createBucket(configuration, clock);
                bucket.getAvailableTokens();
                clock.addTime(3); // 1.8
                if (!verbose) {
                    bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.PROPORTIONALLY);
                } else {
                    bucket.asVerbose().replaceConfiguration(newConfiguration, TokensInheritanceStrategy.PROPORTIONALLY);
                }
                assertThat(bucket.getAvailableTokens()).isEqualTo(36);

                clock.addTime(4);
                assertThat(bucket.getAvailableTokens()).isEqualTo(36);

                clock.addTime(13);
                assertThat(bucket.getAvailableTokens()).isEqualTo(37);
            }
        }

        @ParameterizedTest
        @MethodSource("io.github.bucket4j.ConfigurationReplacementTest#bucketTypes")
        void testReplaceConfigurationProportionallyWhenCapacityOverflown(BucketTypeCase testCase) {
            for (boolean verbose : new boolean[]{true, false}) {
                TimeMeterMock clock = new TimeMeterMock(0);
                BucketConfiguration configuration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(3, Duration.ofNanos(5)).withInitialTokens(0))
                    .build();

                BucketConfiguration newConfiguration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(60, Duration.ofNanos(1000)))
                    .build();
                Bucket bucket = testCase.bucketType().createBucket(configuration, clock);
                bucket.forceAddTokens(10000000);
                assertThat(bucket.getAvailableTokens()).isEqualTo(10000000);
                if (!verbose) {
                    bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.PROPORTIONALLY);
                } else {
                    bucket.asVerbose().replaceConfiguration(newConfiguration, TokensInheritanceStrategy.PROPORTIONALLY);
                }
                assertThat(bucket.getAvailableTokens()).isEqualTo(60); // because should be just reduced to maximum
            }
        }

        @ParameterizedTest
        @MethodSource("io.github.bucket4j.ConfigurationReplacementTest#bucketTypes")
        void testReplaceConfigurationProportionallyFromGreedyRefillToGreedyRefillCaseForRoundingErrorPropogation(BucketTypeCase testCase) {
            for (boolean verbose : new boolean[]{true, false}) {
                TimeMeterMock clock = new TimeMeterMock(0);
                BucketConfiguration configuration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(6, Duration.ofNanos(8)).withInitialTokens(0))
                    .build();
                BucketConfiguration newConfiguration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(3, Duration.ofNanos(8)))
                    .build();
                Bucket bucket = testCase.bucketType().createBucket(configuration, clock); // 0.75
                bucket.getAvailableTokens();
                clock.addTime(3); // 2.25
                if (!verbose) {
                    bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.PROPORTIONALLY);
                } else {
                    bucket.asVerbose().replaceConfiguration(newConfiguration, TokensInheritanceStrategy.PROPORTIONALLY);
                }
                assertThat(bucket.getAvailableTokens()).isEqualTo(1); // 1.125 after replacement
                assertThat(bucket.asVerbose().getAvailableTokens().getState().getRoundingError(0)).isEqualTo(1); // 1/8 == 0.125
            }
        }

        @ParameterizedTest
        @MethodSource("io.github.bucket4j.ConfigurationReplacementTest#bucketTypes")
        void testReplaceConfigurationProportionallyFromGreedyRefillToGreedyRefillCaseForRoundingErrorPropogationAndNegativeAmount(BucketTypeCase testCase) {
            for (boolean verbose : new boolean[]{true, false}) {
                TimeMeterMock clock = new TimeMeterMock(0);
                BucketConfiguration configuration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(6, Duration.ofNanos(8)).withInitialTokens(0))
                    .build();
                BucketConfiguration newConfiguration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(3, Duration.ofNanos(8)))
                    .build();
                Bucket bucket = testCase.bucketType().createBucket(configuration, clock); // 0.75
                bucket.getAvailableTokens();
                clock.addTime(3); // 2.25
                bucket.consumeIgnoringRateLimits(5); // -2.75
                if (!verbose) {
                    bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.PROPORTIONALLY);
                } else {
                    bucket.asVerbose().replaceConfiguration(newConfiguration, TokensInheritanceStrategy.PROPORTIONALLY);
                }
                assertThat(bucket.getAvailableTokens()).isEqualTo(-2); // -1.375 after replacement
                assertThat(bucket.asVerbose().getAvailableTokens().getState().getRoundingError(0)).isEqualTo(5); // 5/8 == 1 - 0.375
            }
        }

    }

    @Nested
    @DisplayName("Async bucket")
    class AsyncBucket {

        @ParameterizedTest
        @MethodSource("io.github.bucket4j.ConfigurationReplacementTest#bucketTypes")
        void shouldForgetAboutPreviouslyConsumedTokensWhenReplaceConfigurationInResetMode(BucketTypeCase testCase) throws Exception {
            for (boolean verbose : new boolean[]{true, false}) {
                TimeMeterMock clock = new TimeMeterMock(0);
                BucketConfiguration configuration = BucketConfiguration.builder()
                    .addLimit(it -> it.capacity(3).refillGreedy(3, Duration.ofNanos(5)).initialTokens(0))
                    .build();
                BucketConfiguration newConfiguration = BucketConfiguration.builder()
                    .addLimit(it -> it.capacity(60).refillGreedy(60, Duration.ofNanos(1000)))
                    .build();
                AsyncBucketProxy bucket = testCase.bucketType().createAsyncBucket(configuration, clock);
                clock.addTime(3); // 1.8
                if (!verbose) {
                    bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.RESET).get();
                } else {
                    bucket.asVerbose().replaceConfiguration(newConfiguration, TokensInheritanceStrategy.RESET).get();
                }
                assertThat(bucket.getAvailableTokens().get()).isEqualTo(60);
            }
        }

        @ParameterizedTest
        @MethodSource("io.github.bucket4j.ConfigurationReplacementTest#bucketTypes")
        void shouldForgetAboutPreviouslyConsumedTokensWhenReplaceConfigurationForBandwidthWhichNotMatchedById(BucketTypeCase testCase) throws Exception {
            for (boolean verbose : new boolean[]{true, false}) {
                TimeMeterMock clock = new TimeMeterMock(0);
                BucketConfiguration configuration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(3, Duration.ofNanos(5)).withInitialTokens(0).withId("x"))
                    .build();
                BucketConfiguration newConfiguration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(60, Duration.ofNanos(1000)).withId("y"))
                    .build();
                AsyncBucketProxy bucket = testCase.bucketType().createAsyncBucket(configuration, clock);
                clock.addTime(3); // 1.8
                if (!verbose) {
                    bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.AS_IS).get();
                } else {
                    bucket.asVerbose().replaceConfiguration(newConfiguration, TokensInheritanceStrategy.AS_IS).get();
                }
                assertThat(bucket.getAvailableTokens().get()).isEqualTo(60);
            }
        }

        @ParameterizedTest
        @MethodSource("io.github.bucket4j.ConfigurationReplacementTest#bucketTypes")
        void shouldMatchBandwidthByIdDuringConfigurationReplacement(BucketTypeCase testCase) throws Exception {
            for (boolean verbose : new boolean[]{true, false}) {
                TimeMeterMock clock = new TimeMeterMock(0);
                BucketConfiguration configuration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(3, Duration.ofNanos(5)).withInitialTokens(0).withId("x"))
                    .addLimit(Bandwidth.simple(1000, Duration.ofNanos(1000)).withInitialTokens(1).withId("z"))
                    .build();
                BucketConfiguration newConfiguration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(60, Duration.ofNanos(1000)).withId("y"))
                    .addLimit(Bandwidth.simple(1000, Duration.ofNanos(1000)).withId("z"))
                    .build();
                AsyncBucketProxy bucket = testCase.bucketType().createAsyncBucket(configuration, clock);
                if (!verbose) {
                    bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.AS_IS).get();
                } else {
                    bucket.asVerbose().replaceConfiguration(newConfiguration, TokensInheritanceStrategy.AS_IS).get();
                }
                assertThat(bucket.getAvailableTokens().get()).isEqualTo(1);
            }
        }

        @ParameterizedTest
        @MethodSource("io.github.bucket4j.ConfigurationReplacementTest#bucketTypes")
        void testReplaceConfigurationAsIsFromGreedyRefillToGreedyRefill(BucketTypeCase testCase) throws Exception {
            for (boolean verbose : new boolean[]{true, false}) {
                TimeMeterMock clock = new TimeMeterMock(0);
                BucketConfiguration configuration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(3, Duration.ofNanos(5)).withInitialTokens(0))
                    .build();
                BucketConfiguration newConfiguration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(60, Duration.ofNanos(1000)))
                    .build();
                AsyncBucketProxy bucket = testCase.bucketType().createAsyncBucket(configuration, clock);
                assertThat(bucket.getAvailableTokens().get()).isEqualTo(0);

                clock.addTime(3); // 1.8
                if (!verbose) {
                    bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.AS_IS).get();
                } else {
                    bucket.asVerbose().replaceConfiguration(newConfiguration, TokensInheritanceStrategy.AS_IS).get();
                }
                assertThat(bucket.getAvailableTokens().get()).isEqualTo(1);

                clock.addTime(4);
                assertThat(bucket.getAvailableTokens().get()).isEqualTo(2);
            }
        }

        @ParameterizedTest
        @MethodSource("io.github.bucket4j.ConfigurationReplacementTest#bucketTypes")
        void testReplaceConfigurationAsIsFromGreedyRefillToIntervallyRefill(BucketTypeCase testCase) throws Exception {
            for (boolean verbose : new boolean[]{true, false}) {
                TimeMeterMock clock = new TimeMeterMock(0);
                BucketConfiguration configuration = BucketConfiguration.builder()
                    .addLimit(it -> it.capacity(3).refillGreedy(3, Duration.ofNanos(5)).initialTokens(0))
                    .build();
                BucketConfiguration newConfiguration = BucketConfiguration.builder()
                    .addLimit(it -> it.capacity(60).refillIntervally(60, Duration.ofNanos(1000)))
                    .build();
                AsyncBucketProxy bucket = testCase.bucketType().createAsyncBucket(configuration, clock);
                bucket.getAvailableTokens();
                clock.addTime(3); // 1.8
                if (!verbose) {
                    bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.AS_IS).get();
                } else {
                    bucket.asVerbose().replaceConfiguration(newConfiguration, TokensInheritanceStrategy.AS_IS).get();
                }
                assertThat(bucket.getAvailableTokens().get()).isEqualTo(1);

                clock.addTime(999);
                assertThat(bucket.getAvailableTokens().get()).isEqualTo(1); // 0.8 tokens from previous bucket should not be copied after config replacement

                clock.addTime(1);
                assertThat(bucket.getAvailableTokens().get()).isEqualTo(60);
            }
        }

        @ParameterizedTest
        @MethodSource("io.github.bucket4j.ConfigurationReplacementTest#bucketTypes")
        void shouldDecreaseAvailableTokensWhenReducingCapacityAndCopyingTokensAsIs(BucketTypeCase testCase) throws Exception {
            TimeMeterMock clock = new TimeMeterMock(0);
            BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(500, Refill.greedy(100, Duration.ofNanos(100))))
                .build();
            BucketConfiguration newConfiguration = BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(200, Refill.greedy(100, Duration.ofNanos(100))))
                .build();
            AsyncBucketProxy bucket = testCase.bucketType().createAsyncBucket(configuration, clock);
            bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.AS_IS).get();
            assertThat(bucket.getAvailableTokens().get()).isEqualTo(200);
        }

        @ParameterizedTest
        @MethodSource("io.github.bucket4j.ConfigurationReplacementTest#bucketTypes")
        void testReplaceConfigurationAdditiveFromGreedyRefillToGreedyRefillWhenCapacityIncreased(BucketTypeCase testCase) throws Exception {
            for (boolean verbose : new boolean[]{true, false}) {
                TimeMeterMock clock = new TimeMeterMock(0);
                BucketConfiguration configuration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(3, Duration.ofNanos(5)).withInitialTokens(0))
                    .build();
                BucketConfiguration newConfiguration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(60, Duration.ofNanos(1000)))
                    .build();
                AsyncBucketProxy bucket = testCase.bucketType().createAsyncBucket(configuration, clock);
                bucket.getAvailableTokens().get();
                clock.addTime(3); // 1.8
                if (!verbose) {
                    bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.ADDITIVE).get();
                } else {
                    bucket.asVerbose().replaceConfiguration(newConfiguration, TokensInheritanceStrategy.ADDITIVE).get();
                }
                assertThat(bucket.getAvailableTokens().get()).isEqualTo(58);

                clock.addTime(4);
                assertThat(bucket.getAvailableTokens().get()).isEqualTo(59);
            }
        }

        @ParameterizedTest
        @MethodSource("io.github.bucket4j.ConfigurationReplacementTest#bucketTypes")
        void testReplaceConfigurationAdditiveFromGreedyRefillToIntervallyRefillWhenCapacityIncreased(BucketTypeCase testCase) throws Exception {
            for (boolean verbose : new boolean[]{true, false}) {
                TimeMeterMock clock = new TimeMeterMock(0);
                BucketConfiguration configuration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(3, Duration.ofNanos(5)).withInitialTokens(0))
                    .build();
                Refill refill = Refill.intervally(60, Duration.ofNanos(1000));
                BucketConfiguration newConfiguration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.classic(60, refill))
                    .build();
                AsyncBucketProxy bucket = testCase.bucketType().createAsyncBucket(configuration, clock);
                bucket.getAvailableTokens().get();
                clock.addTime(3); // 1.8
                if (!verbose) {
                    bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.ADDITIVE).get();
                } else {
                    bucket.asVerbose().replaceConfiguration(newConfiguration, TokensInheritanceStrategy.ADDITIVE).get();
                }
                assertThat(bucket.getAvailableTokens().get()).isEqualTo(58);

                clock.addTime(999);
                assertThat(bucket.getAvailableTokens().get()).isEqualTo(58); // 0.8 tokens from previous bucket should not be copied after config replacement

                clock.addTime(1);
                assertThat(bucket.getAvailableTokens().get()).isEqualTo(60);
            }
        }

        @ParameterizedTest
        @MethodSource("io.github.bucket4j.ConfigurationReplacementTest#bucketTypes")
        void shouldDecreaseAvailableTokensWhenReducingCapacityAndCopyingTokensAdditive(BucketTypeCase testCase) throws Exception {
            TimeMeterMock clock = new TimeMeterMock(0);
            BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(500, Refill.greedy(100, Duration.ofNanos(100))))
                .build();
            BucketConfiguration newConfiguration = BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(200, Refill.greedy(100, Duration.ofNanos(100))))
                .build();
            AsyncBucketProxy bucket = testCase.bucketType().createAsyncBucket(configuration, clock);
            bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.ADDITIVE).get();
            assertThat(bucket.getAvailableTokens().get()).isEqualTo(200);
        }

        @ParameterizedTest
        @MethodSource("io.github.bucket4j.ConfigurationReplacementTest#bucketTypes")
        void shouldIncreaseAvailableTokensWhenReducingCapacityAndCopyingTokensAdditive(BucketTypeCase testCase) throws Exception {
            TimeMeterMock clock = new TimeMeterMock(0);

            BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(500, Refill.greedy(100, Duration.ofNanos(100))).withInitialTokens(200)).build();
            BucketConfiguration newConfiguration = BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(900, Refill.greedy(100, Duration.ofNanos(100))))
                .build();
            AsyncBucketProxy bucket = testCase.bucketType().createAsyncBucket(configuration, clock);
            bucket.getAvailableTokens().get();
            bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.ADDITIVE).get();
            assertThat(bucket.getAvailableTokens().get()).isEqualTo(600);
        }

        @ParameterizedTest
        @MethodSource("io.github.bucket4j.ConfigurationReplacementTest#bucketTypes")
        void shouldDecreaseAvailableTokensWhenReducingCapacityAndCopyingTokensProportionally(BucketTypeCase testCase) throws Exception {
            TimeMeterMock clock = new TimeMeterMock(0);
            BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(500, Refill.greedy(100, Duration.ofNanos(100))))
                .build();
            BucketConfiguration newConfiguration = BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(200, Refill.greedy(100, Duration.ofNanos(100))))
                .build();
            AsyncBucketProxy bucket = testCase.bucketType().createAsyncBucket(configuration, clock);
            bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.PROPORTIONALLY).get();
            assertThat(bucket.getAvailableTokens().get()).isEqualTo(200);
        }

        @ParameterizedTest
        @MethodSource("io.github.bucket4j.ConfigurationReplacementTest#bucketTypes")
        void testReplaceConfigurationProportionallyFromGreedyRefillToGreedyRefill(BucketTypeCase testCase) throws Exception {
            for (boolean verbose : new boolean[]{true, false}) {
                TimeMeterMock clock = new TimeMeterMock(0);
                BucketConfiguration configuration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(3, Duration.ofNanos(5)).withInitialTokens(0))
                    .build();
                BucketConfiguration newConfiguration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(60, Duration.ofNanos(1000)))
                    .build();
                AsyncBucketProxy bucket = testCase.bucketType().createAsyncBucket(configuration, clock);
                bucket.getAvailableTokens();
                clock.addTime(3); // 1.8
                if (!verbose) {
                    bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.PROPORTIONALLY).get();
                } else {
                    bucket.asVerbose().replaceConfiguration(newConfiguration, TokensInheritanceStrategy.PROPORTIONALLY).get();
                }
                assertThat(bucket.getAvailableTokens().get()).isEqualTo(36);

                clock.addTime(4);
                assertThat(bucket.getAvailableTokens().get()).isEqualTo(36);

                clock.addTime(13);
                assertThat(bucket.getAvailableTokens().get()).isEqualTo(37);
            }
        }

        @ParameterizedTest
        @MethodSource("io.github.bucket4j.ConfigurationReplacementTest#bucketTypes")
        void testReplaceConfigurationProportionallyWhenCapacityOverflown(BucketTypeCase testCase) throws Exception {
            for (boolean verbose : new boolean[]{true, false}) {
                TimeMeterMock clock = new TimeMeterMock(0);
                BucketConfiguration configuration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(3, Duration.ofNanos(5)).withInitialTokens(0))
                    .build();

                BucketConfiguration newConfiguration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(60, Duration.ofNanos(1000)))
                    .build();
                AsyncBucketProxy bucket = testCase.bucketType().createAsyncBucket(configuration, clock);
                bucket.forceAddTokens(10000000);
                assertThat(bucket.getAvailableTokens().get()).isEqualTo(10000000);
                if (!verbose) {
                    bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.PROPORTIONALLY).get();
                } else {
                    bucket.asVerbose().replaceConfiguration(newConfiguration, TokensInheritanceStrategy.PROPORTIONALLY).get();
                }
                assertThat(bucket.getAvailableTokens().get()).isEqualTo(60); // because should be just reduced to maximum
            }
        }

        @ParameterizedTest
        @MethodSource("io.github.bucket4j.ConfigurationReplacementTest#bucketTypes")
        void testReplaceConfigurationProportionallyFromGreedyRefillToGreedyRefillCaseForRoundingErrorPropogation(BucketTypeCase testCase) throws Exception {
            for (boolean verbose : new boolean[]{true, false}) {
                TimeMeterMock clock = new TimeMeterMock(0);
                BucketConfiguration configuration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(6, Duration.ofNanos(8)).withInitialTokens(0))
                    .build();
                BucketConfiguration newConfiguration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(3, Duration.ofNanos(8)))
                    .build();
                AsyncBucketProxy bucket = testCase.bucketType().createAsyncBucket(configuration, clock); // 0.75
                bucket.getAvailableTokens().get();
                clock.addTime(3); // 2.25
                if (!verbose) {
                    bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.PROPORTIONALLY).get();
                } else {
                    bucket.asVerbose().replaceConfiguration(newConfiguration, TokensInheritanceStrategy.PROPORTIONALLY).get();
                }
                assertThat(bucket.getAvailableTokens().get()).isEqualTo(1); // 1.125 after replacement
                assertThat(bucket.asVerbose().getAvailableTokens().get().getState().getRoundingError(0)).isEqualTo(1); // 1/8 == 0.125
            }
        }

        @ParameterizedTest
        @MethodSource("io.github.bucket4j.ConfigurationReplacementTest#bucketTypes")
        void testReplaceConfigurationProportionallyFromGreedyRefillToGreedyRefillCaseForRoundingErrorPropogationAndNegativeAmount(BucketTypeCase testCase) throws Exception {
            for (boolean verbose : new boolean[]{true, false}) {
                TimeMeterMock clock = new TimeMeterMock(0);
                BucketConfiguration configuration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(6, Duration.ofNanos(8)).withInitialTokens(0))
                    .build();
                BucketConfiguration newConfiguration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(3, Duration.ofNanos(8)))
                    .build();
                AsyncBucketProxy bucket = testCase.bucketType().createAsyncBucket(configuration, clock); // 0.75
                bucket.getAvailableTokens().get();
                clock.addTime(3); // 2.25
                bucket.consumeIgnoringRateLimits(5).get(); // -2.75
                if (!verbose) {
                    bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.PROPORTIONALLY).get();
                } else {
                    bucket.asVerbose().replaceConfiguration(newConfiguration, TokensInheritanceStrategy.PROPORTIONALLY).get();
                }
                assertThat(bucket.getAvailableTokens().get()).isEqualTo(-2); // -1.375 after replacement
                assertThat(bucket.asVerbose().getAvailableTokens().get().getState().getRoundingError(0)).isEqualTo(5); // 5/8 == 1 - 0.375
            }
        }

    }

}
