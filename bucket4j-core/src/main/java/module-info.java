/**
 * Explicit module descriptor for Bucket4j core.
 *
 * <p>Upstream ships this artifact as an automatic module, which the {@code jlink}
 * tool refuses to link. Declaring the descriptor here makes the jar a first-class
 * named module so it can be resolved on the module path and included in a
 * custom runtime image.
 *
 * <p>The core artifact is self-contained: apart from {@code java.base} it only
 * uses {@code java.sql} (the JDBC proxy APIs), so no other module dependency is
 * declared.
 */
module io.github.bucket4j.core {
    requires java.sql;

    exports io.github.bucket4j;
    exports io.github.bucket4j.distributed;
    exports io.github.bucket4j.distributed.expiration;
    exports io.github.bucket4j.distributed.jdbc;
    exports io.github.bucket4j.distributed.proxy;
    exports io.github.bucket4j.distributed.proxy.generic.compare_and_swap;
    exports io.github.bucket4j.distributed.proxy.generic.pessimistic_locking;
    exports io.github.bucket4j.distributed.proxy.generic.select_for_update;
    exports io.github.bucket4j.distributed.proxy.optimization;
    exports io.github.bucket4j.distributed.proxy.optimization.batch;
    exports io.github.bucket4j.distributed.proxy.optimization.delay;
    exports io.github.bucket4j.distributed.proxy.optimization.manual;
    exports io.github.bucket4j.distributed.proxy.optimization.predictive;
    exports io.github.bucket4j.distributed.proxy.optimization.skiponzero;
    exports io.github.bucket4j.distributed.remote;
    exports io.github.bucket4j.distributed.remote.commands;
    exports io.github.bucket4j.distributed.serialization;
    exports io.github.bucket4j.distributed.versioning;
    exports io.github.bucket4j.local;
    exports io.github.bucket4j.util;
    exports io.github.bucket4j.util.concurrent.batch;
}
