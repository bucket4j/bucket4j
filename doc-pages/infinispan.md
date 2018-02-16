# Infinispan integration
Before use ```bucket4j-infinispan``` module please read [bucket4j-jcache documentation](jcache-usage.md),
because ```bucket4j-infinispan``` is just a follow-up of ```bucket4j-jcache```.

**Question:** Bucket4j already supports JCache since version ```1.2```. Why it was needed to introduce direct support for ```Infinispan```?  
**Answer:** When you want to use Bucket4j together with Infinispan, you must always use ```bucket4j-infinispan``` module instead of ```bucket4j-jcache```,   
because Infinispan does not provide mutual exclusion for entry-processors. Any attempt to use Infinispan via ```bucket4j-jcache``` will be failed with UnsupportedOperationException exception
at bucket construction time.


## Dependencies
To use ```bucket4j-infinispan``` extension you need to add following dependency:
```xml
<dependency>
    <groupId>com.github.vladimir-bukhtoyarov</groupId>
    <artifactId>bucket4j-infinispan</artifactId>
    <version>${bucket4j.version}</version>
</dependency>
```

## Example of Bucket instantiation
```java
org.infinispan.functional.FunctionalMap.ReadWriteMap map = ...;
...

Bucket bucket = Bucket4j.extension(Infinispan.class).builder()
                   .addLimit(Bandwidth.simple(1_000, Duration.ofMinutes(1)))
                   .build(map, key, RecoveryStrategy.RECONSTRUCT);
```

## Example of ProxyManager instantiation
```java
org.infinispan.functional.FunctionalMap.ReadWriteMap map = ...;
...

ProxyManager proxyManager = Bucket4j.extension(Infinispan.class).proxyManagerForMap(map);
```