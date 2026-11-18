# PHASE-7-DEFECT-REGISTER.md

Phase 7 — Defect Register
Date: 2026-11-17

Severity: P0 Critical / P1 Major / P2 Moderate / P3 Minor.
Status: FIXED (verified in Phase 7) / OPEN (needs product-owner or follow-up).

## Defects found and fixed in Phase 7

### D-1 — P1 — DCode had no ENR provider (default DCode→ENR path missing) — FIXED
- Affected: DCoder.
- Reproduction: DCode provider registry listed only Ollama; no way to route the
  coding harness through ENR. Requirement "DCode uses ENR as default inference
  provider" unmet.
- Expected: ENR registered as default provider, selectable alternatives.
- Actual: no ENR provider existed.
- Root cause: Phase 6 deferred DCode-side ENR integration.
- Correction: added `eu.kalafatic.dcode.config.provider.EnrProvider` (ENR 1.0 wire:
  `/health`, `/enr/v1/models`, `/enr/v1/chat`, bearer token, masked config);
  registered as default in `ConfigActivator` (ENR first, Ollama alternative).
- Regression coverage: `EnrProviderTest` (7 tests) + `EnrLiveIntegrationTest`
  (2 live tests) + live completion run through the production class.
- Verification: DCode suite 126/126 (1 skip); live DCode→ENR runs green.

### D-2 — P1 — Standalone ENR lacked the family `/enr/v1` surface — FIXED
- Affected: evo-native-runtime-161126.
- Reproduction: DCode/TRYST/DStudio clients expect ENR 1.0 (`/enr/v1/*`); the
  standalone server only served its private `/api/v1` — two incompatible "ENR 1.0"
  surfaces.
- Correction: added `FamilyApiHandlers` (health, models list/get/load/unload, chat,
  chat/stream SSE with terminal `event: done` + exchange close, runtime, providers,
  capabilities; TEIP error envelope) and registered the routes in `EnrServer`;
  `ModelManager.resolveModelPath`; root handler advertises both APIs.
- Regression coverage: `FamilyApiIntegrationTest` (5 tests) + live curl evidence.
- Verification: ENR full suite 70/70.

### D-3 — P2 — ENR request timeouts parsed but never enforced — FIXED
- Affected: enr-runtime.
- Root cause: `InferenceOptions.timeoutSeconds` and `RuntimeConfig.requestTimeoutSeconds`
  had no consumers.
- Correction: housekeeper scheduler + `armTimeout` (session cancel on expiry) for
  blocking and streaming inference.
- Regression coverage: `EnrRuntimeTest.requestTimeoutCancelsSlowGeneration`
  (deterministic CANCELLED + session cleanup).
- Verification: green.

### D-4 — P2 — ENR idle eviction defined but never scheduled — FIXED
- Correction: housekeeper invokes `SessionManager.evictIdle` every 30 s.
- Regression coverage: `EnrRuntimeTest.idleSessionsAreEvicted`.
- Verification: green.

### D-5 — P1 — DCode ignored session-level provider/model when no router default — FIXED
- Affected: DCoder harness.
- Reproduction: session created with `providerId=enr, modelId=tiny-evo` failed
  immediately with "No model configured" although the session carried an explicit
  provider/model (blocked the family ENR path).
- Root cause: `SessionExecutor.resolveModel` always consulted the router default.
- Correction: session-level selection wins when both ids are present.
- Regression coverage: live DCode+ENR run (session executed through ENR); full
  DCode suite green.

