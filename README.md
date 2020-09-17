![](/doc-pages/white-logo.png)

# Java rate-limiting library based on token-bucket algorithm.
[![Join the chat at https://gitter.im/vladimir-bukhtoyarov/bucket4j](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/vladimir-bukhtoyarov/bucket4j?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Licence](https://img.shields.io/hexpm/l/plug.svg)](https://github.com/vladimir-bukhtoyarov/bucket4j/blob/master/LICENSE)
[![Donate](https://img.shields.io/badge/Donate-PayPal-green.svg)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=BCY8T8GHTT5T4)

## Bucket4j's core features:
* Implemented on top of ideas of the well-known algorithm, which are by de-facto standard for rate limiting in the IT industry.
* Effective lock-free implementation, Bucket4j is good scalable for multi-threading case.
* Pluggable listener API that allows implementing monitoring and logging.
* Ability to use bucket as scheduler.
* Support for multiple limits per single bucket(akka peak rate plus average rate).

## Bucket4j's distributed token-bucket features:
* Support for ```JCache API (JSR 107)```.
* Both synchronous and asynchronous API.
* Various optimization techniques that can be used for reduction of inter-node traffic like batching, postponed syncing, rate prediction.
* A generic framework for building distributed token-bucket on top of technology that you prefer.

Out from the box Bucket4j provides support for following backends:

| Back-end                   | Async supported | Optimized serialization |
| -------------------------- | -------------   |   -------------         |
| ```JCache API (JSR 107)``` |  No             | No                      |
| ```Hazelcast```            |  Yes            | Yes                     |
| ```Apache Ignite```        |  Yes            | n/a                     |
| ```Inifinispan```          |  Yes            | Yes                     |
| ```Oracle Coherence```     |  Yes            | Yes                     |
## Third-party integrations:
* [MarcGiffing/Spring Boot Starter for Bucket4j](https://github.com/MarcGiffing/bucket4j-spring-boot-starter) 
* [marcosbarbero/spring-cloud-zuul-ratelimit](https://github.com/marcosbarbero/spring-cloud-zuul-ratelimit)
* [Zivver/Dropwizard Ratelimit](https://github.com/zivver/dropwizard-ratelimit)

## [Documentation for current version](https://bucket4j.github.io/)
## [Documentation for legacy releases](doc-pages/archive-links.md)

## Third-party articles:
* [Token bucket wikipedia](https://en.wikipedia.org/wiki/Token_bucket)
* [Rate limiting Spring MVC endpoints with bucket4j](https://golb.hplar.ch/2019/08/rate-limit-bucket4j.html)
* [ProgrammerSought Bucket4j - preliminary understanding](http://www.programmersought.com/article/2524209291/)
* [Baeldung - Rate Limiting a Spring API Using Bucket4j](https://www.baeldung.com/spring-bucket4j)

## Third-party demos:
* [Abdennebi/spring-boot-bucket4j-hazelcast-demo](https://github.com/Abdennebi/spring-boot-bucket4j-hazelcast-demo)
* [bucket4j-spring-boot-starter-examples](https://github.com/MarcGiffing/bucket4j-spring-boot-starter-examples)

## Get Bucket4j library
#### You can add Bucket4j to your project as maven dependency
The Bucket4j is distributed through both [JCenter](https://bintray.com/bintray/jcenter) and [Maven Central](http://search.maven.org/),
use any of them:
```xml
<dependency>
    <groupId>com.github.vladimir-bukhtoyarov</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>5.0.0</version>
</dependency>
``` 
#### You can build Bucket4j from sources
```bash
git clone https://github.com/vladimir-bukhtoyarov/bucket4j.git
cd bucket4j
mvn clean install
```
**NOTE:** Build environment must provide support for Docker, because Bucket4j uses [Testcontainers](https://www.testcontainers.org/).

## Have a question?
Feel free to ask in:
* Gitter chat [https://gitter.im/vladimir-bukhtoyarov/bucket4j](https://gitter.im/vladimir-bukhtoyarov/bucket4j) 
* Github issue tracker [https://github.com/vladimir-bukhtoyarov/bucket4j/issues/new](https://github.com/vladimir-bukhtoyarov/bucket4j/issues/new)

## License
Licensed under the Apache Software License, Version 2.0: <http://www.apache.org/licenses/LICENSE-2.0>.

## Copyright
Copyright 2015-2020 Vladimir Bukhtoyarov