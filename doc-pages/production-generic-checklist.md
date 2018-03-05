# Generic production checklist
The considerations described bellow are applicable to each solution based on the token-bucket or leaky-bucket algorithm.
You need to understand, agree and configure following points:

## Be wary of long periods
When you are planning to use any solution based on token-bucket for throttling incoming requests,
you need to pay close attention to the throttling time window.

Lets describe an example of a dangerous configuration:
* Given a bucket with a limit of 10000 tokens/ per 1 hour per user.
* A malicious attacker may send 9999 request in very short period, for example within 10 seconds. This would correspond to 100 request per second which could seriously impact your system.
* A skilled attacker could stop at 9999 request per hour, and repeat every hour, which would make this attack impossible to detect (because the limit would not be reached).

To protect from this kind attacks, you must specify multiple limits [as describe there](https://github.com/vladimir-bukhtoyarov/bucket4j/blob/master/doc-pages/advanced-usage.md#example-of-multiple-bandwidth).
The number of limits specified per bucket does not impact the performance.

For example:
```java
Bucket bucket = Bucket4j.jCacheBuilder(RecoveryStrategy.RECONSTRUCT)
    .addLimit(Bandwidth.simple(10000, Duration.ofSeconds(3_600))
    .addLimit(Bandwidth.simple(20, Duration.ofSeconds(1)) // attacker is unable to achieve 1000RPS and crash service in short time
    .build(cache, bucketId);
```

## Be wary of short-term bursts
Token bucket is an efficient algorithm with low and fixed memory footprint, independently of the incoming request-rate(it can be millions per second) the bucket consumes no more then 40 bytes(five longs).
But an efficient memory footprint has its own cost - bandwidth limitation is only satisfied over a long period of time. In other words you cannot avoid short-timed bursts.

Let us describe an example of local burst:
* Given a bucket with a limit of 100 tokens/min. We start with a full bucket, i.e. with 100 tokens.
* At ```T1``` 100 requests are made and thus the bucket becomes empty.
* At ```T1+1min``` the bucket is full again because tokens fully regenerated and we can immediately consume 100 tokens.
* This means that between  ```T1``` and ```T1+1min``` we have consumed 200 tokens. Over a long period of time there will be no more than 100 requests per min, but as shown above it is possible to burst at **twice the limit** here at 100 tokens per min.

These bursts are inherent to token bucket algorithms and cannot be avoided. If local bursts are unacceptable you then have two options:
* Do not use Bucket4j or any other solution implemented on top of token-bucket algorithms.
* Try to change the bandwidth from ```Tokens per Time``` to ```Tokens/2 per Time/2```,
for example if you need to strongly satisfy the limit 100tokens/60seconds,
then you need to define the bandwidth as ```50tokens/30seconds```, and desired original limitation will be satisfied.
