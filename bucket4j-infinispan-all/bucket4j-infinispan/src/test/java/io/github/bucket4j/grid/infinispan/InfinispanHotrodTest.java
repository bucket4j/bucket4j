package io.github.bucket4j.grid.infinispan;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ClientIntelligence;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.commons.configuration.StringConfiguration;
import org.infinispan.testcontainers.InfinispanContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.grid.infinispan.hotrod.Bucket4jTask;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.github.bucket4j.tck.BackwardCompatibilityRequestCheckHelper;
import io.github.bucket4j.tck.BackwardCompatibilityStateCheckHelper;
import io.github.bucket4j.tck.ProxyManagerSpec;

/*
 * Infinispan 16.1+ server modules are compiled for JDK 25, so a Hot Rod server can no longer be
 * embedded in-process under our JDK 17 build (see bucket4j-infinispan/pom.xml). This test instead
 * runs against a real containerized Infinispan server, deploying the compiled bucket4j-core and
 * bucket4j-infinispan classes into the server's lib directory so that the Bucket4jTask ServerTask
 * is available for the Hot Rod client to invoke.
 */
public class InfinispanHotrodTest extends AbstractDistributedBucketTest {

    private static final String CACHE_NAME = "my-cache";

    private static InfinispanContainer container;
    private static RemoteCacheManager remoteCacheManager;

    @BeforeAll
    public static void init() throws IOException, URISyntaxException {
        Path extensionJar = buildServerExtensionJar();

        container = new InfinispanContainer(DockerImageName.parse("quay.io/infinispan/server:16.2"))
            .withUser(InfinispanContainer.DEFAULT_USERNAME)
            .withPassword(InfinispanContainer.DEFAULT_PASSWORD)
            .withCopyFileToContainer(MountableFile.forHostPath(extensionJar), "/opt/infinispan/server/lib/bucket4j-infinispan-ext.jar");
        container.start();

        ConfigurationBuilder clientConfigBuilder = new ConfigurationBuilder();
        clientConfigBuilder.addServer()
            .host(container.getHost())
            .port(container.getMappedPort(InfinispanContainer.DEFAULT_HOTROD_PORT));
        // The server advertises its container-internal IP as part of its cluster topology, which is
        // unreachable from the host; since it's a single-node container, disable topology-aware
        // routing so the client keeps using the published host/port it was given above.
        clientConfigBuilder.clientIntelligence(ClientIntelligence.BASIC);
        clientConfigBuilder.security().authentication()
            .username(InfinispanContainer.DEFAULT_USERNAME)
            .password(InfinispanContainer.DEFAULT_PASSWORD)
            .realm("default");
        Configuration clientConfig = clientConfigBuilder.build();
        remoteCacheManager = new RemoteCacheManager(clientConfig);

        String cacheXml = "<distributed-cache><encoding media-type=\"application/x-protostream\"/></distributed-cache>";
        remoteCacheManager.administration().getOrCreateCache(CACHE_NAME, new StringConfiguration(cacheXml));
        RemoteCache<String, byte[]> remoteCache = remoteCacheManager.getCache(CACHE_NAME);

        specs = Arrays.asList(
            new ProxyManagerSpec<>(
                "HotrodInfinispanProxyManager",
                () -> UUID.randomUUID().toString(),
                () -> Bucket4jInfinispan.hotrodClientBasedBuilder(remoteCache)
            ).checkExpiration().checkStateBackwardCompatibility(new BackwardCompatibilityStateCheckHelper<String>() {
                @Override
                public byte[] getRawState(String key) {
                    return remoteCache.get(key);
                }

                @Override
                public void setRawState(String key, byte[] state) {
                    remoteCache.put(key, state);
                }
            }).checkRequestBackwardCompatibility(new BackwardCompatibilityRequestCheckHelper<String>() {
                @Override
                public byte[] execute(String key, byte[] requestBytes) {
                    Map<String, Object> params = new HashMap<>();
                    params.put(Bucket4jTask.KEY_PARAM, key);
                    params.put(Bucket4jTask.REQUEST_PARAM, requestBytes);
                    return remoteCache.execute(Bucket4jTask.TASK_NAME, params, key);
                }

                @Override
                public CompletableFuture<byte[]> executeAsync(String key, byte[] requestBytes) {
                    throw new UnsupportedOperationException();
                }
            })
        );
    }

    @AfterAll
    public static void destroy() {
        try {
            if (remoteCacheManager != null) {
                remoteCacheManager.close();
            }
        } finally {
            if (container != null) {
                container.stop();
            }
        }
    }

    private static Path buildServerExtensionJar() throws IOException, URISyntaxException {
        Path jarFile = Files.createTempFile("bucket4j-infinispan-ext", ".jar");
        jarFile.toFile().deleteOnExit();
        Set<String> addedEntries = new HashSet<>();
        try (JarOutputStream jarOut = new JarOutputStream(Files.newOutputStream(jarFile))) {
            addClasspathEntryToJar(jarOut, classpathEntryOf(Bandwidth.class), addedEntries);
            addClasspathEntryToJar(jarOut, classpathEntryOf(Bucket4jInfinispan.class), addedEntries);
        }
        // Files.createTempFile creates the file with mode rw------- (owner-only), which the
        // container's non-root user cannot read once it's mounted; Infinispan Server then silently
        // skips the jar while scanning its lib directory instead of failing loudly.
        jarFile.toFile().setReadable(true, false);
        return jarFile;
    }

    private static Path classpathEntryOf(Class<?> type) throws URISyntaxException {
        URL location = type.getProtectionDomain().getCodeSource().getLocation();
        return Paths.get(location.toURI());
    }

    /**
     * A dependency's classpath entry is a directory of {@code .class} files when it's built as part
     * of the same reactor, but a packaged jar when resolved from the local repository (e.g. when this
     * test is run standalone via {@code mvn -pl}); both forms need to be merged into the extension jar.
     */
    private static void addClasspathEntryToJar(JarOutputStream jarOut, Path classpathEntry, Set<String> addedEntries) throws IOException {
        if (Files.isDirectory(classpathEntry)) {
            addDirectoryToJar(jarOut, classpathEntry, addedEntries);
        } else {
            addJarToJar(jarOut, classpathEntry, addedEntries);
        }
    }

    private static void addDirectoryToJar(JarOutputStream jarOut, Path directory, Set<String> addedEntries) throws IOException {
        List<Path> files;
        try (Stream<Path> paths = Files.walk(directory)) {
            files = paths.filter(Files::isRegularFile).collect(Collectors.toList());
        }
        for (Path path : files) {
            String entryName = directory.relativize(path).toString().replace(File.separatorChar, '/');
            if (!addedEntries.add(entryName)) {
                continue;
            }
            jarOut.putNextEntry(new JarEntry(entryName));
            Files.copy(path, jarOut);
            jarOut.closeEntry();
        }
    }

    private static void addJarToJar(JarOutputStream jarOut, Path sourceJar, Set<String> addedEntries) throws IOException {
        try (JarFile jarIn = new JarFile(sourceJar.toFile())) {
            Enumeration<JarEntry> entries = jarIn.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory() || !addedEntries.add(entry.getName())) {
                    continue;
                }
                jarOut.putNextEntry(new JarEntry(entry.getName()));
                try (InputStream entryIn = jarIn.getInputStream(entry)) {
                    entryIn.transferTo(jarOut);
                }
                jarOut.closeEntry();
            }
        }
    }

}
