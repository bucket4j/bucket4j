import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.ProxyManager;

import java.time.Duration;

public class HazelcastTest {

    /**
     *  docker build -t bucket4j .
     *  docker run -d -it --rm --network=host bucket4j
     */
    public static void main(String[] args) {
        // start hazelcast client which works inside current JVM and does not hold data
        Config config = new Config();
        config.setLiteMember(true);
        JoinConfig joinConfig = config.getNetworkConfig().getJoin();
        joinConfig.getMulticastConfig().setEnabled(true);
        joinConfig.getTcpIpConfig().setEnabled(false);
        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(config);

        IMap<String, GridBucketState> map = hazelcastInstance.getMap("buckets");
        ProxyManager<String> proxyManager = Bucket4j.extension(io.github.bucket4j.grid.hazelcast.Hazelcast.class).proxyManagerForMap(map);

        BucketConfiguration bucketConfiguration = Bucket4j.configurationBuilder()
                .addLimit(Bandwidth.simple(100, Duration.ofMinutes(1)))
                .build();
        Bucket bucket = proxyManager.getProxy("42", bucketConfiguration);
        System.out.println(bucket.tryConsume(10));
    }

}
