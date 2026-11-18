# SELF-DEV WP2 IMPLEMENTATION REPORT — Task Registry, Graph and Preflight

Status: IMPLEMENTED + UNIT TESTED.
Date: 2026-11-18.

## 1. Changes implemented

### 1.1 Task registry (SelfDevOrchestrator.java)
- **Canonical IDs only**: alias IDs `BUILD`, `MAVEN`, `EXPORT`, `DEPLOY` removed after verifying consumers (only one test referenced `BUILD`; updated). Registry: 30 canonical tasks.
- **Duplicate-ID detection**: new `register(task)` helper throws `IllegalStateException` on duplicate registration.
- **Static DAG validation**: `validateTaskGraph()` runs at construction and detects unknown dependencies, self-dependencies and cycles (DFS with visiting/visited sets); errors exposed via `getGraphValidationErrors()`.

### 1.2 Preflight gate (D-39)
- `executeTaskWithDependencies` now enforces a preflight gate for every execution entry point: when no valid resolved-resource snapshot exists, preflight runs automatically (once per orchestrator) and a failed preflight returns `TaskResult.blocked(...)` with an explicit gate reason. In production the snapshot can only be installed by a successful preflight (WP1 gating).

### 1.3 Forged START_EVO removed (D-01 chain)
- `SelfDevOrchestrator`: removed the "START_EVO satisfied via START_EVO_SUPERVISOR" fabrication block.
- `StartSupervisorTask`: START_EVO success is recorded ONLY after a verified `/start-evo` success; a failed `/start-evo` now returns FAILED (previously SUCCESS with the failure text embedded).

### 1.4 Result reuse policy
- Cached SUCCESS results are reused only for pure checks (`GIT`, `GIT_EVO`, `GIT_SUPERVISOR`, `LLM`, `PERMISSIONS`, `SUPERVISOR`). All process-affecting/build/export/deploy/start/stop tasks re-execute.

### 1.5 Cancellation (D-65 partial, D-64 partial)
- `TaskStatus` gained `CANCELLED`; `TaskResult.cancelled(...)` factory added.
- `SelfDevTask.isCancelled()` added; `AbstractSelfDevTask.checkCancelled()` throws `SelfDevCancellationException`; cancellation during execution yields a terminal CANCELLED result recorded in the context; pre-execution cancellation yields SKIPPED with reason (existing semantics preserved).

### 1.6 Honest loop tasks (D-06)
- `SupervisorLoopTask` / `SelfDevLoopTask` now perform a bounded health verification (5 ping attempts, 2 s apart) and their messages explicitly state that the autonomous iteration loop is managed elsewhere (Darwin engine / WP8). Failure is honest FAILED; success requires an actual ping.

### 1.7 Preflight task-graph validation (D-66)
- `SelfDevPreflight.validateTaskGraph` now consumes the orchestrator's real graph validation results; violations produce BLOCKED checks with concrete reasons instead of an unconditional "Valid DAG".

## 2. Tests

New test class: `SelfDevTaskGraphGateTest` (10 tests, all passing):
- alias removal + canonical IDs present + registry size 30
- duplicate registration rejected
- production graph has no validation errors
- unknown/self/cycle detection via injected tasks
- preflight gate blocks execution when preflight fails
- gate passes with a valid snapshot
- snapshot installed iff preflight succeeds (contract)
- cancellation during execution → CANCELLED (terminal, recorded)
- pre-cancel → SKIPPED with non-empty reason
- loop task fails honestly when endpoint unreachable (debug port offset)

Updated tests:
- `SelfDevTaskArchitectureTest`: `testStartEvoSupervisorDependencySatisfiesCreateProject` now asserts the no-forgery contract (BLOCKED, START_EVO stays FAILED, message identifies START_EVO); `testTaskDependencyEnforcement` now exercises the dependency path with a simulated COPY failure instead of launching the real repository copy + Tycho build (previously ~5 min real build per run); `setUp` installs a synthetic valid snapshot so the preflight gate passes deterministically.
- `SelfDevResourceManagerIntegrationTest`: dependency test uses canonical `BUILD_EVO`.

## 3. Build and test execution

- `mvn -o -pl eu.kalafatic.evolution.controller -DskipTests compile` → BUILD SUCCESS (Maven 3.9.12, JDK 21).
- `mvn -o -pl eu.kalafatic.evolution.supervisor -DskipTests compile` → BUILD SUCCESS.
- `SelfDevTaskGraphGateTest` 10/10 PASS; `SelfDevTaskArchitectureTest` 7/7 PASS; `SelfDevResourceTruthTest` 6/6 PASS; `SelfDevResourceManagerIntegrationTest` 3/3 PASS.

## 4. Runtime safety incident (evidence)

During an intermediate test run, the old dependency-resolution behavior executed the real pipeline (COPY → BUILD → EXPORT → DEPLOY → START_EVO) and launched a real EVO child process. Evidence: process 636597, PPID 9631 (test JVM), command line `.../run_181126_020712_2/runtime/evo/evolution/evo --port=28080 --mode=SELF_DEV`, start time 02:11 (this session's test run). Ownership verified; the child was terminated; parent processes were untouched. The test was corrected to use a simulated FAILED dependency result so the unit test never starts the real pipeline again. This incident motivates WP5 (persistent process ownership + readiness verification).

## 5. Pre-existing / environment-dependent failures (NOT caused by WP2)

| Test | Cause |
|---|---|
| SelfDevPipelineHardeningTest.testPreflightValidSourcePasses | user EMF config supervisor repo `/home/petr/git/evo` missing → preflight BLOCKED (pre-existing strict validation) |
| SelfDevPipelineHardeningTest.testBuildExitCode0WithoutArtifactFails | real supervisor jar in canonical repo `target/` (D-10; resolved in WP4) |
| SelfDevPipelineHardeningTest.testRollbackAndPromoteLifecycle | RollbackTask false-success (D-04; scheduled WP10) |
| SelfDevStandalonePipelineTest (4F) | hardcoded port 28080 vs allocated 44513 (live processes, PAM); deploy fixture lacks `configuration/config.ini`; stale Git-root expectation; overridden-run artifact missing |
| SelfDevHttpTasksTest (14E) | HTTP task tests require a live server setup absent in this environment (identical failure set before WP2) |
| SelfDevFlowTest (2F) | snapshot-integrity fixture and reflective bootstrap helpers (dead-code paths flagged in WP0) |
| CanonicalEvoRepositoryPreflightTest (5F/1E) | canonical-root rules vs /tmp fixtures + supervisor config (unchanged by WP2) |

## 6. Defect register updates

- D-39 FIXED (orchestrator-level preflight gate).
- D-66 FIXED (real DAG validation in preflight).
- D-06 FIXED (loop tasks made honest: bounded health verification with explicit semantics).
- D-65 PARTIALLY_FIXED (CANCELLED added as terminal status; WARNING/NOT_EXECUTED remain message-only by design; full enum consolidation across the three unrelated bundles is out of scope and would break EMF/supervisor consumers).
- D-64 PARTIALLY_FIXED (controller TaskStatus extended; the three enums serve three different subsystems and are not consolidated).
- D-01 PARTIALLY_FIXED (forged/reused START_EVO paths removed; the supervisor "already running" fast-path ownership fix is scheduled WP5).
- D-57 OPEN (UI rows from registry — WP11).
