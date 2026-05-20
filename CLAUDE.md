# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

- **Build all modules**: `./mvnw --no-transfer-progress -B clean package`
- **Build without tests**: `./mvnw -DskipTests clean package`
- **Run all tests**: `./mvnw --no-transfer-progress -B clean test`
- **Run tests in a single module**: `./mvnw -pl bucket4j-core test`
- **Run a single test class**: `./mvnw -pl bucket4j-core -Dtest=LockFreeBucketLayout test`
- **Run a single test method**: `./mvnw -pl bucket4j-core -Dtest=LockFreeBucketLayout#testMethod test`
- **Run a Groovy specification**: `./mvnw -pl bucket4j-core -Dtest=BandwidthBuilderTest test`
- **Skip backward-compatibility tests**: They are commented out from the root POM; run separately via `./mvnw -pl backward-compatibility-tests/...`
- **Build requirements**: JDK 17, Zulu distribution (CI uses `actions/setup-java@v4`)

## Project Architecture

**bucket4j** is a Java rate-limiting library implementing the token-bucket algorithm. It is a multi-module Maven project (parent: `bucket4j_jdk17`, artifact group: `com.bucket4j`).

### Core module (`bucket4j-core`)
- **Package**: `io.github.bucket4j`
- **Entry points**: `Bucket` (main API interface), `Bucket.builder()` (construction via `ConfigurationBuilder` + `Bandwidth`/`Refill`)
- **Core classes**: `Bucket`, `Bandwidth`, `Refill`, `BucketConfiguration`, `ConsumptionProbe`, `EstimationProbe`, `BlockingBucket`, `SchedulingBucket`, `VerboseBucket`
- **Local implementations** (`io.github.bucket4j.local`): `LockFreeBucket` (default, lock-free CAS), `SynchronizedBucket`, `ThreadUnsafeBucket`, `LocalBucketBuilder`
- **Configuration**: `ConfigurationBuilder` builds buckets with limits, `TokensInheritanceStrategy` controls how tokens transfer on reconfiguration
- **Math**: Pure integer arithmetic (no floats/doubles), `BucketState64BitsInteger` for state management
- **Concurrency**: Lock-free by default via `SynchronizationStrategy` enum; pluggable strategies

### Distributed module (in `bucket4j-core`, `io.github.bucket4j.distributed`)
- **Proxy pattern**: `BucketProxy` / `AsyncBucketProxy` wrap remote state, delegate via `ProxyManager` implementations
- **Optimization**: Pluggable request optimization strategies (batch, delay, predictive, skip-on-zero, manual) in `proxy/optimization/`
- **Remote commands**: Command pattern in `remote/commands/` (e.g., `TryConsumeAndReturnRemainingTokensCommand`)
- **Serialization**: Custom serialization framework in `serialization/` for cross-version compatibility
- **Versioning**: `versioning/` for rolling upgrade support across bucket4j versions
- **JDBC**: `jdbc/` for SQL-based select-for-update proxy managers
- **Expiration**: `ExpirationAfterWriteStrategy` for distributed bucket TTL

### Backend integration modules
Each backend module provides a `ProxyManager` implementation:

| Module | Technology | Async |
|--------|-----------|-------|
| `bucket4j-jcache` | JCache (JSR 107) | No |
| `bucket4j-hazelcast-all` | Hazelcast | Yes |
| `bucket4j-ignite` | Apache Ignite | Yes |
| `bucket4j-infinispan-all` | Infinispan | Yes |
| `bucket4j-coherence` | Oracle Coherence | Yes |
| `bucket4j-couchbase` | Couchbase | Yes |
| `bucket4j-redis/*` | Redis (5 sub-modules) | Yes |
| `bucket4j-postgresql`, `bucket4j-mysql`, `bucket4j-mariadb`, `bucket4j-mssql`, `bucket4j-db2`, `bucket4j-oracle` | SQL databases | Yes |
| `bucket4j-caffeine` | Caffeine cache | N/A |
| `bucket4j-mongodb` | MongoDB | Yes |

### Redis sub-modules (`bucket4j-redis/`)
- `bucket4j-redis-common` — shared interfaces and base implementations
- `bucket4j-jedis`, `bucket4j-lettuce`, `bucket4j-redisson`, `bucket4j-vertx`, `bucket4j-glide` — Redis client integrations

### Testing conventions
- **Test frameworks**: JUnit 5 (`**/*Test.class`) and Groovy/Spock (`**/*Specification.class`)
- **Test profiles**: Database backends use Testcontainers (version 1.20.1)
- **Mock objects**: Located in `io.github.bucket4j.mock` package
- **TCK tests**: Compatibility tests in `io.github.bucket4j.tck`
- **Examples module**: `bucket4j-examples` has usage demo code (not library code)
- **OSGi**: BND plugin generates OSGi manifests for all modules