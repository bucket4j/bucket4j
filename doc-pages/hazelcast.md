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
com.hazelcast.core.IMap<K, GridBucketState> map = ...;
...

Bucket bucket = Bucket4j.extension(Hazelcast.class).builder()
                   .addLimit(Bandwidth.simple(1_000, Duration.ofMinutes(1)))
                   .build(map, key, RecoveryStrategy.RECONSTRUCT);
```

## Example of ProxyManager instantiation
```java
com.hazelcast.core.IMap<K, GridBucketState> map = ...;
...

ProxyManager proxyManager = Bucket4j.extension(Hazelcast.class).proxyManagerForMap(map);
```

## Configuring Custom Serialization for Bucket4j library classes
If you configure nothing, then by default Java serialization will be used for serialization Bucket4j library classes. Java serialization can be rather slow and should be avoided in general.
```Bucket4j``` provides [custom serializers](https://docs.hazelcast.org/docs/3.0/manual/html/ch03s03.html) for all library claasses that could be transferred over network.  
To let Hazelcast know about fast serializers you should register them programmatically in the serialization config:
```java
import com.hazelcast.config.Config;
import com.hazelcast.config.SerializationConfig;
import com.hazelcast.config.SerializerConfig;
import io.github.bucket4j.grid.hazelcast.serialization.HazelcastSerializer;
...
    Config config = ...
    SerializationConfig serializationConfig = config.getSerializationConfig();

    // the starting type ID number for Bucket4j classes.
    // you free to choose any unused ID, but be aware that Bucket4j uses 21 types currently,
    // and may use more types in the future, so leave enough empty space after baseTypeIdNumber 
    int baseTypeIdNumber = 10000;
    
    HazelcastSerializer.addCustomSerializers(serializationConfig, baseTypeIdNumber);
```

