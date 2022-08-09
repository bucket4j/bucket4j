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

                    if (!proxyManager.getProxyConfiguration(666L).isPresent()) {
                        throw new IllegalStateException("Bucket must ber persisted with new version before running this test");
                    }

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
