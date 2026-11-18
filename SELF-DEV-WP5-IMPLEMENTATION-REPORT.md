# SELF-DEV WP5 IMPLEMENTATION REPORT — Supervisor Process Ownership and Lifecycle

Status: IMPLEMENTED + UNIT TESTED.
Date: 2026-11-18.

## 1. Changes implemented

### 1.1 Persistent ownership records (D-16)
- New `ProcessOwnershipRecord` (controller selfdev package): immutable record with kind (SUPERVISOR/EVO_RCP), runId, supervisor instance ID, PID, parent PID, executable, command line, working directory, runtime path, start time, effective ports and exit status.
- Persisted per run at `<run>/runtime/process-ownership.json` (one entry per kind). `write/read/clear` helpers; `clear` records the exit status.

### 1.2 Ownership verification (D-17/D-18)
- `ProcessOwnershipRecord.verifyIdentity(record)`: a PID is valid only if the live process handle exists, is alive, and its command line/executable matches the recorded markers (supervisor jar path / java marker). Stale or recycled PIDs fail verification.
- `terminateOwnedProcessTree(record)`: terminates descendants + the process ONLY after identity verification; otherwise refuses and logs.
- `isOwnedByRun(record, context)`: run-scoped ownership check.

### 1.3 SupervisorRuntime lifecycle
- **Fast path corrected (D-01/D-17)**: "already running" is accepted ONLY when the current run's ownership record exists, PID identity verifies, the endpoint pings, AND the recorded effective ports match. Foreign/stale supervisors are never adopted.
- **Launch**: writes the ownership record (PID, parent PID, command line, workdir, runtime path, effective ports); registers the record in a JVM-level set; installs a one-time JVM shutdown hook that identity-verified-terminates owned children (parent never leaves unmanaged children on graceful exit).
- **Stop (D-18/D-19)**: graceful-first (HTTP `/shutdown` when the owned endpoint responds) → identity verification → bounded force escalation (descendants first) → persisted exit status. A live PID with mismatched identity is REFUSED with FAILED (never killed); no-ownership is an honest no-op.

### 1.4 EVO child ownership
- `EvoRcpRuntime.start` persists the EVO ownership record (server port, command, workdir, runtime path); failed launches record the exit status.
- `EvoRcpRuntime.stop` is ownership-record-first: when the JVM has no live handle, only the identity-verified record PID is terminated; mismatch → FAILED (refusal).

### 1.5 Supervisor bundle
- `SupervisorMain.EVOSupervisorServer`: implemented `/shutdown` (graceful: stops owned EVO child, then exits) — the previously unimplemented endpoint (D-19).
- `handleStopEvo`: descendants-first graceful stop with bounded force escalation (D-21).
- Duplicate-launch prevention (D-22): `ProcessRunner.isRCPRunning()` + `EVOSupervisorServer.isActiveEvoRunning()`; `SelfDevSupervisor.restart` skips the relaunch when an owned EVO child is already running (idempotent RESTART).

## 2. Tests

New test class: `SelfDevProcessOwnershipTest` (7 tests, all passing):
- ownership record round trip (persistence, fields, run-scoped check, exit status)
- identity verification rejects dead/unknown PIDs
- identity verification rejects mismatched command lines
- terminate refuses mismatched records and the parent JVM survives
- STOP refuses a mismatched live PID (FAILED, "refused") — parent survives
- STOP no-op without ownership
- EVO ownership round trip

## 3. Build and test execution

- Controller + supervisor bundles compile: BUILD SUCCESS.
- `SelfDevProcessOwnershipTest` 7/7 PASS; `SelfDevArtifactManifestTest` 7/7 PASS.
- Supervisor bundle suite: 31/31 PASS (including Endpoint/start-evo tests with the live-supervisor environment).

## 4. Runtime safety

- The pre-existing user supervisor (PID 138088, ports 28480/28580) and parent server (28080) were NOT touched; the new fast path refuses to adopt them for any test run.
- All termination tests exercise refusal paths (mismatched identity) — no real process was killed by tests.

## 5. Defect register updates

- D-16 FIXED (persistent per-run ownership records + JVM shutdown hook).
- D-17 FIXED (run-scoped ownership gate replaces static-handle adoption).
- D-18 FIXED (identity verification before termination; refusal semantics).
- D-19 FIXED (`/shutdown` implemented; graceful-first bounded escalation).
- D-21 FIXED (descendants-first stop on the supervisor side).
- D-22 FIXED (idempotent restart guard).
- D-24 FIXED (ownership decisions now use ProcessOwnershipRecord).
- D-01 PARTIALLY_FIXED (all forged/reused START_EVO paths removed in WP2; the supervisor fast path is now ownership-gated; readiness verification completes in WP6).
- D-23 PARTIALLY_FIXED (EVO ownership record + stop refusal; readiness levels in WP6).
- D-20 OPEN (supervisor-side readiness verification — WP6).
- D-25, D-28 OPEN (port availability/fallbacks — WP7).
