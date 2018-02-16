# Advanced usage examples

### Example of multiple bandwidth

Imagine that you are developing load testing tool, in order to be ensure that testable system is able to dispatch 1000 requests per 1 minute.
But you do not want to randomly kill the testable system by generation all 1000 events in one second instead of 1 minute. 
To solve problem you can construct following bucket:
```java
static final long MAX_WAIT_NANOS = TimeUnit.HOURS.toNanos(1);
// ...

Bucket bucket = Bucket4j.builder()
       // allows 1000 tokens per 1 minute
       .addLimit(Bandwidth.simple(1000, Duration.ofMinutes(1)))
       // but not often then 50 tokens per 1 second
       .addLimit(Bandwidth.simple(50, Duration.ofSeconds(1)))
       .build();

// ...
while (true) {
  // Consume a token from the token bucket.  If a token is not available this method will block until the refill adds one to the bucket.
  if (bucket.tryConsume(1, MAX_WAIT_NANOS, BlockingStrategy.PARKING)) {
       workloadExecutor.execute(new LoadTask());
  };
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

### Returning tokens back to bucket.
The [compensating transaction](https://en.wikipedia.org/wiki/Compensating_transaction) is one of obvious use case when you want to return tokens back to bucket:
```java
Bucket wallet;
...
wallet.tryConsume(50); // get 50 cents from wallet
try {
    buyCocaCola();
} catch(NoCocaColaException e) {
    // return money to wallet
    wallet.addTokens(50);
}
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
You can specify your custom time meter, if existing miliseconds or nanotime time meters is not enough for your purposes.
Imagine that you have a clock, which synchronizes its time with other machines in current cluster,
if you want to use time provided by this clock instead of time provided by JVM then you can write something like this:

```java
public class ClusteredTimeMeter implements TimeMeter {

    @Override
    public long currentTimeNanos() {
        return ClusteredClock.currentTimeMillis() * 1_000_000;
    }

}

Bandwidth limit = Bandwidth.simple(100, Duration.ofMinutes(1));
Bucket bucket = Bucket4j.builder()
                .withCustomTimePrecision(new ClusteredTimeMeter())
                .addLimit(limit)
                .build();
```

### Change configuration of bucket on the fly
Sometimes you want to change configuration for already created bucket.
For example because client upgrade its service plan and you want to provide additional tokens for him.
If you do not want to lose information about already consumed tokens,
then you can reconfigure bucket on the fly instead of creation new instance of bucket:
```java
Bucket bucket = ...


Bandwidth newLimit = Bandwidth.simple(newCapacity, Duration.ofMinutes(1));
BucketConfiguration newConfiguration = Bucket4j.configurationBuilder()
                .addLimit(newLimit)
                .buildConfiguration();
bucket.replaceConfiguration(newConfiguration)
```
