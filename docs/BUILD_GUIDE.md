# EVO — Build Guide

All operations run from the authoritative parent: `/home/petr/git/evolution-121126`.

Prerequisites: JDK 21 (`/usr/lib/jvm/java-21-openjdk-amd64`), Maven 3.9+ (use `./mvnw`), network for p2 repos (Eclipse 2025-12, Orbit, GEF Classic).

## Operations

| Operation | Command (from repo root) | Output | Exit |
|---|---|---|---|
| Full Build (skip tests) | `./mvnw -B -DskipTests clean verify` | All 30 modules compiled; products materialized into `eu.kalafatic.evolution.repository/target/products/` | 0 = success |
| Clean Build | same as Full Build (`clean verify`) | — | 0 |
| Build Selected Module | `./mvnw -B -DskipTests clean verify -pl <module> -am` | module + its dependencies | 0 |
| Test | `./mvnw -B verify` | test bundles + supervisor tests | non-0 on failures |
| Validate | `./mvnw -B validate` | fast reactor validation | 0 |
| Package | `./mvnw -B -DskipTests package` | bundle jars + product assemblies | 0 |
| Product Build | `./mvnw -B -DskipTests install -pl eu.kalafatic.evolution.repository -am` | materialized products (linux/gtk/x86_64, win32) | 0 |

Environment: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`. Profiles: default `all-platforms` (win32+linux); `windows`/`linux` restrict the environment.

Scripts: `build.sh`, `build-product.sh|bat|ps1` (product assembly helpers).

## Verified results (2026-11-19)
- Full reactor `install -DskipTests`: **SUCCESS** (products materialized for linux + win32).
- UPM-executed `validate`: SUCCESS; UPM-executed `buildModule eu.kalafatic.utils`: SUCCESS.
- UPM-executed `test` (mvn verify): runs; `controller.tests` has pre-existing functional failures (assertions/ports/SQLite locks, no classpath errors).

## Failure reporting
Read the module summary at the end of Maven output (`Reactor Summary`); per-module `target/` artifacts and `eu.kalafatic.evolution.controller.tests/target/surefire-reports/*.txt` for test details.
