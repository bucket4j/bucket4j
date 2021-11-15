![](/asciidoc/src/main/docs/asciidoc/images/white-logo.png)

# Java rate-limiting library based on token-bucket algorithm.

[![Licence](https://img.shields.io/hexpm/l/plug.svg)](https://github.com/vladimir-bukhtoyarov/bucket4j/blob/master/LICENSE)

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

### Supported JCache compatible(or similar) back-ends
In addition to local in-memory buckets, the Bucket4j supports clustered usage scenario on top of following back-ends:
| Back-end                   |  Async supported | Optimized serialization | Thin-client support |
| :---                       | :---:            | :---:                   | :---:               |
| ```JCache API (JSR 107)``` |  No              | No                      | No                  |
| ```Hazelcast```            |  Yes             | Yes                     | Planned             |
| ```Apache Ignite```        |  Yes             | n/a                     | Yes                 |
| ```Inifinispan```          |  Yes             | Yes                     | No                  |
| ```Oracle Coherence```     |  Yes             | Yes                     | No                  |

## Get Bucket4j library
#### You can add Bucket4j to your project as maven dependency
The Bucket4j is distributed through [Maven Central](http://search.maven.org/):
```xml
<dependency>
    <groupId>com.github.vladimir-bukhtoyarov</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>7.0.0</version>
</dependency>
``` 
#### You can build Bucket4j from sources
```bash
git clone https://github.com/vladimir-bukhtoyarov/bucket4j.git
cd bucket4j
mvn clean install
```

## Have a question?
Feel free to ask via:
* [Bucket4j discussions](https://github.com/vladimir-bukhtoyarov/bucket4j/discussions) for questions, feature proposals, sharing of experience.
* [Bucket4j issue tracker](https://github.com/vladimir-bukhtoyarov/bucket4j/issues/new) to report a bug.
* [Vladimir Bukhtoyarov - Upwork Profile](https://www.upwork.com/freelancers/~013d8e02a32ffdd5f5) if you want to get one time paid support. 

## License
Copyright 2015-2021 Vladimir Bukhtoyarov
Licensed under the Apache Software License, Version 2.0: <http://www.apache.org/licenses/LICENSE-2.0>.