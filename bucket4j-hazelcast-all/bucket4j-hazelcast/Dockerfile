FROM hazelcast/hazelcast:4.2
USER root

ENV BUCKET4J_VERSION 6.3.0
ENV JCACHE_VERSION 1.1.1
ENV HZ_VERSION 4.2

# Adding custom hazelcast.xml
ADD hazelcast.xml ${HZ_HOME}
ENV JAVA_OPTS -Dhazelcast.config=${HZ_HOME}/hazelcast.xml

# Download  bucket4j jars from maven repo.
ADD https://repo.maven.apache.org/maven2/com/github/vladimir-bukhtoyarov/bucket4j-core/$BUCKET4J_VERSION/bucket4j-core-$BUCKET4J_VERSION.jar ${HZ_HOME}
ADD https://repo.maven.apache.org/maven2/com/github/vladimir-bukhtoyarov/bucket4j-jcache/$BUCKET4J_VERSION/bucket4j-jcache-$BUCKET4J_VERSION.jar ${HZ_HOME}
ADD https://repo.maven.apache.org/maven2/com/github/vladimir-bukhtoyarov/bucket4j-hazelcast/$BUCKET4J_VERSION/bucket4j-hazelcast-$BUCKET4J_VERSION.jar ${HZ_HOME}
RUN chmod a+r ${HZ_HOME}/bucket4j-*.jar