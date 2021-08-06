![](/doc-pages/white-logo.png)

# Java rate-limiting library based on token-bucket algorithm.

[![Licence](https://img.shields.io/hexpm/l/plug.svg)](https://github.com/vladimir-bukhtoyarov/bucket4j/blob/master/LICENSE)
[![Donate](https://img.shields.io/badge/Donate-PayPal-green.svg)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=BCY8T8GHTT5T4)



## Supported back-ends
As mentioned above in addition to local in-memory buckets, the Bucket4j supports clustered usage scenario on top of following back-ends:
 
| Back-end                   | Documentation page                                  | Async supported | Optimized serialization | Thin-client support |
| -------------------------- | --------------------------------------------------- | :-------------: | :-------------:         | :-------------:     |
| ```JCache API (JSR 107)``` | [bucket4j-jcache](doc-pages/jcache-usage.md)        | No              | No                      | No                  |
| ```Hazelcast```            | [bucket4j-hazelcast](doc-pages/hazelcast.md)        | Yes             | Yes                     | Planned             |
| ```Apache Ignite```        | [bucket4j-ignite](doc-pages/ignite.md)              | Yes             | n/a                     | Yes                 |
| ```Inifinispan```          | [bucket4j-infinspan](doc-pages/infinispan.md)       | Yes             | Yes                     | No                  |
| ```Oracle Coherence```     | [bucket4j-coherence](doc-pages/coherence.md)        | Yes             | Yes                     | No                  |

## General documentation
#### Basics:
* [Token bucket wikipedia](https://en.wikipedia.org/wiki/Token_bucket) - wikipedia page describes the token-bucket algorithm in classical form.
* [Non-formal overview of token-bucket algorithm](doc-pages/token-bucket-brief-overview.md) - the brief overview of token-bucket algorithm.

#### Examples:
* [Basic-usage](doc-pages/basic-usage.md) - examples of basic usage.
* [Advanced-usage](doc-pages/advanced-usage.md) - examples of advanced usage.
* [Asynchronous-usage](doc-pages/asynchronous.md) - examples of asynchronous usage.
* [Listening of bucket events](doc-pages/listener.md) - examples of monitoring.

#### Production checklist
* [Common production checklist](doc-pages/production-generic-checklist.md) - Mandatory points that need to be understood before using the Bucket4j in production, independently of local or clustered usage scenarios.
* [JCache production checklist](doc-pages/production-jcache-checklist.md) - Mandatory points that need to be understood before using the Bucket4j over JCache cluster.

#### Archive:
* [Documentation for legacy releases](doc-pages/archive-links.md).

## Third-party integrations:
* [marcosbarbero/spring-cloud-zuul-ratelimit](https://github.com/marcosbarbero/spring-cloud-zuul-ratelimit)
* [MarcGiffing/Spring Boot Starter for Bucket4j](https://github.com/MarcGiffing/bucket4j-spring-boot-starter) . Demos of usage (incluing Zuul and Hazelcast) can be found there [bucket4j-spring-boot-starter-examples](https://github.com/MarcGiffing/bucket4j-spring-boot-starter-examples).
* [JHipster/JHipster API Gateway](https://jhipster.github.io/api-gateway/#rate_limiting)
* [Zivver/Dropwizard Ratelimit](https://github.com/zivver/dropwizard-ratelimit)

## Third-party demos and articles:
* [Rate limiting Spring MVC endpoints with bucket4j](https://golb.hplar.ch/2019/08/rate-limit-bucket4j.html)
* [Abdennebi/spring-boot-bucket4j-hazelcast-demo](https://github.com/Abdennebi/spring-boot-bucket4j-hazelcast-demo)
* [ProgrammerSought Bucket4j - preliminary understanding](http://www.programmersought.com/article/2524209291/)
* [Baeldung - Rate Limiting a Spring API Using Bucket4j](https://www.baeldung.com/spring-bucket4j)

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
Copyright 2015-2020 Vladimir Bukhtoyarov
Licensed under the Apache Software License, Version 2.0: <http://www.apache.org/licenses/LICENSE-2.0>.