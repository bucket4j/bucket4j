![](/asciidoc/src/main/docs/asciidoc/images/white-logo.png)

# Java rate-limiting library based on token-bucket algorithm.

[![Licence](https://img.shields.io/hexpm/l/plug.svg)](https://github.com/bucket4j/bucket4j/blob/master/LICENSE.txt)

#### Get dependency
The Bucket4j is distributed through [Maven Central](http://search.maven.org/):

##### Java 17 dependency
```xml
<!-- For java 17+ -->
<dependency>
  <groupId>com.bucket4j</groupId>
  <artifactId>bucket4j_jdk17-core</artifactId>
  <version>8.11.1</version>
</dependency>
```
##### Java 11 dependency
```xml
<!-- For java 11 -->
<dependency>
  <groupId>com.bucket4j</groupId>
  <artifactId>bucket4j_jdk11-core</artifactId>
  <version>8.11.1</version>
</dependency>
```
##### Java 8 dependency
To get the builds for `Java 8` follow to [these instructions](https://bucket4j.com/commercial/java8.html).

#### Quick start
```java
import io.github.bucket4j.Bucket;

...
// bucket with capacity 20 tokens and with refilling speed 1 token per each 6 second
private static Bucket bucket = Bucket.builder()
      .addLimit(limit -> limit.capacity(20).refillGreedy(10, Duration.ofMinutes(1)))
      .build();

private void doSomethingProtected() {
   if (bucket.tryConsume(1)) {
      doSomething();    
   } else {
      throw new SomeRateLimitingException();
   }
}
```
More examples [can be found there](https://bucket4j.github.io/8.11.1/toc.html#quick-start-examples)

## [Documentation](https://bucket4j.github.io)
* [Reference](https://bucket4j.github.io/8.11.1/toc.html)
* [Quick start examples](https://bucket4j.github.io/8.11.1/toc.html#quick-start-examples)
* [Third-party articles](https://bucket4j.github.io/#third-party-articles)

## Bucket4j basic features
* *Absolutely non-compromise precision* - Bucket4j does not operate with floats or doubles, all calculation are performed in the integer arithmetic, this feature protects end users from calculation errors involved by rounding.
* *Effective implementation in terms of concurrency*:
  - Bucket4j is good scalable for multi-threading case it by defaults uses lock-free implementation.
  - In same time, library provides different concurrency strategies that can be chosen when default lock-free strategy is not desired.
* *Effective API in terms of garbage collector footprint*: Bucket4j API tries to use primitive types as much as it is possible in order to avoid boxing and other types of floating garbage.
* *Pluggable listener API* that allows to implement monitoring and logging.
* *Rich diagnostic API* that allows to investigate internal state.
* *Rich configuration management* - configuration of the bucket can be changed on fly

## Bucket4j distributed features
In additional to basic features described above, ```Bucket4j``` provides ability to implement rate-limiting in cluster of JVMs:
- Bucket4j out of the box supports any GRID solution which compatible with JCache API (JSR 107) specification.
- Bucket4j provides the framework that allows to quickly build integration with your own persistent technology like RDMS or a key-value storage.
- For clustered usage scenarios Bucket4j supports asynchronous API that extremely matters when going to distribute world, because asynchronous API allows avoiding blocking your application threads each time when you need to execute Network request.

## [Spring boot starter](https://github.com/MarcGiffing/bucket4j-spring-boot-starter)
Bucket4j is not a framework, it is a library, with Bucket4j you need to write a code to achive your goals. 
For generic use-cases, try to look at powerfull [Spring Boot Starter for Bucket4j](https://github.com/MarcGiffing/bucket4j-spring-boot-starter), that allows you to set access limits on your API effortlessly. 
Its key advantage lies in the configuration via properties or yaml files, eliminating the need for manual code authoring.

### Supported JCache compatible(or similar) back-ends
In addition to local in-memory buckets, the Bucket4j supports clustered usage scenario on top of following back-ends:

| Back-end                   |  Async supported | Flexible Per-entry expiration | Optimized serialization | Thin-client support |                                     Documentation link                                      |
| :---                       | :---:            |:-----------------------------:|:-----------------------:|:-------------------:|:-------------------------------------------------------------------------------------------:|
| ```JCache API (JSR 107)``` |  No              |              No               |          No             |         No          |        [bucket4j-jcache](https://bucket4j.github.io/8.7.0/toc.html#bucket4j-jcache)         |
| ```Hazelcast```            |  Yes             |              Yes              |           Yes           |         No          |     [bucket4j-hazelcast](https://bucket4j.github.io/8.7.0/toc.html#bucket4j-hazelcast)      |
| ```Apache Ignite```        |  Yes             |              No               |           n/a           |         Yes         |        [bucket4j-ignite](https://bucket4j.github.io/8.7.0/toc.html#bucket4j-ignite)         |
| ```Inifinispan```          |  Yes             |             TODO              |           Yes           |         No          | [bucket4j-infinispan](https://bucket4j.github.io/8.7.0/toc.html#bucket4j-infinispan)        |
| ```Oracle Coherence```     |  Yes             |             TODO              |           Yes           |         No          |     [bucket4j-coherence](https://bucket4j.github.io/8.7.0/toc.html#bucket4j-coherence)      |

### Redis back-ends
| Back-end                   |  Async supported | Redis cluster supported |                                                      Documentation link                                                      |
| :---                       | :---:            |:-----------------------:|:----------------------------------------------------------------------------------------------------------------------------:|
| ```Redis/Redisson```       |  Yes             |           Yes           | [bucket4j-redis/Redisson](https://bucket4j.github.io/8.7.0/toc.html#example-of-bucket-instantiation-via-redissonbasedproxymanager) |
| ```Redis/Jedis```          |  No              |           Yes           |    [bucket4j-redis/Jedis](https://bucket4j.github.io/8.7.0/toc.html#example-of-bucket-instantiation-via-jedisbasedproxymanager)    |
| ```Redis/Lettuce```        |  Yes             |           Yes           |   [bucket4j-redis/Lettuce](https://bucket4j.github.io/8.7.0/toc.html#example-of-bucket-instantiation-via-lettucebasedproxymanager) |

### JDBC back-ends
| Back-end                   |                                     Documentation link                                      |
|:---------------------------|:-------------------------------------------------------------------------------------------:|
| ```MySQL```                |       [bucket4j-mysql](https://bucket4j.github.io/8.11.1/toc.html#mysql-integration)        |
| ```PostgreSQL```           |  [bucket4j-postgresql](https://bucket4j.github.io/8.11.1/toc.html#postgresql-integration)   |
| ```Oracle```               |      [bucket4j-oracle](https://bucket4j.github.io/8.11.1/toc.html#oracle-integration)       |
| ```Microsoft SQL Server``` | [bucket4j-mssql](https://bucket4j.github.io/8.11.1/toc.html#microsoftsqlserver-integration) |
| ```MariaDB```              |     [bucket4j-mariadb](https://bucket4j.github.io/8.11.1/toc.html#mariadb-integration)      |


### Local caches support
Sometimes you are having deal with bucket per key scenarios but distributed synchronization is unnecessary, for example where request stickiness is provided by a load balancer, or other use-cases where stickiness can be achieved by the application itself, for example, Kafka consumer. For such scenarios Bucket4j provides support for following list of local caching libraries:
| Back-end                      | Documentation link      |
| :---                          | :---:                   |
| ```Caffeine```                | [bucket4j-caffeine](https://github.com/bucket4j/bucket4j/blob/7.3/bucket4j-caffeine/src/main/java/io/github/bucket4j/caffeine/CaffeineProxyManager.java)      |

### Third-party integrations
| Back-end                      |                               Project page                               |
| :---                          |:------------------------------------------------------------------------:|
| ```Datomic Database```        | [clj-bucket4j-datomic](https://github.com/fr33m0nk/clj-bucket4j-datomic) |

## [Bucket4j Backward compatibility policy](backward-compatibility-policy.md)

## Have a question?
Feel free to ask via:
* [Bucket4j issue tracker](https://github.com/bucket4j/bucket4j/issues/new) to report a bug.
* [Bucket4j discussions](https://github.com/bucket4j/bucket4j/discussions) for questions, feature proposals, sharing of experience.

## License
Copyright 2015-2024 Vladimir Bukhtoyarov
Licensed under the Apache Software License, Version 2.0: <http://www.apache.org/licenses/LICENSE-2.0>.

## :beer: Want to support?
[Make donate](https://app.lava.top/ru/2716741203?donate=open) to increase motivation of maintainer.
