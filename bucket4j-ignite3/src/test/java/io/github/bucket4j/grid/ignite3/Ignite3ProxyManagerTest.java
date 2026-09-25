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

import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.github.bucket4j.tck.ProxyManagerSpec;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteServer;
import org.apache.ignite.InitParameters;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;

/**
 * Runs the bucket4j TCK suite against a single embedded (same-JVM) Apache Ignite 3.x node backed by
 * {@link Ignite3ProxyManager}'s request-batching (coalescing) pattern (see issue #626).
 */
public class Ignite3ProxyManagerTest extends AbstractDistributedBucketTest {

    private static final String TABLE_NAME = "bucket4j_test_buckets";

    private static IgniteServer server;
    private static Ignite ignite;

    @BeforeAll
    public static void setup() throws Exception {
        Path workDir = Files.createTempDirectory("bucket4j-ignite3-work");
        Path configPath = Files.createTempFile("bucket4j-ignite3-config", ".conf");
        Files.writeString(configPath, """
                ignite {
                  network.port: 3344
                  network.nodeFinder.netClusterNodes: [ "localhost:3344" ]
                }
                """);

        server = IgniteServer.start("bucket4j-ignite3-test-node", configPath, workDir);
        server.initCluster(InitParameters.builder()
                .metaStorageNodeNames("bucket4j-ignite3-test-node")
                .clusterName("bucket4j-ignite3-test-cluster")
                .build());
        ignite = server.api();

        ignite.sql().executeScript(
                "CREATE TABLE " + TABLE_NAME + " (\"key\" VARCHAR PRIMARY KEY, \"value\" VARBINARY)");

        specs = Arrays.asList(
                new ProxyManagerSpec<>(
                        "Ignite3ProxyManager",
                        () -> UUID.randomUUID().toString(),
                        () -> Bucket4jIgnite3.<String>builder().ignite(ignite).table(TABLE_NAME).keyType(String.class)
                )
        );
    }

    @AfterAll
    public static void shutdown() {
        if (server != null) {
            server.shutdown();
        }
    }

}
