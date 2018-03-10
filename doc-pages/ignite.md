# Apache Ignite integration
Before use ```bucket4j-ignite``` module please read [bucket4j-jcache documentation](jcache-usage.md),
because ```bucket4j-ignite``` is just a follow-up of ```bucket4j-jcache```.

**Question:** Bucket4j already supports JCache since version ```1.2```. Why it was needed to introduce direct support for ```Apache Ignite```?  
**Answer:** Because [JCache API (JSR 107)](https://www.jcp.org/en/jsr/detail?id=107) does not specify asynchronous API,
developing the dedicated module ```bucket4j-ignite``` was the only way to provide asynchrony for users who use ```Bucket4j``` and ```Apache Ignite``` together.

**Question:** Should I migrate from ```bucket4j-jcache``` to ```bucketj-ignite``` If I do not need in asynchronous API?  
**Answer:** No, you should not migrate to ```bucketj-ignite``` in this case.

## Dependencies
To use ```bucket4j-ignite``` extension you need to add following dependency:
```xml
<dependency>
    <groupId>com.github.vladimir-bukhtoyarov</groupId>
    <artifactId>bucket4j-ignite</artifactId>
    <version>${bucket4j.version}</version>
</dependency>
```

## Example of Bucket instantiation
```java
org.apache.ignite.IgniteCache cache = ...;
...

Bucket bucket = Bucket4j.extension(Ignite.class).builder()
                   .addLimit(Bandwidth.simple(1_000, Duration.ofMinutes(1)))
                   .build(cache, key, RecoveryStrategy.RECONSTRUCT);
```

## Example of ProxyManager instantiation
```java
org.apache.ignite.IgniteCache cache = ...;
...

ProxyManager proxyManager = Bucket4j.extension(Ignite.class).proxyManagerForCache(cache);
```
