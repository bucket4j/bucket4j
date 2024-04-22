# Get Java 8 builds

:heavy_exclamation_mark: According to Bucket4j [backward compatibility policies](backward-compatibility-policy.md) artifacts for Java 8 are not published to Maven-Central since `8.11.0`.

:shit: Obviously, it bad news for all who get stuck on Java 8 by different reasons.

:ambulance: Bucket4j maintainer understand your pain and can provide compiled Maven artifacts for java 8 as ZIP archive,
that can be installed into your local maven repo, or deployed to your company repository like Artifactory or Nexus,
or added as [system dependency](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#system-dependencies).

:heavy_dollar_sign: This costs some money. By buying artifacts for Java 8, you increase motivation of project maintainer to develop new features,
write documentation and answer to user questions. [To get artifacts for Java 8    follow this instructions](https://bucket4j.com/java8.html).

:pill: If you don't want to pay something for Java 8 builds, feel free to do backporting job by yourself, due to all sources are licensed under `Apache 2.0 license`,
nothing to prevent you to maintain in-house fork.