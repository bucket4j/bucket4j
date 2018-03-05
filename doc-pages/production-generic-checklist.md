# Generic production checklist
The thoughts, described bellow, are applicable to each solution based on token-bucket or leaky-bucket algorithm.
You need to understand, agree and configure following points:

## Be careful when having deals with long periods
When you are planning to use any solution based on token-bucket for throttling incoming requests,
you should to pay high attention to choose appropriate throttling time window.

Lets describe the example of dangerous configuration:
* Given the per-user limitation 10000 tokens/ per 1 hour.
* As a hacker I am able to send 9999 request in very short period, for example at first 10 seconds, and potentially 100RPS can damage or slowdown any part of your service.
* Then as good hacker I stop to sending, and bucket is unable to catch me, because limit was not reached.
* Then I will be reproduce this attack each hour.

To protect from this kind attacks, you should specify multiple limits [as describe there](https://github.com/vladimir-bukhtoyarov/bucket4j/blob/master/doc-pages/advanced-usage.md#example-of-multiple-bandwidth).
From performance perspective it does not matter how many limits specified per single bucket.
```java
Bucket bucket = Bucket4j.jCacheBuilder(RecoveryStrategy.RECONSTRUCT)
    .addLimit(Bandwidth.simple(10000, Duration.ofSeconds(3_600))
    .addLimit(Bandwidth.simple(20, Duration.ofSeconds(1)) // hacker is unable to achieve 1000RPS and crash service in short time
    .build(cache, bucketId);
```

## Token bucket algorithms defines its contract in long terms, you can not avoid the short-timed bursts
Token bucket is effective algorithm with low and fixed memory footprint, independently of incoming request-rate(it can be millions per second) the bucket consumes no more then 40 bytes(five longs).
But effective memory footprint has its own cost - bandwidth limitation is satisfied properly only when we are thinking of it in long run terms,
in other words you can not avoid the short-timed bursts.

Lets describe the example of local burst:
* Given the per-user limitation 100 tokens/ per 1 minute.
* At moment of time ```T1``` the bucket is full.
* All 100 tokens consumed from bucket, the bucket is empty.
* At moment of time ```T1+1min``` the bucket is full again because tokens fully regenerated.
* All 100 tokens consumed from bucket. And we are giving the point where 200 tokens consumed during 1 minutes, but bucket configured by 100 tokens/minute. This situation called as burst.

And there is no way to avoid the local short-timed bursts. If local bursts are unacceptable for your case then you have two alternatives:
* Do not use Bucket4j or any other solution implemented on top of token-bucket ideas.
* Try to change the bandwidth from ```Tokens per Time``` to ```Tokens/2 per Time/2```,
for example if you need to strongly satisfy the limit 100tokens/60seconds,
then you need to define the bandwidth as ```50tokens/30seconds```, and desired original limitation will be satisfied as well independently of local bursts.