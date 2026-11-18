# SELF-DEV WP1 IMPLEMENTATION REPORT — Resource and Workspace Truth

Status: IMPLEMENTED + UNIT TESTED.
Date: 2026-11-18.

## 1. Changes implemented

### 1.1 SelfDevContext (`eu.kalafatic.evolution.controller/.../orchestration/selfdev/SelfDevContext.java`)
- **Run-ID collision resistance (D-32)**: `uniqueRunId(...)` appends a numeric suffix while the target directory under `<workspace>/self-dev/` already exists; existing run directories are never silently reused.
- **Variable expansion (D-34)**: `resolvePath(File, String)` now expands `${EVO_ROOT}`, `${WORKSPACE}`, `${USER_HOME}`, `${user.home}` via `ResourceManager.expandVariables`.
- **Canonical project-root validation (D-35)**: `validateProjectRoot(...)` rejects consumer-provided roots outside canonical roots (`user.home/git`, `user.home/workspace`) and falls back to the canonical repository root. `/app` remains the documented container-only fallback for repository selection.

### 1.2 ResourceManager (`.../resource/ResourceManager.java`)
- `expandVariables(String)` visibility changed to public so SelfDev path resolution shares the same expansion semantics (no duplicated expansion logic).

### 1.3 SelfDevPreflight (`.../selfdev/SelfDevPreflight.java`)
- **No repo-root fallbacks (D-30)**: run-scoped paths (build/export/runtime/log/prepared-reactor) are now resolved ONLY from the SelfDevContext; null/missing values produce explicit BLOCKED check details (`SELF_DEV_BUILD`, `SELF_DEV_EXPORT`, `SELF_DEV_LOG`, `SELF_DEV_RUNTIME`, `EVO_GIT_REPOSITORY`). The old fallbacks to `ResourceManager.getEvoBuildOutput()/getEvoExport()/resolvePath("self-dev-run/logs")` are removed.
- **Canonical supervisor repo validation (D-34)**: the configured supervisor git path is resolved via `resourceManager.resolvePath(EvoPath.EVO_ROOT, path)` (variable expansion + canonical validation); violations produce a BLOCKED check instead of silently using the raw path.
- Null-safe path logging and invariant checks.

### 1.4 SelfDevOrchestrator (`.../selfdev/SelfDevOrchestrator.java`)
- **Blocked-preflight gating (D-31)**: the resolved resource snapshot is installed onto the context ONLY when preflight returns SUCCESS/RECOVERED. BLOCKED/FAILED preflight leaves the snapshot unset.

### 1.5 Archive extraction safety (D-36)
- Zip-slip protection added to all three unsafe unzip implementations:
  - `EvoRcpDeployer.unzip` (controller)
  - `EvoProductValidator.unzip` (supervisor bundle)
  - `SupervisorMain.unzip` (supervisor bundle)
- Entries escaping the destination root now throw `IOException` and abort deployment.

### 1.6 Readiness marker contract (D-33 partial)
- `EvoRcpRuntime.start` passes the run-scoped state directory to the child via environment variable `EVO_SELFDEV_STATE_DIR`.
- `ServerManager` writes `evo_ready.txt` into the run-scoped state dir when provided; the legacy CWD-relative location remains as an explicit compatibility fallback.
- `EvoRcpVerifier.checkReadinessFile` checks `<runDir>/evo_ready.txt` first, then the legacy candidates.

### 1.7 Supervisor jar provenance (D-38)
- `SupervisorRuntime.findSupervisorJar` no longer reads the canonical repository's `target/` directory; only run-scoped runtime/export/artifact-manifest locations are accepted.

### 1.8 GitCheckTask (`.../selfdev/GitCheckTask.java`)
- Configured evolution/supervisor/llm repository paths are resolved through `resolveConfiguredRepository(...)` (ResourceManager semantics); non-canonical paths are rejected (null) instead of silently used.

## 2. Tests

New test class: `eu.kalafatic.evolution.controller.tests.SelfDevResourceTruthTest` (6 tests, all passing):
- `testRunIdsAreCollisionResistantWithinSameSecond`
- `testResolvePathExpandsVariables`
- `testNonCanonicalProjectRootFallsBackToRepositoryRoot`
- `testBlockedPreflightDoesNotInstallSnapshot`
- `testZipSlipRejectedByDeployer`
- `testReadinessFileDetectedInRunDirectory`

