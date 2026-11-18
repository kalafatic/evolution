# SELF-DEV WP10 IMPLEMENTATION REPORT — Rollback and Recovery

Status: IMPLEMENTED + UNIT TESTED (Git-local steps simulation-only).
Date: 2026-11-18.

## 1. Changes implemented

### 1.1 Honest scoped RollbackTask (D-04)
- Explicit scopes: `RollbackScope.PROCESS_ONLY, DEPLOYMENT, SOURCE_COPY, GIT_LOCAL`; the default task covers all four, scoped construction supported.
- Every step produces an explicit recorded result (`PROCESS_ONLY.STOP_EVO`, `PROCESS_ONLY.STOP_SUPERVISOR`, `DEPLOYMENT.RESTORE`, `GIT_LOCAL.RESTORE`, `RECOVERY.RESTART`) with per-step diagnostics.
- The final result is SUCCESS only when every requested critical step succeeded; any failure aggregates into FAILED. Exceptions are never swallowed.

### 1.2 Deterministic known-good selection (D-43)
- `PromoteTask` now maintains `<export>/promoted/promoted-index.json` with {path, buildId, sha256, revision, promotedAt} entries.
- `PromoteTask.findNewestVerifiedPromoted(promotedDir)` selects the newest indexed entry whose file exists AND whose checksum verifies; tampered entries are skipped. Index-less fallback uses newest-modification-time ordering — never `files[0]`.

### 1.3 Deployment restore with verification
- The known-good is checksum-verified BEFORE anything is touched; the current deployment is only then replaced; the restored deployment must pass `EvoRcpDeployer.validateDeployment` layout validation; mismatch/validation failures produce FAILED with explicit deploymentState (`NO_KNOWN_GOOD`, `CHECKSUM_MISMATCH`, `RESTORED_INVALID_LAYOUT`, `RESTORE_EXCEPTION`, `RESTORED_PREVIOUS_KNOWN_GOOD`).

### 1.4 Failed-startup recovery sequence
- When the run owned an EVO child before rollback (ownership record present), after a successful deployment restore the task restarts the restored runtime and requires readiness (`waitUntilReady`, default APPLICATION_INITIALIZED); failure is recorded as `RECOVERY.RESTART` failure.

### 1.5 Git-local recovery (simulation-only)
- Source-copy restoration uses the WP9 boundary: `GitBaseline` + `restoreIterationBaseline` (simulated by default); a missing baseline fails the rollback honestly instead of executing destructive reset/clean.

### 1.6 Artifact discovery isolation
- `MavenSupervisorBuilder.getArtifact` no longer discovers artifacts inside the canonical repository directory (D-38 residual; fixes the environment-dependent `testBuildExitCode0WithoutArtifactFails`).

## 2. Tests

New test class: `SelfDevRollbackTest` (5 tests, all passing):
- promoted index newest-verified selection (+ tampered-newest fallback)
- rollback fails honestly without a verified known-good
- PROCESS_ONLY rollback is an honest no-op with diagnostics
- GIT_LOCAL rollback fails without a baseline
- SOURCE_COPY-only scope does not touch DEPLOYMENT

Updated: `SelfDevPipelineHardeningTest.testRollbackAndPromoteLifecycle` now exercises the scoped honest rollback (PROCESS_ONLY no-op success) and promotion pre-validation via `execute` (candidate missing ⇒ FAILED).

## 3. Build and test execution

- Controller bundle compile: BUILD SUCCESS.
- `SelfDevRollbackTest` 5/5 PASS; `SelfDevGitLifecycleSimulationTest` 9/9 PASS; `SelfDevBuildPolicyTest` 7/7 PASS.
- `SelfDevPipelineHardeningTest` now 12/14 PASS; the 2 remaining failures are the documented environment-dependent ones (user supervisor EMF config `/home/petr/git/evo`; live unrelated supervisor answering /ping).

## 4. Defect register updates

- D-04 FIXED (rollback honest: per-step results, aggregated failure, no swallowed exceptions).
- D-43 FIXED (deterministic verified known-good selection via promoted index + checksum).
