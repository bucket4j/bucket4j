# The official apacheignite/ignite:3.1.0 image bundles a JDK 11 JRE at /opt/java/openjdk,
# but this module (and bucket4j-core) are compiled to JDK 17 class files, so the compute-job
# server-side jar can't load on the stock image. Both this image and eclipse-temurin:17-jdk
# happen to use the same JAVA_HOME path, so replacing that directory's contents is enough -
# no JAVA_HOME/PATH changes are needed.
FROM eclipse-temurin:17-jdk AS jdk17
FROM apacheignite/ignite:3.1.0
COPY --from=jdk17 /opt/java/openjdk /opt/java/openjdk
