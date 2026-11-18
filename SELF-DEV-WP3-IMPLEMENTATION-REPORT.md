# SELF-DEV WP3 IMPLEMENTATION REPORT — Build Pipeline

Status: IMPLEMENTED + UNIT TESTED.
Date: 2026-11-18.

## 1. Changes implemented

### 1.1 Build policy (new class)
- `SelfDevBuildPolicy` (controller selfdev package): explicit policy model.
  - Default: `TESTS_ENABLED` — tests run in every self-dev build.
  - Explicit escape hatch only: `-Devo.build.skipTests=true` or `-Devo.build.policy=smoke` produces `SMOKE_SKIP_TESTS`; `-Devo.build.policy=tests-on` wins over the escape hatch.
  - A smoke build is never usable as evidence of test success (policy recorded in build diagnostics).

### 1.2 Defaults corrected (D-03)
- `TychoEvoRcpBuilder`: `skipTests` field default changed `true -> false`; `isSkipTests()` now consults the build policy.
- `BuildEvoTask`: skipTests derived from `SelfDevBuildPolicy`.
- `MavenSupervisorBuilder`: hardcoded `-DskipTests` removed; policy-driven.
- `GenomeCheckTask`: hardcoded `-DskipTests` removed; policy-driven.
- Supervisor bundle `ProcessRunner.runBuild`: hardcoded `-DskipTests` removed; explicit `evo.build.skipTests` property only.
- Supervisor bundle `BuildConfiguration`: `skipTests` default `true -> false` (explicit property only).

### 1.3 Supervisor reactor isolation (D-13)
- `MavenSupervisorBuilder`: the `-pl supervisor -am` reactor build now runs against the isolated prepared reactor (`<run>/source`) instead of the canonical repository root. A new guard refuses any build when `supervisorDirectory` resolves to the canonical repository root.
- `GenomeCheckTask`: prefers the isolated reactor; falls back to the genome module dir only when no isolated reactor exists.

### 1.4 EXPORT never builds implicitly (D-11)
- `TychoEvoRcpBuilder.exportProduct`: removed the embedded Maven re-build. A missing build artifact now produces an explicit FAILED result directing the operator to run `BUILD_EVO` first.

### 1.5 UI command descriptions (D-15)
- `DevelopmentPage` row commands updated to the actual executed semantics:
  - MAVEN_EVO / BUILD_EVO: `mvnw validate clean verify -P<platform>` with tests enabled by default.
  - BUILD_SUPERVISOR: `mvn validate clean install -pl ... -am (isolated reactor)`.
  - EXPORT_EVO / EXPORT_SUPERVISOR: "materialize exact BUILD_* artifact ... (no implicit build)".

## 2. Tests

New test class: `SelfDevBuildPolicyTest` (7 tests, all passing):
- default policy enables tests
- explicit smoke escape hatch
- policy property precedence (`smoke` / `tests-on`)
- TychoEvoRcpBuilder defaults to tests enabled
- EXPORT fails fast without implicit Maven build (elapsed < 15 s)
- supervisor build refuses canonical repo root
- supervisor build prefers isolated reactor

Updated test:
- `SelfDevTaskArchitectureTest.testMavenEvoTaskSemanticsAndSkipTests` asserts the new default (tests enabled).

## 3. Build and test execution

- Controller + supervisor + view bundles compile: BUILD SUCCESS (Maven 3.9.12, JDK 21).
- `SelfDevBuildPolicyTest` 7/7 PASS.
- `SelfDevTaskGraphGateTest` 10/10 PASS; `SelfDevResourceTruthTest` 6/6 PASS.
- Supervisor bundle test suite: 31/31 PASS (SelfDevSupervisorTest 10, ExportAndStartEvoEndpointTest 3, EvoValidatorTest 4, SupervisorModeTest 3, ResultReaderTest 2, tool tests).

Note: `ExportAndStartEvoEndpointTest` logs `[HTTP] ERROR: Failed to start HTTP server: Address already in use` (pre-existing live supervisor on 28480); tests still pass.

## 4. Defect register updates

- D-03 FIXED (tests enabled by default; explicit escape hatch only).
- D-13 FIXED (supervisor/genome reactor builds isolated from the canonical repo).
- D-11 FIXED (EXPORT never triggers an implicit build).
- D-15 FIXED (UI command descriptions match executed commands).
