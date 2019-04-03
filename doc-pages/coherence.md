# Oracle Coherence integration
Before use ```bucket4j-coherence``` module please read [bucket4j-jcache documentation](jcache-usage.md),
because ```bucket4j-coherence``` is just a follow-up of ```bucket4j-jcache```.

**Question:** Bucket4j already supports JCache since version ```1.2```. Why it was needed to introduce direct support for ```Oracle Coherence```?  
**Answer:** Because [JCache API (JSR 107)](https://www.jcp.org/en/jsr/detail?id=107) does not specify asynchronous API,
developing the dedicated module ```bucket4j-coherence``` was the only way to provide asynchrony for users who use ```Bucket4j``` and ```Oracle Coherence``` together.

**Question:** Should I migrate from ```bucket4j-jcache``` to ```bucketj-coherence``` If I do not need in asynchronous API?  
**Answer:** No, you should not migrate to ```bucketj-coherence``` in this case.

## Dependencies
To use ```bucket4j-coherence``` extension you need to add following dependency:
```xml
<dependency>
    <groupId>com.github.vladimir-bukhtoyarov</groupId>
    <artifactId>bucket4j-coherence</artifactId>
    <version>${bucket4j.version}</version>
</dependency>
```

## Example of Bucket instantiation
```java
com.tangosol.net.NamedCache<K, GridBucketState> cache = ...;
...

Bucket bucket = Bucket4j.extension(Coherence.class).builder()
                   .addLimit(Bandwidth.simple(1_000, Duration.ofMinutes(1)))
                   .build(cache, key, RecoveryStrategy.RECONSTRUCT);
```

## Example of ProxyManager instantiation
```java
com.tangosol.net.NamedCache<K, GridBucketState> cache = ...;
...

ProxyManager proxyManager = Bucket4j.extension(Coherence.class).proxyManagerForCache(cache);
```
