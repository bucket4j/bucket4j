# Hazelcast integration
Before use ```bucket4j-hazelcast``` module please read [bucket4j-jcache documentation](jcache-usage.md),
because ```bucket4j-hazelcast``` is just a follow-up of ```bucket4j-jcache```.

**Question:** Bucket4j already supports JCache since version ```1.2```. Why it was needed to introduce direct support for ```Hazelcast```?  
**Answer:** Because [JCache API (JSR 107)](https://www.jcp.org/en/jsr/detail?id=107) does not specify asynchronous API,
developing the dedicated module ```bucket4j-hazelcast``` was the only way to provide asynchrony for users who use ```Bucket4j``` and ```Hazelcast``` together.

**Question:** Should I migrate from ```bucket4j-jcache``` to ```bucket4j-hazelcast``` If I do not need in asynchronous API?  
**Answer:** No, you should not migrate to ```bucket4j-hazelcast``` in this case.

## Dependencies
To use ```bucket4j-hazelcast``` extension you need to add following dependency:
```xml
<dependency>
    <groupId>com.github.vladimir-bukhtoyarov</groupId>
    <artifactId>bucket4j-hazelcast</artifactId>
    <version>${bucket4j.version}</version>
</dependency>
```

## Example of Bucket instantiation
```java
com.hazelcast.core.IMap map = ...;
...

Bucket bucket = Bucket4j.extension(Hazelcast.class).builder()
                   .addLimit(Bandwidth.simple(1_000, Duration.ofMinutes(1)))
                   .build(map, key, RecoveryStrategy.RECONSTRUCT);
```

## Example of ProxyManager instantiation
```java
com.hazelcast.core.IMap map = ...;
...

ProxyManager proxyManager = Bucket4j.extension(Hazelcast.class).proxyManagerForMap(map);
```
