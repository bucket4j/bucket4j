package io.github.bucket4j.grid.geode;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.RegionShortcut;
import org.apache.geode.cache.server.CacheServer;

/**
 * Standalone process (run in a Docker container by the Testcontainers-based tests) that hosts a real
 * Geode peer {@link Cache} with a {@link CacheServer}, so that tests running in a different JVM can
 * connect to it as a genuine {@code ClientCache} over the network - something that is impossible to
 * exercise in-process, since a peer {@code Cache} and a {@code ClientCache} cannot coexist in the same JVM.
 */
public class GeodeServerMain {

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(args[0]);
        String regionName = args[1];

        Cache cache = new CacheFactory()
            .set("mcast-port", "0")
            .set("locators", "")
            .set("log-level", "warning")
            .create();
        cache.<Object, byte[]>createRegionFactory(RegionShortcut.PARTITION).create(regionName);

        CacheServer cacheServer = cache.addCacheServer();
        cacheServer.setPort(port);
        cacheServer.start();

        System.out.println("GEODE_SERVER_READY");
        Thread.sleep(Long.MAX_VALUE);
    }

}
