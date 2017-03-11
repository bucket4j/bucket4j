### Advanced usage

#### Example of multiple bandwidth

Imagine that you are developing load testing tool, in order to be ensure that testable system is able to dispatch 1000 requests per 1 minute.
But you do not want to randomly kill the testable system by generation all 1000 events in one second instead of 1 minute. 
To solve problem you can construct following bucket:
```java
import com.github.bucket4j.Bucket4j;

Bucket bucket = Bucket4j.builder()
       // allows 10000 tokens per 1 minute
       .withLimitedBandwidth(10000, Duration.ofMinutes(1))
       // but not often then 50 tokens per 1 second
       .withLimitedBandwidth(50, Duration.ofSeconds(1))
       .build();

// ...
while (true) {
  // Consume a token from the token bucket.  If a token is not available this method will block until
  // the refill adds one to the bucket.
  bucket.consume(1);

  workloadExecutor.execute(new LoadTask());
}
```

#### Example of guaranteed bandwidth

Let's imagine that you develop mailing server. 
In order to prevent spam, you want to restrict user to send emails not often than by 1000 times per hour. 
But in same time, you want to provide guarantee to user that will be able to send 1 email each 10 minute even in the case where the limit by hour is exceeded. 
In this case you can construct bucket like this:

```java
import com.github.bucket4j.Bucket4j;

Bucket bucket = Bucket4j.builder()
    .withLimitedBandwidth(1000, Duration.ofHours(1))
    .withGuaranteedBandwidth(1, Duration.ofMinutes(10))
    .build();
```

#### Using initial capacity  

By default initial size of bucket is equal to capacity. 
But sometimes, you may want to have lesser initial size, for example for case of cold start in order to prevent denial of service. 
You can specify initial size as third parameter during bandwidth construction:

```java

int initialCapacity = 42;

Bucket bucket = Bucket4j.builder()
    .withLimitedBandwidth(1000, initialCapacity, Duration.ofHours(1))
    .build();
```

#### Using dynamic capacity  

Sometimes, you may want to have a bucket with dynamic capacity. For example if you want to have capacity 10 per 1 minute for daily time,
and 2 per minute for nightly time, then construct bucket like this

```java

BandwidthAdjuster adjuster = new BandwidthAdjuster() {
    @Override
    public long getCapacity(long currentTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(currentTime);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        if (hour >= 7 && hour <= 23) {
            return 10;    
        } else {
            return 2;
        }
    }
};
Bucket4j.builder()
    .withLimitedBandwidth(adjuster, 10, Duration.ofMinutes(1));
```

#### Customizing time measurement
##### Choosing nanotime time resolution
By default Bucket4j uses millisecond time resolution, it is preferred time measurement strategy. 
But rarely(for example benchmarking) you wish the nanosecond precision:
``` java
Bucket4j.builder().withNanosecondPrecision()
```
Be very careful to choose this time measurement strategy, because ```System.nanoTime()``` produces inaccurate results, 
use this strategy only if period of bandwidth is too small that millisecond resolution will be undesired.
   
##### Specify custom time measurement strategy
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

Bucket bucket = Bucket4j.builder()
                .withCustomTimePrecision(new CurrentThreadCpuTimeMeter())
                .withLimitedBandwidth(100, Duration.ofMinutes(1))
                .build();


```