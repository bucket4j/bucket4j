# Advanced usage examples

### Example of multiple bandwidth

Imagine that you are developing load testing tool, in order to be ensure that testable system is able to dispatch 1000 requests per 1 minute.
But you do not want to randomly kill the testable system by generation all 1000 events in one second instead of 1 minute. 
To solve problem you can construct following bucket:
```java
Bucket bucket = Bucket4j.builder()
       // allows 1000 tokens per 1 minute
       .addLimit(Bandwidth.simple(1000, Duration.ofMinutes(1)))
       // but not often then 50 tokens per 1 second
       .addLimit(Bandwidth.simple(50, Duration.ofSeconds(1)))
       .build();

// ...
while (true) {
  // Consume a token from the token bucket.  If a token is not available this method will block until
  // the refill adds one to the bucket.
  bucket.consume(1);

  workloadExecutor.execute(new LoadTask());
}
```

### Using initial capacity  

By default initial size of bucket is equal to capacity. 
But sometimes, you may want to have lesser initial size, for example for case of cold start in order to prevent denial of service: 

```java
int initialCapacity = 42;
Bandwidth limit = Bandwidth.simple(1000, Duration.ofHours(1));
Bucket bucket = Bucket4j.builder()
    .addLimit(initialCapacity, limit)
    .build();
```

### Using dynamic capacity  

Sometimes, you may want to have a bucket with dynamic capacity. For example if you want to have capacity 10 per 1 minute for daily time,
and 2 per minute for nightly time, then construct bucket like this

```java
Capacity dynamicCapacity = new Capacity() {
    @Override
    public long getValue(long currentTime) {
        int hour = LocalTime.now().getHour();
        if (hour >= 7 && hour <= 23) {
            return 10;    
        } else {
            return 2;
        }
    }
};
Refill refill = ...;
Bandwidth limit = Bandwidth.classic(dynamicCapacity, refill)
Bucket bucket = Bucket4j.builder()
    .addLimit(limit)
    .build();
```

### Customizing time measurement
#### Choosing nanotime time resolution
By default Bucket4j uses millisecond time resolution, it is preferred time measurement strategy. 
But rarely(for example benchmarking) you wish the nanosecond precision:
``` java
Bucket4j.builder().withNanosecondPrecision()
```
Be very careful to choose this time measurement strategy, because ```System.nanoTime()``` produces inaccurate results, 
use this strategy only if period of bandwidth is too small that millisecond resolution will be undesired.
   
#### Specify custom time measurement strategy
You can specify your custom time meter, if existing miliseconds or nanotime time meters is not enough for your purposes. For example:

```java
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class CurrentThreadCpuTimeMeter implements TimeMeter {

    private static final ThreadMXBean THREAD_MX_BEAN = ManagementFactory.getThreadMXBean();

    @Override
    public long currentTimeNanos() {
        return THREAD_MX_BEAN.getCurrentThreadCpuTime();
    }

}

Bandwidth limit = Bandwidth.simple(100, Duration.ofMinutes(1));
Bucket bucket = Bucket4j.builder()
                .withCustomTimePrecision(new CurrentThreadCpuTimeMeter())
                .addLimit(limit)
                .build();
```