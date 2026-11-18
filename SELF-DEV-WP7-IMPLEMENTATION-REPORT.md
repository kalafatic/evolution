# SELF-DEV WP7 IMPLEMENTATION REPORT — Port Isolation

Status: IMPLEMENTED + UNIT TESTED.
Date: 2026-11-18.

## 1. Changes implemented

### 1.1 One authoritative child-port contract (D-26)
- `SupervisorRuntime` now passes the explicit child EVO HTTP port to the supervisor process (`-Devo.http.port=<effective server port>`) alongside the supervisor API/control ports (`-Dport`, `-Dcontrol.port`).
- `SupervisorMain.EVOSupervisorServer.resolveChildEvoPort(base)` — the child port is taken from `-Devo.http.port` when the parent provides it; the legacy offset derivation (`this.port - 28480`) is retained only as a backward-compatibility fallback.
- `ProcessRunner.resolveChildPort()` — same contract for supervisor-side launches.
- All three EVO launch branches in `handleStartEvo` use the contract and report the verified child port in their success responses.

### 1.2 Hardcoded fallbacks removed (D-28)
- `SupervisorClient` context-less fallback now uses `PortRegistry.EVO_SUPERVISOR_API.getDefaultPort()` (centralized default) instead of a hardcoded `28480` literal.
- `ProcessRunner` launch paths use the contract-aware resolver instead of a raw `findAvailablePort(28080)`.

### 1.3 Busy ports are conflicts (D-25, completed)
- `findAvailablePort` (SupervisorMain + ProcessRunner) returns -1 when no port is free; every launch path converts -1 into an explicit conflict error and aborts. `EvoRcpRuntime.isPortAvailable` guards direct launches (WP6).

### 1.4 Documented offset schemes
- Debug offset +10 remains the only child-port offset (PortRegistry.DEBUG_PORT_OFFSET). The debug UI supervisor (+1) scheme remains as-is and is explicitly logged; it is an additional headless+UI pairing, not an EVO child port change.

## 2. Tests

New test class: `SelfDevPortIsolationTest` (3 tests, all passing):
- debug offset applied consistently across supervisor/control/server ports
- SupervisorClient targets the effective supervisor port; context-less fallback uses the centralized registry default
- debug ports differ from the production base

## 3. Build and test execution

- Controller + supervisor bundles compile: BUILD SUCCESS.
- `SelfDevPortIsolationTest` 3/3 PASS; `SelfDevReadinessTest` 7/7 PASS; `SelfDevTaskGraphGateTest` 10/10 PASS.
- Supervisor bundle suite: 31/31 PASS.

## 4. Defect register updates

- D-26 FIXED (explicit child-port contract; offset re-derivation removed from the primary path).
- D-28 FIXED (hardcoded supervisor-port fallback replaced with centralized default).
- D-27 DOCUMENTED (debug UI +1 pairing retained intentionally; unchanged).
- D-29 OPEN (UI displays actual allocated ports — WP11).
