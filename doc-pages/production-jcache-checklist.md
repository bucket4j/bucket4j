# Production checklist especially in the context of distributed systems
Before using Bucket4j in clustered scenario you need to understand, agree and configure following points:

#### Do not forget about exception handling
When working within a distributed system, it is innevitable that requests may cross the border of the current JVM, leading to a communication on the network.
Network being unreliable, it is impossible to avoid failures. Thus you should embrace this reality and be ready to get unchecked exceptions when interacting with a distributed bucket.
**It is your responsibility to handle(or ignore) such exceptions:**
* You probably do not want to fail business transactions if the grid responsible for throttling goes down. If this is the case you can simply log the exception and continue your business transaction without throttling
* If you wish to fail your business transaction when the grid responsible for throttling goes down, simply rethrow or don't catch the exception 

#### Do not forget to configure backups
If the state of any bucket should survive the restart/crash of grid node that holds its state, you need to configure backups yourself, in way specific to the particular grid vendor. For example, see how to [configure backups for Apache Ignite](https://apacheignite.readme.io/v2.3/docs/primary-and-backup-copies).  

#### Retention tuning is your responsibility
When dealing with multi-tenant scenarios like a bucket per user or a bucket per IP address, 
the amount of buckets in the cache will continuously increase. This is because a new bucket will be created each time a new key is detected. 
To prevent exhausting the available memory of your cluster you need to configure following aspects:
* **Expiration since last access** - in order to allow the grid to remove the keys which haven't been used in a long time. For example, see how to [configure expiration policy for Apache Ignite](https://apacheignite.readme.io/docs/expiry-policies).
* **Maximum cache size(in units or bytes)** - Obviously it is preferable to lose bucket data than lose the whole cluster due to out of memory exception.

#### High availability(HA) tuning and testing is your responsibility
There are no special settings for HA supported by Bucket4j, because Bucket4j does nothing more that just invoking EntryProcessors on the cache.
Instead Bucket4j relies on *you* to configure the cache with proper parameters that control redundancy and high availability. 

Years of experience working with distributed system has tought the author that High Availability does not come for free. You need to test and verify that your system remains available. This cannot be provided by this or any other library. Your system will most certainly go down if you do not plan for that.
   
#### Verification of compatibility with particular JCache provider is your responsibility
Keep in mind that there are many non-certified implementations of JCache specification on the market.
Many of them want to increase their popularity by declaring support for the JCache API,
but often only the API is supported and the semantic of JCache is totally ignored.
Usage Bucket4j with this kind of libraries should be completely avoided.

Bucket4j is only compatible with implementations which obey the JCache specification rules(especially related to EntryProcessor execution).
Oracle Coherence, Apache Ignite, Hazelcast are good examples of safe implementations of JCache.

Because it is impossible to test all possible JCache providers, you need to test your provider by yourself.
Just run this code in order to be sure that your implementation of JCache provides good isolation for EntryProcessors
```java
import javax.cache.Cache;
import javax.cache.processor.EntryProcessor;
import java.util.concurrent.CountDownLatch;
import java.io.Serializable;

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
                        EntryProcessor<String, Integer, Void> processor = (EntryProcessor<String, Integer, Void> & Serializable) (mutableEntry, objects) -> {
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
If check passed then your JCache provider is compatible with Bucket4j, the throttling will work fine in distributed and concurrent environment. 
If check is not passed, then reach to the particular JCache provider team and consult why its implementation misses the writes.  
