# JCache integration
```Bucket4j``` supports any GRID solution which compatible with [JCache API (JSR 107)](https://www.jcp.org/en/jsr/detail?id=107) specification.
To use JCache extension you also need to add following dependency:
```xml
<dependency>
    <groupId>com.github.vladimir-bukhtoyarov</groupId>
    <artifactId>bucket4j-jcache</artifactId>
    <version>${bucket4j.version}</version>
</dependency>
```

## Overview of JCache integration
In case of JCache usage scenario bucket divided in two logical parts:
- [GridBucketState](https://github.com/vladimir-bukhtoyarov/bucket4j/blob/master/src/main/java/com/github/bucket4j/grid/GridBucketState.java) has one-to-one logical relation with bucket and stored in data-grid, most likely you should not worry about this part, JCache will manage (and replicate if configured) for you.
- [GridBucket](https://github.com/vladimir-bukhtoyarov/bucket4j/blob/master/src/main/java/com/github/bucket4j/grid/GridBucket.java) acts like proxy and just issues commands for bucket mutation, then commands serialized and executed on the JCache node which holds the GridBucketState instance. In opposite to GridBucketState, GridBucket has many-to-one relation with bucket, because bucket can be accessible from multiple client node, 
and even multiple times on same(as in your example) if client has no rational strategy to cache the instance of GridBucket.

[Question](https://github.com/vladimir-bukhtoyarov/bucket4j/issues/6):
> is the provided JCache integration safe across multiple JVMs? I mean, does it ensure that two nodes creating a bucket simultaneously on a given Cache<K, V> will only actually create one single bucket (without resetting a previously created one with the same key)?

**Answer:**
Yes, JCache integration is safe, independently of used JCache provider, Bucket4j never replaces bucket which already exists:
```java
public class JCacheProxy<K extends Serializable> implements GridProxy {
    private final Cache<K, GridBucketState> cache;
    private final K key;
    ...
    @Override
    public void setInitialState(GridBucketState initialState) {
        cache.putIfAbsent(key, initialState);
    }
    ...
}
```
This behavior is guaranteed by **putIfAbsent** method contract of [javax.cache.Cache](http://static.javadoc.io/javax.cache/cache-api/1.0.0/javax/cache/Cache.html) class.

[Question:](https://github.com/vladimir-bukhtoyarov/bucket4j/issues/26)
> Do I need to cache grid-buckets?

**Answer:**
GridBucket can be created multiple time and this will not lead to logical error, 
because the main part of bucket GridBucketState is strongly protected from duplication, 
but this will lead to significant performance degradation, 
because GridBucket [issues network request](https://github.com/vladimir-bukhtoyarov/bucket4j/blob/master/src/main/java/com/github/bucket4j/grid/GridBucket.java#L34) to grid at the moment of bucket initialization, so you will pay at least twice for non-optimized code. 
So, **GridBucket must not be treated as light-weight entity**, it is better to cache its instance and reuse between invocations.
Fortunately, you can work through ProxyManager(described below) and do not worry about caching the proxies, because ProxyManager operates with light-weight versions of JCache buckets.

## Working through ProxyManager
TODO

## 
TODO


## Handling split-brain and similar problems
The distributed usage scenario is little bit more complicated than simple usage inside one JVM, 
because it is need specify the reaction which should be applied in case of bucket state is lost by any reason, for example because of:
- Split-brain happen.
- The bucket state was stored on single grid node without replication strategy and this node was crashed.
- Wrong cache configuration.
- Pragmatically errors introduced by GRID vendor.
- Human mistake.

The ```Bucket4j``` make the client to specify recovery strategy from the list:
- **RECONSTRUCT** Initialize bucket yet another time. Use this strategy if availability is more preferred than consistency.
- **THROW_BUCKET_NOT_FOUND_EXCEPTION** Throw BucketNotFoundException. Use this strategy if consistency is more preferred than availability. 


## Examples of distributed usage
### Example of Hazelcast integration 
```java
  
Config config = new Config();
CacheSimpleConfig cacheConfig = new CacheSimpleConfig();
cacheConfig.setName("my_buckets");
config.addCacheConfig(cacheConfig);

hazelcastInstance = Hazelcast.newHazelcastInstance(config);
ICacheManager cacheManager = hazelcastInstance.getCacheManager();
cache = cacheManager.getCache("my_buckets");

// Bucket will be stored in the imap by this ID 
Object bucketId = "666";

// construct bucket
Bucket bucket = Bucket4j.jCacheBuilder(RecoveryStrategy.RECONSTRUCT)
                .addLimit(Bandwidth.simple(1_000, Duration.ofMinutes(1)))
                .build(cache, KEY);
```

### Example of Apache Ignite(GridGain) integration 
```java
Ignite ignite = Ignition.start();
...

// You can use spring configuration if do not want to configure cache in the java code  
CacheConfiguration cfg = new CacheConfiguration("my_buckets");

// setup cache configuration as you wish
cfg.setXXX...
cache = ignite.getOrCreateCache(cfg);

// Bucket will be stored in the Ignite cache by this ID 
Object bucketId = "21";

// construct bucket
Bucket bucket = Bucket4j.jCacheBuilder(RecoveryStrategy.RECONSTRUCT)
                .addLimit(Bandwidth.simple(1_000, Duration.ofMinutes(1)))
                .build(cache, KEY);
```