/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2026 Vladimir Bukhtoyarov
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package io.github.bucket4j.grid.ignite3;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.table.KeyValueView;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.MountableFile;

import io.github.bucket4j.distributed.jdbc.BucketTableSettings;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.github.bucket4j.tck.ProxyManagerSpec;

/**
 * Integration test for {@link Bucket4jIgnite3}, covering both the CAS-based and the
 * colocated-compute-based proxy managers against a real Apache Ignite 3.x cluster node
 * started via Testcontainers.
 *
 * <p>
 * The compute-based proxy manager runs {@link io.github.bucket4j.grid.ignite3.compute.Bucket4jIgnite3ComputeJob}
 * on the server node, so this module (together with bucket4j-core) is packaged as a shaded
 * "server-job" jar by the build (see pom.xml) and copied into the container's classpath
 * directory ({@code /opt/ignite/lib}) before the node starts.
 */
public class Ignite3Test extends AbstractDistributedBucketTest {

    private static final int REST_PORT = 10300;
    private static final int CLIENT_PORT = 10800;
    private static final String NODE_NAME = "defaultNode";

    private static GenericContainer<?> container;
    private static IgniteClient client;

    @BeforeAll
    public static void setup() throws IOException, InterruptedException {
        Path serverJobJar = locateServerJobJar();

        // The stock apacheignite/ignite:3.1.0 image bundles a JDK 11 JRE, but this module's
        // compute-job server-side jar is compiled to JDK 17 class files (see pom.xml), so the
        // node process can't load it. Build a variant image with the bundled JRE swapped for
        // JDK 17 (see src/test/resources/ignite3-jdk17/Dockerfile) to run the compute-based spec.
        ImageFromDockerfile image = new ImageFromDockerfile("bucket4j-ignite3-jdk17-test", false)
            .withDockerfile(Paths.get("src/test/resources/ignite3-jdk17/Dockerfile"));

        container = new GenericContainer<>(image)
            .withExposedPorts(REST_PORT, CLIENT_PORT)
            .withCopyFileToContainer(MountableFile.forHostPath(serverJobJar), "/opt/ignite/lib/" + serverJobJar.getFileName())
            .waitingFor(Wait.forListeningPorts(REST_PORT));
        container.start();

        // The thin-client port only starts accepting connections once the cluster has been initialized,
        // so cluster init has to happen before we can connect - the REST port is enough to wait for upfront.
        initCluster();

        client = connectClient();

        client.sql().executeScript("CREATE TABLE IF NOT EXISTS bucket_cas (id VARCHAR PRIMARY KEY, state VARBINARY)");
        client.sql().executeScript("CREATE TABLE IF NOT EXISTS bucket_compute (id VARCHAR PRIMARY KEY, state VARBINARY(65500))");

        KeyValueView<String, byte[]> casKeyValueView = client.tables().table("bucket_cas").keyValueView(String.class, byte[].class);
        BucketTableSettings computeTableSettings = BucketTableSettings.customSettings("bucket_compute", "id", "state");

        specs = List.of(
            new ProxyManagerSpec<>(
                "IgniteKeyValueCasBasedProxyManager",
                () -> UUID.randomUUID().toString(),
                () -> Bucket4jIgnite3.INSTANCE.casBasedBuilder(casKeyValueView)
            ),
            new ProxyManagerSpec<>(
                "IgniteComputeProxyManager",
                () -> UUID.randomUUID().toString(),
                () -> Bucket4jIgnite3.INSTANCE.computeBasedBuilder(client, computeTableSettings)
            )
        );
    }

    @AfterAll
    public static void shutdown() throws Exception {
        if (client != null) {
            client.close();
        }
        if (container != null) {
            container.stop();
        }
    }

    private static Path locateServerJobJar() throws IOException {
        Path targetDir = Paths.get("target");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(targetDir, "*-server-job.jar")) {
            for (Path path : stream) {
                return path;
            }
        }
        throw new IllegalStateException("Server job jar was not found in " + targetDir.toAbsolutePath() +
            " - it should have been built by the maven-shade-plugin execution bound to process-test-classes");
    }

    private static void initCluster() throws IOException, InterruptedException {
        String restUrl = "http://" + container.getHost() + ":" + container.getMappedPort(REST_PORT) + "/management/v1/cluster/init";
        String body = "{\"metaStorageNodes\":[\"" + NODE_NAME + "\"],\"clusterName\":\"bucket4j-test-cluster\"}";
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create(restUrl))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        IOException lastError = null;
        for (int attempt = 0; attempt < 60; attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return;
                }
                lastError = new IOException("Cluster init failed with status " + response.statusCode() + ": " + response.body());
            } catch (IOException e) {
                lastError = e;
            }
            Thread.sleep(1000);
        }
        throw new IllegalStateException("Failed to initialize Ignite 3 cluster", lastError);
    }

    private static IgniteClient connectClient() throws InterruptedException {
        String address = container.getHost() + ":" + container.getMappedPort(CLIENT_PORT);
        RuntimeException lastError = null;
        for (int attempt = 0; attempt < 60; attempt++) {
            try {
                return IgniteClient.builder()
                    .addresses(address)
                    .build();
            } catch (RuntimeException e) {
                lastError = e;
            }
            Thread.sleep(1000);
        }
        throw new IllegalStateException("Failed to connect Ignite 3 thin client to " + address, lastError);
    }

}
