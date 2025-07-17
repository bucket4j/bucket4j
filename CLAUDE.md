# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is the Bucket4j MongoDB integration module, part of the larger Bucket4j rate limiting library ecosystem. Bucket4j is a Java rate-limiting library based on the token-bucket algorithm, and this module provides MongoDB backend support for distributed rate limiting scenarios.

## Build System

- **Build Tool**: Maven (multi-module project)
- **Java Version**: Java 17+
- **Main Commands**:
  - `mvn clean compile` - Compile the project
  - `mvn test` - Run tests (includes MongoDB integration tests with Testcontainers)
  - `mvn clean install` - Build and install to local repository

## Module Structure

This is a single module within the larger Bucket4j project:
- **bucket4j-mongodb** - MongoDB integration module
  - Uses MongoDB Reactive Streams driver (version 4.11.1)
  - Implements `AbstractCompareAndSwapBasedProxyManager` for distributed bucket management
  - Provides both synchronous and asynchronous APIs

## Key Components

- **MongoDBCompareAndSwapBasedProxyManager** - Main proxy manager implementing compare-and-swap operations for MongoDB
- **Bucket4jMongoDB** - Factory class for building MongoDB-based proxy managers
- **MongoDBUtilitySubscriber** - Utility subscriber for handling reactive streams operations

## Testing

- Uses **Testcontainers** with MongoDB container for integration tests
- Test class: `MongoDBTest` extends `AbstractDistributedBucketTest`
- Includes TTL expiration testing for bucket cleanup

## Dependencies

- Core Bucket4j library (`bucket4j_jdk17-core`)
- MongoDB Reactive Streams driver (`mongodb-driver-reactivestreams`)
- Testcontainers for testing (`testcontainers-mongodb`)

## Architecture Notes

- Implements compare-and-swap pattern for distributed consistency
- Supports both sync and async operations via reactive streams
- Uses MongoDB's findOneAndReplace with upsert for atomic operations
- Supports bucket expiration via MongoDB TTL indexes
- Key serialization handled via Mapper interface