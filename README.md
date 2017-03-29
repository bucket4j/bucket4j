# Bucket4j - is a java implementation of token-bucket algorithm
[![Build Status](https://travis-ci.org/vladimir-bukhtoyarov/bucket4j.svg?branch=master)](https://travis-ci.org/vladimir-bukhtoyarov/bucket4j)
[![Coverage Status](https://coveralls.io/repos/github/vladimir-bukhtoyarov/bucket4j/badge.svg?branch=master)](https://coveralls.io/github/vladimir-bukhtoyarov/bucket4j?branch=master)
[![Join the chat at https://gitter.im/vladimir-bukhtoyarov/bucket4j](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/vladimir-bukhtoyarov/bucket4j?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

## Advantages of Bucket4j
* Implemented on top of ideas of well known algorithm, which are by de-facto standard for rate limiting in the IT industry.
* Effective lock free implementation without any critical section, Bucket4j is good scalable for multithreading case.
* Absolutely non-compromise precision, Bucket4j does not operate with floats or doubles, all calculation are performed in the integer arithmetic,
this feature protects end users from calculation errors involved by rounding.
* Ability to switch from one JVM to cluster in two lines of code. Using Bucket4j you are able to limiting something in the cluster of JVMs.
Since [release 1.3](https://github.com/vladimir-bukhtoyarov/bucket4j/releases/tag/1.4.0) the ```Bucket4j``` supports any GRID solution which compatible with [JCache API (JSR 107)](https://www.jcp.org/en/jsr/detail?id=107) specification.
Just use your favorite grid including [Hazelcast](http://hazelcast.com/products/hazelcast/), [Ignite](http://www.oracle.com/technetwork/middleware/coherence/overview/index-087514.html), [Coherence](http://www.oracle.com/technetwork/middleware/coherence/overview/index.html), [Infinispan](http://infinispan.org/) or any other.
* Ability to specify more than one bandwidth per bucket. For example you can limit 1000 events per hours but not often then 100 events per minute.
* Customizable time measurement. You are able to specify how to time will be measurement: as `System.nanotime()` or `System.currentTimeMillis()`
or you can to specify own way to measure time.

## Documentation
The items placed in recommended to read order:
* [Basic-usage](doc-pages/basic-usage.md) - examples of basic usage, just start from here and most likely you will skip the rest items below. 
* [Brief overview of token-bucket algorithm](doc-pages/token-bucket-brief-overview.md) - the brief overview of token-bucket algorithm. 
* [Jcache integration](doc-pages/jcache-usage.md) - documentation and examples about usage ```Bucket4j``` with in-memory grids which supports ```JCache API (JSR 107)``` specification.
* [Advanced usage](doc-pages/advanced-usage.md) - examples of advanced usage.

## Get Bucket4j library

#### By direct link
[Download compiled jar, sources, javadocs](https://github.com/vladimir-bukhtoyarov/bucket4j/releases/tag/1.4.0)

#### You can build Bucket4j from sources

```bash
git clone https://github.com/vladimir-bukhtoyarov/bucket4j.git
cd bucket4j
mvn clean install
```

#### You can add Bucket4j to your project as maven dependency

The bucket4j library is distributed through [Bintray](http://bintray.com/), so you need to add Bintray repository to your `pom.xml`

```xml
     <repositories>
         <repository>
             <id>jcenter</id>
             <url>http://jcenter.bintray.com</url>
         </repository>
     </repositories>
```

Then include Bucket4j as dependency to your `pom.xml`

```xml
<dependency>
    <groupId>com.github</groupId>
    <artifactId>bucket4j</artifactId>
    <version>1.4.0</version>
</dependency>
```


## Have a question?
Feel free to ask in the [gitter chat](https://gitter.im/vladimir-bukhtoyarov/bucket4j) 

## License
Copyright 2015-2017 Vladimir Bukhtoyarov
Licensed under the Apache Software License, Version 2.0: <http://www.apache.org/licenses/LICENSE-2.0>.

