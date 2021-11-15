# Listening for bucket events

### What can be listened:
You can decorate the bucket by listener in order to track following events:
- When tokens consumed from bucket.
- When consumption requests was rejected by bucket.
- When thread was parked for wait of tokens refill in result of interaction with ```BlockingBucket```.
- When thread was interrupted during the wait of tokens refill  in result of interaction with ```BlockingBucket```.
- When delayed task was submit to ```ScheduledExecutorService``` in result of interaction with ```AsyncScheduledBucket```.

### Corner cases:
**Question:** How many listeners is need to create in case of application uses many buckets?  
**Answer:**  it depends:
- If you want to have aggregated statistics for all buckets then create single listener per application and reuse this listener for all buckets.
- If you want to measure statistics independently per each bucket then use listener per bucket model.

**Question:** where is methods of listener are invoking in case of distributed usage?  
**Answer:** listener always invoked on client side, it is means that each client JVM will have own totally independent stat for same bucket.

**Question:** Why does bucket invoke the listener on client side instead of server side in case of distributed scenario? What I need to do if I need in aggregated stat across the whole cluster?  
**Answer:** Because of planned expansion to non-JVM back-ends such as Redis, MySQL, PostgreSQL.
It is not possible to serialize and invoke listener on this non-java back-ends, so it was decided to invoke listener on client side,
in order to avoid inconsistency between different back-ends in the future.
You can do post-aggregation of monitoring statistics via features built-into your monitoring database or via mediator(like StatsD) between your application and monitoring database.

### How to attach listener to bucket?
The bucket can be decorated by listener via ```toListenable``` method.
```java
BucketListener listener = new MyCoolListener();

Bucket bucket = Bucket4j.builder()
                    .addLimit(Bandwidth.simple(100, Duration.ofMinutes(1)))
                    .build()
                    .toListenable(listener);
```

### Example of integration with Dropwizard metrics-core
[SimpleBucketListener](https://github.com/vladimir-bukhtoyarov/bucket4j/blob/4.0/bucket4j-core/src/main/java/io/github/bucket4j/SimpleBucketListener.java) is simple implementation of ```BucketListener``` that available out of the box.
It is example of exposing statistics via Dropwizard Metrics(for Micrometer it should be quite similar):
```java
public static Bucket buildMonitoredBucket(Bandwidth limit, String bucketName, MetricRegistry registry) {
  SimpleBucketListener stat = new SimpleBucketListener(); 
  registry.register(name + ".consumed", (Gauge<Long>) stat::getConsumed);
  registry.register(name + ".rejected", (Gauge<Long>) stat::getRejected);
  registry.register(name + ".parkedNanos", (Gauge<Long>) stat::getParkedNanos);
  registry.register(name + ".interrupted", (Gauge<Long>) stat::getInterrupted);
  registry.register(name + ".delayedNanos", (Gauge<Long>) stat::getDelayedNanos);
  
  return Bucket4j.builder().addLimit(limit).build().toListenable(stat);
  
}
```