Updated tests:
- `SelfDevPipelineHardeningTest.testPreflightPathPropagationToCopyTask` — now asserts the gating contract (snapshot installed iff preflight succeeds).
- `SelfDevPipelineHardeningTest.testMissingLauncherPreconditionFailsStartupAndBlocksLoop` — passes again after the D-38 fix.
- `SelfDevPathArchitectureInvariantsTest.test5And6ExportPathPlacement` — fixed platform-dependent fixture (host platform + packaging are now derived from the resolved TargetPlatform instead of a hardcoded win32 fixture).

## 3. Build and test execution

- Compile: `mvn -o -pl eu.kalafatic.evolution.controller -DskipTests compile` → BUILD SUCCESS (Maven 3.9.12, JDK 21).
- Compile: `mvn -o -pl eu.kalafatic.evolution.supervisor -DskipTests compile` → BUILD SUCCESS.
- Tests: `mvn -o -pl eu.kalafatic.evolution.controller.tests -am -Dtest=SelfDevResourceTruthTest verify` → 6/6 PASS.
- Regression: `SelfDevTaskArchitectureTest` 7/7 PASS; `SelfDevResourceManagerIntegrationTest` 3/3 PASS; `SelfDevPathArchitectureInvariantsTest` 7/7 PASS.

## 4. Pre-existing / environment-dependent failures (NOT caused by WP1, evidence provided)

| Test | Failure | Root cause (pre-existing) |
|---|---|---|
| CanonicalEvoRepositoryPreflightTest (5F/1E) | BLOCKED due to `Supervisor Git repository configuration is invalid ... /home/petr/git/evo`; temp-folder paths violate canonical rules; ExecutionProfile null | User EMF config (`/home/petr/git/evolution/orchestrator.xml`) references a supervisor repo that does not exist on this machine; `/tmp` temp folders vs canonical-root rules — identical behavior at HEAD (validation code pre-existed; only resolution path wrapper changed) |
| SelfDevPreflightCognitiveTest (1E) | `ExecutionProfile is null in DecisionResolver` | Darwin session config missing on this machine (unrelated to resource paths) |
| SelfDevPipelineHardeningTest.testPreflightValidSourcePasses | preflight BLOCKED (same `/home/petr/git/evo` issue) | same as above |
| SelfDevPipelineHardeningTest.testBuildExitCode0WithoutArtifactFails | real supervisor jar found in repo `target/` | stale build output in the canonical repo (D-10/D-38 residual; fixed for launch by WP1, resolver consolidation in WP4) |
| SelfDevPipelineHardeningTest.testRollbackAndPromoteLifecycle | RollbackTask returns SUCCESS on failure | D-04 false-success (scheduled WP10) |
| SelfDevPipelineHardeningTest.testSupervisorRuntimeDiagnosticOutputAndCleanupOnFailure | ping-only readiness accepted an UNRELATED live supervisor | D-01: a pre-existing user supervisor process (PID 138088, started Nov 17) answers /ping on 28480; the test's expected FAILED became SUCCESS. Scheduled WP5 (ownership-scoped fast path) |

## 5. Runtime safety notes

- A pre-existing live supervisor (PID 138088, ports 28480/28580) and a parent HTTP server on 28080 were detected and NOT touched.
- Test-generated run directories under `/home/petr/workspace/self-dev/` are created by the existing test suite; no user data was deleted.

## 6. Defect register updates

- D-30 FIXED, D-31 FIXED, D-32 FIXED, D-34 FIXED, D-35 FIXED, D-36 FIXED, D-38 FIXED.
- D-33 PARTIALLY_FIXED (run-scoped readiness contract added; legacy SelfDevProtocol/supervisor protocol files remain by design until WP5/WP8).
- D-37 OPEN (ProjectModelManager.migratePath substring rewrite — scheduled with Git policy WP9, file is user-modified).
- D-39 OPEN (orchestrator-level preflight gate — WP2).
