/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2022 Vladimir Bukhtoyarov
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
package com.bucket4j.backward_compatibility;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.TokensInheritanceStrategy;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.grid.hazelcast.HazelcastProxyManager;

import java.io.IOException;
import java.time.Duration;

public class HazelcastScenarios {

    public static class StartServerNode {
        public static void main(String[] args) throws InterruptedException {
            Config config = new Config();
            config.getNetworkConfig().setPublicAddress("127.0.0.1");
            config.getNetworkConfig().setPort(5701);

            JoinConfig joinConfig = config.getNetworkConfig().getJoin();
            joinConfig.getMulticastConfig().setEnabled(false);
            joinConfig.getTcpIpConfig().setEnabled(true);

//            joinConfig.getTcpIpConfig().addMember("127.0.0.1:5702,127.0.0.1:5703");
            config.setLiteMember(false);
            HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(config);
            hazelcastInstance.getMap("my_buckets");
            Thread.currentThread().join();
        }
    }

    public static class ImplicitConfigurationReplacementCases {
        public static class CreateBucketWithoutImplicitConfigReplacement {
            public static void main(String[] args) throws IOException {
                HazelcastInstance hazelcastInstance = createLiteMemberHazelcastInstance();
                try {
                    IMap<Long, byte[]> map = hazelcastInstance.getMap("my_buckets");
                    ProxyManager<Long> proxyManager = new HazelcastProxyManager<>(map, ClientSideConfig.getDefault());
                    proxyManager.removeProxy(666L);

                    BucketConfiguration config = BucketConfiguration.builder()
                            .addLimit(Bandwidth.simple(3, Duration.ofSeconds(60)))
                            .build();
                    BucketProxy bucket = proxyManager.builder().build(666L, config);
                    System.out.println("Available tokens: " + bucket.asVerbose().getAvailableTokens().getValue());;
                } finally {
                    hazelcastInstance.shutdown();
                    System.out.println("Hazelcast has been stopped");
                    System.exit(0);
                }
            }
        }

        public static class CreateBucketWithImplicitConfigReplacement {
            public static void main(String[] args) throws IOException {
                HazelcastInstance hazelcastInstance = createLiteMemberHazelcastInstance();
                try {
                    IMap<Long, byte[]> map = hazelcastInstance.getMap("my_buckets");
                    ProxyManager<Long> proxyManager = new HazelcastProxyManager<>(map, ClientSideConfig.getDefault());
                    proxyManager.removeProxy(666L);

                    BucketConfiguration config = BucketConfiguration.builder()
                            .addLimit(Bandwidth.simple(3, Duration.ofSeconds(60)))
                            .build();
                    BucketProxy bucket = proxyManager.builder()
                            .withImplicitConfigurationReplacement(1, TokensInheritanceStrategy.AS_IS)
                            .build(666L, config);
                    System.out.println("Available tokens: " + bucket.asVerbose().getAvailableTokens().getValue());;
                } catch (Throwable t) {
                    t.printStackTrace();
                } finally {
                    hazelcastInstance.shutdown();
                    System.out.println("Hazelcast has been stopped");
                    System.exit(0);
                }
            }
        }

        public static class CreateBucketWithFlexibleExpiration {
            public static void main(String[] args) throws IOException {
                HazelcastInstance hazelcastInstance = createLiteMemberHazelcastInstance();
                try {
                    IMap<Long, byte[]> map = hazelcastInstance.getMap("my_buckets");
                    ProxyManager<Long> proxyManager = new HazelcastProxyManager<>(map, ClientSideConfig.getDefault().withExpirationAfterWriteStrategy(ExpirationAfterWriteStrategy.fixedTimeToLive(Duration.ofSeconds(10))));
                    proxyManager.removeProxy(666L);

                    BucketConfiguration config = BucketConfiguration.builder()
                            .addLimit(Bandwidth.simple(3, Duration.ofSeconds(60)))
                            .build();
                    BucketProxy bucket = proxyManager.builder().build(666L, config);
                    System.out.println("Available tokens: " + bucket.asVerbose().getAvailableTokens().getValue());;
                } catch (Throwable t) {
                    t.printStackTrace();
                } finally {
                    hazelcastInstance.shutdown();
                    System.out.println("Hazelcast has been stopped");
                    System.exit(0);
                }
            }
        }
    }

    private static HazelcastInstance createLiteMemberHazelcastInstance() throws IOException {
        // start hazelcast client which works inside current JVM and does not hold data
        Config config = new Config();
        config.setLiteMember(true);
        JoinConfig joinConfig = config.getNetworkConfig().getJoin();
        joinConfig.getMulticastConfig().setEnabled(false);
        joinConfig.getTcpIpConfig().setEnabled(true);
        joinConfig.getTcpIpConfig().addMember("127.0.0.1:5701");
        return Hazelcast.newHazelcastInstance(config);
    }

}
