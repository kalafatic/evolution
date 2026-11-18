# SELF-DEV WP6 IMPLEMENTATION REPORT — RCP Readiness

Status: IMPLEMENTED + UNIT TESTED.
Date: 2026-11-18.

## 1. Changes implemented

### 1.1 Explicit readiness levels
- New `RcpReadinessLevel` enum (ordered): PROCESS_STARTED → PROCESS_ALIVE → HTTP_RESPONSIVE → SUPERVISOR_RESPONSIVE → OSGI_RESOLVED → REQUIRED_BUNDLES_ACTIVE → APPLICATION_INITIALIZED → APPLICATION_HEALTHY.

### 1.2 EvoRcpVerifier (D-02)
- `verifyReady(..., minimumLevel)` — success requires the configured minimum level to be actually verified; default minimum is APPLICATION_INITIALIZED.
- **Timeout is FAILED** and reports the highest level actually verified; the old "process alive ⇒ SUCCESS" fallback is removed.
- Level evidence: HTTP 200 ⇒ HTTP_RESPONSIVE; `/server/osgi` 200 without unresolved `eu.kalafatic.*` bundles ⇒ OSGI_RESOLVED + REQUIRED_BUNDLES_ACTIVE; readiness file / log markers / status body `RUNNING` ⇒ APPLICATION_INITIALIZED (+APPLICATION_HEALTHY with RUNNING status).
- Unresolved EVO bundles remain a hard startup failure.
- New `probeReadiness(runtimeDir, logDir, port, timeout)` — Process-free probe used for re-attaching to owned, already-running children.

### 1.3 EvoRcpRuntime
- **Adoption path (D-23)**: a running child is adopted only via the current run's ownership record (identity + effective port verified) AND a readiness probe ≥ APPLICATION_INITIALIZED; otherwise START fails with the highest verified level and directs to STOP_EVO first.
- **Occupied-port guard**: `isPortAvailable(...)` — an occupied effective server port prevents an unsafe direct launch with an explicit conflict failure.

### 1.4 Supervisor-side readiness (D-20)
- `SupervisorMain.handleStartEvo` (all three launch styles): the liveness-only/no-verification gates are replaced by `waitForEvoReadiness(process, port, 30)` requiring `/server/status` 200 + `RUNNING`. Success responses now include the verified port; unready children are destroyed and an error is returned.

### 1.5 Port availability honesty (D-25)
- `SupervisorMain.findAvailablePort` and `ProcessRunner.findAvailablePort` now return -1 (never the busy start port); all call sites guard against -1 with explicit conflict errors.

## 2. Tests

New test class: `SelfDevReadinessTest` (7 tests, all passing):
- timeout is FAILED (not alive-success) with PROCESS_ALIVE reported
- HTTP-without-app is FAILED with highest level HTTP_RESPONSIVE
- full readiness succeeds with APPLICATION_HEALTHY
- unresolved OSGi bundle fails startup
- minimum-level parameter controls success
- port availability detection
- readiness level ordering

## 3. Build and test execution

- Controller + supervisor bundles compile: BUILD SUCCESS.
- `SelfDevReadinessTest` 7/7 PASS; `SelfDevProcessOwnershipTest` 7/7 PASS.
- Supervisor bundle suite: 31/31 PASS.

## 4. Defect register updates

- D-02 FIXED (readiness levels; timeout ⇒ FAILED; default minimum APPLICATION_INITIALIZED).
- D-20 FIXED (supervisor-side HTTP readiness replaces liveness gates).
- D-23 FIXED (ownership+readiness-verified adoption; occupied port prevents duplicate launch).
- D-25 FIXED (findAvailablePort never returns a busy port; conflict reporting).
