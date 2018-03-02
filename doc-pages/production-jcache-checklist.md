# Production checklist especially related to distributed usage scenario
Before using Bucket4j in clustered scenario you need to understand, agree and configure following points:

#### Do not forget about exception handling
Since you choose to go to distributed world, you need to keep and mind that any check-limit request can cross the border of current JVM.
Thus each request to bucket can lead to network communication.
There is no way to avoid failures when having deal with network IO, so you must to be prepared to get unchecked exception on any interaction with bucket. 
**So, it is your responsibility to handle(or ignore) exceptions:**
* Most likely you don not want to fail business transaction if grid that responsible for throttling is crashed.
If so then just log exception and continue business transaction without throttling.
* If business transaction should be failed when grid grid that responsible for throttling is crashed, then just do not catch exceptions,
and let it reach the top level(or log and rethrow).

#### Do not forget to configure backups
If state of any bucket should survive the restart/crash of grid node that holds its state, then you need to configure backups,
in way which specific for particular grid vendor. See how to [configure backups for Apache Ignite](https://apacheignite.readme.io/v2.3/docs/primary-and-backup-copies) as example.  

#### Retention tuning is your responsibility
When having deal with multi-tenant scenarios like bucket per user or bucket per IP address, 
amount of buckets in the cache will continuously increase, because new bucket will be created each time when new key detected. 
To prevent exhausting of available memory of cluster you need to configure following points:
* **Expiration since last access** - in order to allow grid to remove the buckets which was unused for long time. See how to [configure expiration policy for Apache Ignite](https://apacheignite.readme.io/docs/expiry-policies) as example.
* **Maximum cache size(in units or bytes)** - it is obvious, that better to miss the state of several buckets in case of cache overflow than lose the cluster at all. 
Grid will evict the last used data firstly in case of overflow, so usually even mistake in choosing capacity should not be a reason of eviction the meaningful data.

#### High availability(HA) tuning and testing is your responsibility
There are no special settings for HA supported by Bucket4j, because Bucket4j does nothing more that just invoking EntryProcessors on the cache.
Instead Bucket4j relies that You will configure the cache with proper parameters that control redundancy and high availability. 
Of course you may regret for this pure behavior, but as experienced developer in distributed domain the author of Bucket4j argues
that nobody helps you if you did not simulate crash scenarios and did not figure out how to your system behaves in case of different types of outages.    

#### Verification of compatibility with JCache provider is your responsibility
Keep in mind that there are many non-certified implementations of JCache specification on the market.
Many of theme want to increase own popularity by declaring support of JCache API,
but often only API is supported and semantic of JCache is totally ignored.
Usage Bucket4j with this kind of libraries should be totally prevented.
Because Bucket4j is compatible only with implementations which obey the JCache specification rules(especially related to EntryProcessor execution).
The Oracle Coherence, Apache Ignite, Hazelcast are good examples of well implementations of JCache.

Because it is impossible to test all possible JCache provider, you need to test your provider by yourself.
Just run this code in order to be sure that your implementation of JCache provides good isolation for EntryProcessors
```java
import javax.cache.Cache;
import javax.cache.processor.EntryProcessor;
import java.util.concurrent.CountDownLatch;

public class CompatibilityTest {

    final Cache<String, Integer> cache;


    public CompatibilityTest(Cache<String, Integer> cache) {
        this.cache = cache;
    }

    public void test() throws InterruptedException {
        String key = "42";
        int threads = 4;
        int iterations = 1000;
        cache.put(key, 0);
        CountDownLatch latch = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                try {
                    for (int j = 0; j < iterations; j++) {
                        EntryProcessor<String, Integer, Void> processor = (mutableEntry, objects) -> {
                            int value = mutableEntry.getValue();
                            mutableEntry.setValue(value + 1);
                            return null;
                        };
                        cache.invoke(key, processor);
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        latch.await();
        int value = cache.get(key);
        if (value == threads * iterations) {
            System.out.println("Implementation which you use is compatible with Bucket4j");
        } else {
            String msg = "Implementation which you use is not compatible with Bucket4j";
            msg += ", " + (threads * iterations - value) + " writes are missed";
            throw new IllegalStateException(msg);
        }
    }

}
```
The check does 4000 increments of integer in parallel and verifies that no one update has been missed.
If check passed then your JCache provider is compatible with Bucket4j, the throttling will wotk fine in distributed and concurrent environment. 
If check is not passed, then reach to the particular JCache provider team and consult why its implementation misses the writes.  