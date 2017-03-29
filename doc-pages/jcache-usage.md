# JCache integration
```Bucket4j``` supports any GRID solution which compatible with [JCache API (JSR 107)](https://www.jcp.org/en/jsr/detail?id=107) specification.

## Is the provided JCache integration safe across multiple JVMs?
Original question was raised in this issue [#6](https://github.com/vladimir-bukhtoyarov/bucket4j/issues/6).

Question:
> is the provided JCache integration safe across multiple JVMs? I mean, does it ensure that two nodes creating a bucket simultaneously on a given Cache<K, V> will only actually create one single bucket (without resetting a previously created one with the same key)?
 
Answer:
> Yes, JCache integration is safe, independently of used JCache provider, Bucket4j never replaces bucket which already exists:

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
This behavior is guaranteed by [putIfAbsent](http://static.javadoc.io/javax.cache/cache-api/1.0.0/javax/cache/Cache.html#putIfAbsent(K, V)) method contract, 
of class [javax.cache.Cache](http://static.javadoc.io/javax.cache/cache-api/1.0.0/javax/cache/Cache.html).

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