### D-6 — P2 — DCode `GET /health` bypassed the API (FHC 1.0 probe broken) — FIXED
- Affected: DCoder server.
- Reproduction: `GET /health` returned static-web-content 404 ("No web content
  configured") instead of the health JSON; TRYST's DCode health probe failed.
- Correction: `DCodeHttpServer.serve` dispatches `/health` to `DCodeApi` before the
  static-content path.
- Regression coverage: live TRYST→DCode→ENR run (probe succeeded, session executed);
  DCode suite green.

### D-7 — P3 — ENR tokenizer unboxing NPE for missing vocab entry — FIXED
- Correction: explicit guard with `IllegalArgumentException` message.
- Verification: ENR suite green.

## Open defects

### D-8 — P2 — Port convention collision: DCode HTTP vs DStudio TEIP both 48080 — OPEN
- Both products default to 48080 on loopback. Consequences: the second service to
  start binds elsewhere (DCode falls back) or fails (DStudio throws); adapters
  default to `localhost:48080`, so a fallback port silently breaks the default
  endpoint conventions.
- Proposed correction: assign DStudio TEIP the 48180 convention (its diagnostics
  port today) or DCode the 48180; update product defaults + adapter endpoints +
  tests. **Requires Product Owner approval** (family port convention).
- Mitigation in the meantime: configure `dstudio.teip.port` / `dcode` port
  explicitly.

### D-9 — P1 — DCode headless product launcher fails to acquire the application service — OPEN
- Affected: DCoder product assembly (`eu.kalafatic.dcode.server.headless`).
- Reproduction: launch the built headless product (Java 21) → `IllegalStateException:
  Unable to acquire application service. Ensure that the org.eclipse.core.runtime
  bundle is resolved and started`; no bundle-resolution errors in the log.
- Evidence: three launch attempts (fresh `-data`, `-clean`, Java 21) — same error.
- The identical stack boots green under the Equinox test harness
  (`OsgiWiringTest`, `HeadlessRuntimeWebTest`).
- Root cause: unresolved (product config.ini/p2 profile issue suspected — not
  investigated to closure within this phase; the suite harness provides the working
  runtime path).
- Proposed correction: compare the product p2 profile against the working test
  launch (start levels/`osgi.bundles` ordering); add a product-launch smoke check.
- Workaround used for acceptance: the full server stack runs in-process (live
  headless server executed during Phase 7 — see WP-7.7).

### D-10 — P3 — EVO Linux product: launch verified, full GUI session not exercised — PARTIALLY RESOLVED
- The self-dev pipeline had materialized both Linux products; Phase 7 extracted and
  launched them: the full RCP product started on X display :1 (portal served,
  background server bound with port fallback, readiness marker written) and the
  headless product started headless. The Darwin TEIP/API routes were not reachable
  in the fallback-port configuration used, and the host's 28080 is occupied by a
  pre-existing third-party service (Open WebUI), so the full EVO GUI development
  session (Scenario A) remains NOT TESTED.
- Proposed follow-up: run EVO on a host with the canonical ports free and execute
  Scenario A live.

### D-11 — P3 — TRYST documentation inconsistency (TCP port 9090 vs code 38280) — OPEN
- `TRYST_OPERATIONAL_CONFIGURATION.md` line 20 states the TCP default as 9090; code
  uses 38280 (`ApplicationConfiguration`, `ServicePortIdentifier`).
- Proposed correction: update the documentation.

### D-12 — P2 — EVO: ENR is selectable but not the unconditional default — OPEN (policy)
- `LlmRouter` routes to ENR only on explicit selection (`provider=enr`, `engine=enr`,
  `enr:` prefix); the direct Ollama/native paths remain the default for pre-existing
  EVO inference flows. Documented no-silent-switching rule (Phase 6).
- Phase 7 requirement "EVO uses ENR as default" is satisfied for the ENR-capable
  paths (registration, discovery, selection); making ENR the default for every EVO
  inference path requires Product Owner approval.
- Proposed correction (pending approval): default `LlmRouter` fallback to the ENR
  provider when available and no explicit provider was configured.

### D-13 — P3 — ENR CUDA tests fail while another CUDA consumer holds the GPU — OPEN (environment)
- When an unrelated Ollama model occupies ~14.3 of 16.3 GiB VRAM, ENR CUDA context
  creation fails (`cuCtxCreate failed: 2`) in 3 tests. With VRAM free the same
  tests pass 9/9. Correct reporting, but concurrent CUDA consumers need VRAM
  coordination (documented in `docs/ENR-COMPUTE-BACKENDS.md`).

## Pre-existing failures (not introduced by Phase 7)

- EVO `controller.tests`: 20 pre-existing red classes (27 failures + 18 errors
  across the classes executed; environment/LLM-dependent or stale assertions) —
  recorded in WP-7.10 with the full list. Not caused by Phase 7 changes (same set
  as the Phase 6 baseline).
- `HuggingFaceAccountingAndLifecycleTest` runs an unbounded mock dataset-acquisition
  loop (offsets grew beyond 121M rows) and never terminates; excluded from the
  targeted regression run and recorded here as a pre-existing test defect.
