# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Rodriguez is a fault injection test harness based on "Release It!" failure patterns. It simulates various infrastructure failures (network, HTTP, JDBC, filesystem, AWS services) on different ports from a single process. Each port maps to a specific failure behavior.

## Build Commands

```bash
# Build all modules
mvn clean package

# Build skipping tests
mvn clean package -DskipTests

# Run tests for all modules
mvn test

# Run tests for a specific module
mvn test -pl rodriguez-core
mvn test -pl rodriguez-jdbc
mvn test -pl rodriguez-aws-sdk
mvn test -pl rodriguez-fuse

# Run a single test class
mvn test -pl rodriguez-core -Dtest=HarnessServerTest

# Run the harness locally
mvn exec:java -pl rodriguez-build

# Build Docker image
mvn jib:dockerBuild -pl rodriguez-build

# Build GraalVM native image (requires GraalVM 21 JDK)
mvn -Pgraalvm package
```

## Running Example Tests (requires Rodriguez running via Docker)

```bash
docker compose up -d

# Node.js (vitest)
cd examples/nodejs && npm test

# Go
cd examples/go && go test -v -timeout 120s ./...

# PHP (PHPUnit)
cd examples/php && vendor/bin/phpunit
```

## Architecture

### Module Structure

- **rodriguez-core** — Core framework: socket/HTTP behaviors, control server (port 10200), configuration, metrics
- **rodriguez-jdbc** — JDBC driver mock (`jdbc:rodriguez://`) with CSV-based fixtures and configurable delays
- **rodriguez-aws-sdk** — S3Mock (port 10213) and SQSMock (port 10214) with filesystem/in-memory backing
- **rodriguez-fuse** — FUSE filesystem fault injection (SlowIO, DiskFull, CorruptedRead, etc.)
- **rodriguez-build** — Aggregates all modules for Docker/native packaging

### Behavior Type Hierarchy

All fault behaviors implement `InstabilityBehavior` (uses Jackson `@JsonTypeInfo` for polymorphic JSON deserialization by `type` class name):

- `SocketInstabilityBehavior` — Raw `ServerSocket`-level faults (RefuseConnection, NotAccept, AcceptButSilent, NeverDrain, NoResponseAndSendRST)
- `HttpInstabilityBehavior` — Uses `com.sun.net.httpserver.HttpServer` (SlowResponse, BrokenJson, ResponseHeaderOnly, OversizedResponse, ContentTypeMismatch, RefuseAuthentication, S3Mock, SQSMock)

Behaviors implementing `MetricsAvailable` get a `MetricRegistry` injected by `HarnessServer`.

### Extension SPI

Extensions use Java `ServiceLoader` via `HarnessExtension` interface (`getName()`, `start(JsonNode)`, `shutdown()`). Registration file: `META-INF/services/net.unit8.rodriguez.HarnessExtension`. Extensions are configured under the `"extensions"` key in configuration JSON.

### Configuration Loading

1. Each module provides `META-INF/rodriguez/default-config.json` (auto-discovered from classpath)
2. Optional user config via CLI `--config` flag
3. Configs are merged (user overrides defaults) via `HarnessConfig.merge()`

### Default Port Mapping

10200: Control API, 10201-10209: Core behaviors, 10210: JDBC mock, 10211-10212: HTTP behaviors, 10213: S3Mock, 10214: SQSMock

### AWS Mock Pattern

S3Mock/SQSMock use enum-based action routing (`S3Action`/`SQSAction`) with `MockAction<T>` interface. S3 is filesystem-backed; SQS is in-memory. SQSMock supports both AWS Query and JSON protocols.

## Key Conventions

- All documentation and source code must be written in English
- Java 21 required (uses pattern matching for instanceof, record patterns)
- Tests use JUnit 5 + AssertJ
- PicoCLI for CLI interface with annotation processing
- JSON configuration with Jackson (polymorphic types via `@JsonTypeInfo`)
- Logging via `java.util.logging`

## Version Bump Procedure

When bumping the project version, update `0.X.0` → `0.Y.0` in all of the following locations:

1. **Parent POM** — `pom.xml` (`<version>`)
2. **Child module parent references** — `rodriguez-core/pom.xml`, `rodriguez-jdbc/pom.xml`, `rodriguez-aws-sdk/pom.xml`, `rodriguez-fuse/pom.xml`, `rodriguez-build/pom.xml` (each `<parent><version>`)
3. **Explicit dependency versions in rodriguez-jdbc** — `rodriguez-jdbc/pom.xml` (`rodriguez-core` dependency)
4. **Explicit dependency versions in rodriguez-build** — `rodriguez-build/pom.xml` (`rodriguez-core`, `rodriguez-jdbc`, `rodriguez-aws-sdk`, `rodriguez-fuse` dependencies)

All occurrences of the old version string in `**/pom.xml` must be replaced. Verify with:

```bash
grep -r "0\.OLD\.0" --include="pom.xml"
```

After updating, run `mvn clean compile` to verify the build succeeds.
