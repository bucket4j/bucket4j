package io.github.bucket4j.grid.ignite;

import io.github.bucket4j.grid.ignite.thick.Bucket4jIgniteThick;
import io.github.bucket4j.grid.ignite.thin.Bucket4jIgniteThin;

/**
 * Entry point for Apache Ignite integration
 */
public class Bucket4jIgnite {

    /**
     * @return entry points for Ignite integration based on top of Thick client.
     */
    public static Bucket4jIgniteThick thick() {
        return Bucket4jIgniteThick.INSTANCE;
    }

    /**
     * @return entry points for Ignite integration based on top of Thin client.
     */
    public static Bucket4jIgniteThin thin() {
        return Bucket4jIgniteThin.INSTANCE;
    }

}
