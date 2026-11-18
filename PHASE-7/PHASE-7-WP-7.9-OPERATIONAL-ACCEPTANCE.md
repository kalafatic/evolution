# PHASE-7-WP-7.9-OPERATIONAL-ACCEPTANCE.md

Phase 7 — WP-7.9 Operational and Lifecycle Acceptance
Date: 2026-11-17
Status: COMPLETE

## 1. Lifecycle matrix

| Product | Startup | Readiness | Discovery | Shutdown | Restart | Health | Logging |
|---|---|---|---|---|---|---|---|
| EVO | RCP `Application` / `HeadlessApplication` (product defined; Linux product not materialized in this env — see D-10) | TEIP `/teip/v1/status`, health capabilities | `CapabilityRegistry`, TEIP capabilities | OSGi stop | suite-covered | capability health + server health | RCP log |
| TRYST | `TrystServerApplication` stages | `/api/status` (UP) — **live** | ENR model discovery at startup — **live** | disposal cascade | **live** (DB persisted across restarts) | `/api/status`, `ServerReadinessService` | startup diagnostics — **live** |
| DStudio | `DStudioServerApplication`/`-headless` | `/api/status` (UP) — **live** | harness+engine registries | clean stop | reconcile-on-restart | diagnostics endpoint | console — **live** |
| DCode | lifecycle stages BOOTSTRAP→CLIENTS | `/health` (UP, per-stage) — **live** | OSGi services | `DefaultDCodeLifecycle` stop | recovery service (suite) | `/health`, `/api/v1/status` | structured log — **live** |
| ENR | `EnrCli start` (pid file, shutdown hook) | `/enr/v1/health` (UP) — **live** | `Backends` ServiceLoader; model dirs | `EnrRuntime.shutdown()` cleans sessions/models | **live** | health 200/503 | JUL — **live** |

Startup ordering: each product starts independently; TRYST/DStudio/DCode tolerate a
missing ENR (best-effort discovery, degraded state, clear diagnostics). Shutdown
ordering: applications stop their own servers; ENR stops last independently.

## 2. Ports

| Port | Owner | Allocation | Conflict handling |
|---|---|---|---|
| 28080/28180/28280/28380/28480/28580/28680 | EVO | `PortRegistry` + `PortAllocationManager` (persisted) | validated assignment, fallback |
| 28780 | ENR family surface | `PortRegistry.EVO_ENR` | +1..+100 fallback (embedded); standalone: clear "Address already in use" error — **live-verified** |
| 38080/38280 | TRYST | `PortAllocationGroup` | dynamic fallback 38080–38580; port 8080 forbidden — **live: bound 38280 with persisted offset 200** |
| 48080/48180 | DStudio TEIP | `DStudioConfiguration` | no fallback (throws; documented D-5) |
| 48080/48380/48680 | DCode | `DCodePorts` | 500-port fallback range; `dcode.server.port` published |
| 8111 | ENR standalone native API | `enr.yaml` | fixed |

## 3. Mandatory failure scenarios

| # | Scenario | Result | Evidence |
|---|---|---|---|
| 1 | ENR unavailable during application startup | PASS | TRYST starts, ENR provider health OFFLINE; DCode registers ENR provider but works with alternatives; DStudio engine status UNKNOWN |
| 2 | ENR unavailable during active coding | **PASS (live)** | DCode stayed UP; session FAILED gracefully with `ProviderException`; no hang |
| 3 | DCode unavailable during application startup | PASS | TRYST DCode descriptor status UNKNOWN; adapter health probe fails cleanly; alternative selection possible |
| 4 | DCode terminates during execution | PASS (suite) | TRYST adapter timeout/cancellation handling; task lifecycle continues without corruption (live: unreachable DCode → task completed flow with empty review) |
| 5 | Model unavailable | PASS | ENR 400/404 envelope (`NOT_FOUND`/`VALIDATION_FAILED`) — live-verified `/enr/v1/models/missing` 404 |
| 6 | Model loading failure | PASS | ENR `LOAD_FAILED` 422 + `IllegalArgumentException` 400 paths (suite) |
| 7 | GPU unavailable | PASS | `selectBackend` falls back to CPU with warning (`allowCudaFallback`); CUDA unavailable → clean `IllegalStateException` |
| 8 | Inference timeout | PASS | Phase 7 fix: request timeouts now enforced; deterministic test `requestTimeoutCancelsSlowGeneration` (CANCELLED + session cleanup) green |
| 9 | Network interruption | PASS | stateless HTTP clients; ENR 503 degraded health; DCode SSE reconnect/dedupe |
| 10 | Database unavailable | PASS (suite) | SQLite busy timeout/WAL; migration abort safety |
| 11 | Port conflict | **PASS (live)** | ENR standalone reports "Address already in use" (clear error); DCode PortAllocator fallback (suite); TRYST dynamic fallback (suite) |
| 12 | Task cancellation | **PASS (live)** | DCode `POST /sessions/{id}/cancel` accepted twice (idempotent), session terminated |
| 13 | Application termination during execution | PASS (suite) | DCode recovery marks interrupted sessions FAILED; TRYST `TaskRecoveryService` |

Graceful degradation and recovery verified. Applications report missing defaults
clearly (descriptor status, connection-test messages, ENR health). No silent
provider switching was observed (configured policy respected — EVO router explicit
selection, TRYST/DStudio selectable defaults).

## 4. Resource management (see WP-7.11 for measurements)

- ENR: session admission limit (8 default), idle eviction now scheduled (Phase 7),
  VRAM accounting, weight cache; DCode: fixed 4-thread executor; TRYST: worker pool
  = cores/2, sandbox cleanup; thread/process management covered by suites.
- Live footprints: ENR JVM ≈ 60 MB RSS (models loaded), DCode live server ≈ 146 MB
  RSS, TRYST/DStudio headless within normal JVM ranges.

## 5. Defects

- D-9: DCode headless product launcher fails with "Unable to acquire application
  service" (P1) — the same stack boots green under the Equinox test harness;
  product config.ini/p2 profile needs investigation.
- D-10: EVO Linux product not materialized in this environment (only win32 product
  output present); headless product definition exists.
