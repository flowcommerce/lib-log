# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

lib-log (`lib-log-play29`) is a Scala logging library for Flow Commerce Play 2.9 services. It provides a fluent, injectable logger (`RollbarLogger`) that integrates with Rollbar for production error tracking and supports structured logging via Logstash/Sumo.

## Build Commands

```bash
sbt compile              # Compile
sbt test                 # Run all tests
sbt scalafmtAll          # Format all code (must run before pushing)
sbt "testOnly io.flow.log.LogUtilSpec"  # Run a single test class
sbt clean coverage test coverageReport  # Run tests with coverage report
sbt publishLocal         # Build and publish locally
```

## Code Coverage

Enforced minimums: 25% statement, 15% branch. Build fails if not met.

## Code Style

- Scalafmt 3.5.9 with `scalafmtOnCompile := true` (auto-formats on compile)
- Max line width: 120 characters
- Trailing commas: always
- Alignment: none
- Scala 2.13 dialect
- Compiler uses `-Xfatal-warnings` — all warnings are errors

## Architecture

All code lives in package `io.flow.log` under `src/main/scala/`. Four source files:

- **RollbarLogger** — Immutable case class with fluent builder methods (`.withKeyValue()`, `.fingerprint()`, `.organization()`, etc.). Only `warn` and `error` are sent to Rollbar; `debug` and `info` go to logs only. Supports frequency-based sampling via `.withFrequency()`. Use `RollbarLogger.SimpleLogger` in tests.

- **Rollbar** (`Rollbar.scala`) — Guice module (`RollbarModule`), provider (`RollbarProvider`), and factory (`RollbarFactory`). Uses AssistedInject factory pattern so `RollbarLogger` instances can be copied while sharing a single Rollbar notifier. Includes custom Jackson serializer for Play JSON types and custom fingerprint generator.

- **LogUtil** — Injectable utility for timing sync (`duration()`) and async (`durationF()`) operations with structured attributes. Supports frequency-based sampling.

- **SerializePlayJson** — Jackson module registration for Play JSON serialization.

## Key Patterns

- **Dependency injection**: Google Guice with AssistedInject for the logger factory
- **Immutable builders**: `RollbarLogger` is a case class; fluent methods return new copies via `.copy()`
- **Testing**: ScalaTest WordSpec with Matchers (`src/test/scala/io/flow/log/`)
- **No `return` statements** — strictly avoided per Scala conventions

## CI/CD

- **Jenkins**: `skeletonLibraryPipeline()` in `Jenkinsfile`
- **GitHub Actions**: PR title must start with a JIRA ticket (e.g., `FDN-1234`, pattern: `^[A-Z]{3,}-(?!0+\b)\d{3,6}\b`). Auto-merge workflow for PRs labeled `auto-merge`.

## Publishing

Artifacts publish to Flow Artifactory (`flow.jfrog.io`). Requires `ARTIFACTORY_USERNAME` and `ARTIFACTORY_PASSWORD` environment variables.
