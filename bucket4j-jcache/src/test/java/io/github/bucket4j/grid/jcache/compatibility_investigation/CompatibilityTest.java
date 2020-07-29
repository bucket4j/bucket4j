
package io.github.bucket4j.grid.jcache.compatibility_investigation;

import javax.cache.Cache;
import javax.cache.processor.EntryProcessor;
import java.io.Serializable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

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
        AtomicLong sum = new AtomicLong();
        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                try {
                    for (int j = 0; j < iterations; j++) {
                        EntryProcessor<String, Integer, Void> processor = (EntryProcessor<String, Integer, Void> & Serializable) (mutableEntry, objects) -> {
                            try {
                                Thread.sleep(1);
                            } catch (InterruptedException e1) {
                                throw new IllegalStateException(e1);
                            }
                            int value = mutableEntry.getValue();
                            try {
                                Thread.sleep(1);
                            } catch (InterruptedException e) {
                                throw new IllegalStateException(e);
                            }
                            int increment = ThreadLocalRandom.current().nextInt(10);
                            mutableEntry.setValue(value + increment);
                            sum.addAndGet(increment);
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
        if (value == sum.get()) {
            System.out.println("Implementation which you use is compatible with Bucket4j");
        } else {
            String msg = "Implementation which you use is not compatible with Bucket4j";
            msg += ", " + (sum.get() - value) + " writes are missed";
            throw new IllegalStateException(msg);
        }
    }

}