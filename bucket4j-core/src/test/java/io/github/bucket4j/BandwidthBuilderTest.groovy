package io.github.bucket4j;

import io.github.bucket4j.util.ComparableByContent;
import spock.lang.Specification;
import spock.lang.Unroll

import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit;

/**
 * @author Vladimir Bukhtoyarov
 */
class BandwidthBuilderSpecification extends Specification {

    static Instant firstRefillTime = ZonedDateTime.now()
            .truncatedTo(ChronoUnit.HOURS)
            .plus(1, ChronoUnit.HOURS)
            .toInstant();

    @Unroll
    def "#i test BandwidthBuilder"(int i, Bandwidth oldStyle, Bandwidth newStyle) {
        expect:
            ComparableByContent.equals(oldStyle, newStyle)
        where:
        [i, oldStyle, newStyle] << [
            [1,
             Bandwidth.simple(10, Duration.ofHours(1)),
             Bandwidth.builder().capacity(10).refillGreedy(10, Duration.ofHours(1)).build()
            ],
            [2,
             Bandwidth.classic(20, Refill.greedy(10, Duration.ofHours(1))),
             Bandwidth.builder().capacity(20).refillGreedy(10, Duration.ofHours(1)).build()
            ],
            [3,
             Bandwidth.classic(20, Refill.intervally(10, Duration.ofHours(1))),
             Bandwidth.builder().capacity(20).refillIntervally(10, Duration.ofHours(1)).build()
            ],
            [4,
             Bandwidth.classic(20, Refill.intervallyAligned(10, Duration.ofHours(1), firstRefillTime, false)),
             Bandwidth.builder().capacity(20).refillIntervallyAligned(10, Duration.ofHours(1), firstRefillTime).build()
            ],
            [5,
             Bandwidth.classic(20, Refill.intervallyAligned(10, Duration.ofHours(1), firstRefillTime, true)),
             Bandwidth.builder().capacity(20).refillIntervallyAlignedWithAdaptiveInitialTokens(10, Duration.ofHours(1), firstRefillTime).build()
            ],
            [6,
             Bandwidth.simple(10, Duration.ofHours(1)).withId("x.y.z"),
             Bandwidth.builder().capacity(10).refillGreedy(10, Duration.ofHours(1)).id("x.y.z").build()
            ],
            [7,
             Bandwidth.simple(10, Duration.ofHours(1)).withInitialTokens(5),
             Bandwidth.builder().capacity(10).refillGreedy(10, Duration.ofHours(1)).initialTokens(5).build()
            ],
        ]
    }



}