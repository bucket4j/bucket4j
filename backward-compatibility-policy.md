# Bucket4j Backward compatibility policies
When choosing an alternative from many open-source libraries it is better to understand explicit(documented) or implicit(discovered from real maintainers actions) backward compatibility policy that each alternative provides.
Bucket4j maintainers decided to declare backward compatibility policies explicitly, and you can expect that these policies will not be violated in the future.
Different backward compatibility aspects are described bellow.

## Documentation
Documentation is provided for any version, does not matter how old it is. 
We never remove documentation for legacy releases.
Documentation for previous releases always available [in archive](https://bucket4j.com/previous-releases.html).

## Rolling updates
When using distributed bucket, you should understand that bucket is stored inside some persistent storage in some format.
Format can be changed in time, because of needs to introduce new features that require to store new information that can not be understand by previous versions.
Bucket4j maintainers provide functionality for rolling updates, and keep serious attention for testing backward compatibility in that aspect.
You do not have to stop all production, to upgrade Bucket4j library. Bucket4j is explicitly designed and tested to be updatable nod-by-node without loosing HA.

## JVM version compatibility
Bucket4j provides Maven-Central artifacts for two LTS version of JDK. Currently, for 17 and 11. In the future it will be 21 and 17, then 25 and 21, etc...
If you need for build for more legacy JDK, then contact to maintainers, nothing impossible if you are able to pay for this job.

## Third-party libraries compatibility
Bucket4j, usually provides Maven-Central artifacts for two version of third-party library. 
[For example](https://github.com/bucket4j/bucket4j/tree/master/bucket4j-hazelcast-all) different maven artifacts are available for Hazelcast(4.x and 5.x).
If you need for build for more legacy third-party dependency(like apache ignite 1.x), then contact to maintainers, nothing impossible if you are able to pay for this job.

## Removal of deprecated API
Removal of all methods that are marked as deprecated always happens at major release. 
For example, if some method is marked as deprecated at `8.10.0` that it is right to expect that it will not be removed prior `9.0.0`.