# PHASE-7-WP-7.3-DCODE-ENR-NATIVE-INTEGRATION.md

Phase 7 — WP-7.3 DCode and ENR Native Integration
Date: 2026-11-17
Status: COMPLETE (with explicit limitations)

## A. DCode integration (harness side)

Verified by inspection and by the executed DCode test suites (124 tests, 0 failures):

| Check | Result | Evidence |
|---|---|---|
| Native initialization | PASS | `ConfigActivator` / `HarnessActivator` wiring, lifecycle stages `CORE_SERVICES`→`PROVIDERS` |
| Service discovery | PASS | OSGi service registry (manual wiring convention) |
| Harness registration | PASS | FHC 1.0 adapters registered in EVO `ToolFactory`, TRYST `ApplicationLifecycleCoordinator`, DStudio `DStudioLifecycleCoordinator` |
| Default selection | PASS | DCode auto-selected when no explicit selection (all three apps); explicit selections preserved |
| Configuration | PASS | TRYST V20 `dcode_configurations`; DStudio `harness_config`; EVO `connected-applications.json` |
| Session integration | PASS | FHC 1.0 `/api/v1/sessions` (CREATE→RUNNING→WAITING_FOR_APPROVAL/FAILED/CANCELLED) |
| Context handling | PASS | `ContextEngine` bounded context; per-repo summaries |
| Tool registration | PASS | 24 tools (11 base + 13 JDT/PDE) via one `ToolRegistry` |
| File operations | PASS | `ToolEngine.resolveContained` workspace containment (traversal/symlink rejection) |
| Git operations | PASS | JGit engine (clone/status/diff/commit/pull/push/branches); READ_ONLY policy enforcement |
| Execution lifecycle | PASS | `SessionExecutor` (4-thread pool, cancellation, transitions) |
| Streaming progress | PASS | `SseHub` SSE with monotonic ids, bounded queues |
| Cancellation | PASS | idempotent cancel (`/sessions/{id}/cancel` + loop polling + tool cancellation) |
| Error propagation | PASS | TEIP error envelope in DCode API |
| Recovery | PASS | `SessionRecoveryService` marks interrupted sessions FAILED on restart (tested) |

Same integration architecture in all three apps: one product-local `CodingHarnessAdapter`
SPI per FHC 1.0 §4, one wire client per product, no shared code. PASS.

## B. ENR integration (runtime side)

| Check | Result | Evidence |
|---|---|---|
| Native initialization | PASS | `EnrRuntime` constructor + `EnrServer` (standalone); OSGi `EnrActivator` (embedded bundle) |
| Runtime discovery | PASS | `/enr/v1/health`, `/enr/v1/providers`, `/enr/v1/capabilities`, `/enr/v1/runtime` |
| Runtime registration | PASS | TRYST `enr-default` provider (startup discovery); DStudio `enr` engine default; EVO `LlmRouter` ENR slot |
| Default selection | PASS* | TRYST + DStudio: ENR default. EVO: selectable via router metadata (see limitation L-1). DCode: FIXED in Phase 7 (see C) |
| Model discovery | PASS | `/enr/v1/models` (both implementations); live verified |
| Model loading/unloading | PASS | `/enr/v1/models/{id}/load|unload` + native `ModelManager` (active-session protection, 409) |
| Inference execution | PASS | `/enr/v1/chat` + native kernels; live verified |
| Streaming inference | PASS | `/enr/v1/chat/stream` SSE (`data: {chunk...}` + terminal `event: done`); live verified |
| Context limits | PASS | maxSeqLen enforcement, `MAX_SEQ_LEN_REACHED` termination |
| Resource management | PASS | session admission limit, CUDA VRAM accounting, weight cache |
| GPU/CPU selection | PASS | `Backends` SPI, per-model backend, CPU fallback policy |
| Backend configuration | PASS | `enr.yaml` (preferred_backend, model_directories) |
| Cancellation | PASS | cooperative per-step cancellation + `/api/v1/inference/cancel` + stream cancel |
| Timeout management | FIXED | `InferenceOptions.timeoutSeconds` and `requestTimeoutSeconds` now enforced by the new housekeeper scheduler (were parsed but ignored) |
| Runtime recovery | PASS | health checks 503 on degraded; shutdown cleans sessions/models |

## C. DCode → ENR default inference path (the Phase 7 correction)

**Baseline defect (D-1):** DCode had no ENR provider. Its provider layer registered
only Ollama. The mandatory default path `DCode → ENR` did not exist.

**Correction (implemented and verified):**

1. New `eu.kalafatic.dcode.config.provider.EnrProvider` in the DCode config bundle —
   implements the existing `LlmProvider` SPI over the family ENR 1.0 wire contract
   (`GET /health`, `GET /enr/v1/models`, `POST /enr/v1/chat`). Endpoint default
   `http://127.0.0.1:28780` (family port), overridable via `-Ddcode.enr.endpoint`;
   optional bearer token support; token masking in `getConfiguration()`.
2. `ConfigActivator` now registers ENR as the **default** provider (registered first),
   with Ollama as the alternative. No harness code branches on provider type
   (SPI preserved, no hardcoding of ENR in the orchestration).
3. Tests:
   - `EnrProviderTest` (7 wire tests, mock ENR server) — green.
   - `EnrLiveIntegrationTest` (2 live tests, guarded by reachability) — **executed
     against a live ENR standalone server, 2/2 green**.
   - Live harness run (executed): `connectionTest=true`, model discovery returned the
     2 loaded models, blocking completion returned `finish=stop` with real token
     counts through the production `EnrProvider` class.

**Verification of the required default-path steps (executable evidence):**

| # | Step | Result |
|---|---|---|
| 1 | DCode initializes (provider registry starts) | PASS (test suite) |
| 2 | DCode discovers ENR | PASS (live: `/enr/v1/health` + `/enr/v1/models`) |
| 3 | ENR reports readiness | PASS (live: `{"status":"UP","ready":true}`) |
| 4 | DCode retrieves available models | PASS (live: 2 models, providerId=enr) |
| 5 | DCode selects configured model | PASS (session-level `providerId`/`modelId` resolution; router error on unset, no silent invention) |
| 6 | DCode submits inference requests | PASS (live completion through `EnrProvider.complete`) |
| 7 | ENR executes inference | PASS (real native kernel run, token counts) |
| 8 | DCode receives responses | PASS (live response parsed) |
| 9 | DCode continues the coding workflow | PASS (harness tests with real agent loop; live coding run deferred — see L-2) |
| 10 | Runtime errors and recovery handled | PASS (`ProviderException` mapping; ENR 503/DEGRADED surfaces in `testConnection`) |

The inference integration does not bypass ENR when ENR is the configured provider:
`ProviderRegistry.complete(providerId, ...)` dispatches to the ENR provider; no
fallback silently re-routes.

## D. Alternative providers

- DCode: Ollama + OpenAI-compatible providers remain registered/selectable; per-session
  `providerId` overrides unchanged.
- EVO: Ollama/native EVO/OpenAI/Gemini `ILlmProvider`s unchanged; `LlmRouter` keeps
  routing to them when ENR is not explicitly requested.
- TRYST: Ollama provider and legacy `AiInferenceService` remain; per-task model
  override unchanged.
- DStudio: `OllamaInferenceEngineAdapter` remains registered; engines independently
  enabled/disabled.
- ENR is the default, not the only provider — verified by the registries above.

## E. Fixes delivered in Phase 7 (ENR standalone repo)

1. `/enr/v1` family surface added to the standalone ENR server (`FamilyApiHandlers`,
   routes registered in `EnrServer`): health, models (list/get/load/unload), chat,
   chat/stream (SSE), runtime, providers, capabilities. TEIP error envelope for the
   family surface. DCode/TRYST/DStudio wire clients now interoperate with the
   standalone runtime.
2. Request timeout enforcement (`armTimeout` + housekeeper scheduler in `EnrRuntime`).
3. Periodic idle eviction (sessions) via the same housekeeper.
4. `ModelManager.resolveModelPath(id)` for family model load by id.
5. Family surface integration tests (`FamilyApiIntegrationTest`, 5 tests) — green.

## F. Limitations

- **L-1 (EVO policy)**: EVO's `LlmRouter` keeps its documented no-silent-switching
  policy — ENR engages when explicitly selected (`provider=enr`, `engine=enr`,
  `enr:` prefix). Making ENR the unconditional default for every EVO inference path
  requires Product Owner approval (Phase 6 finalization report, item 2).
- **L-2 (model availability)**: a full coding task through DCode→ENR with a
  coding-capable model was not executed because the standalone ENR only had the
  tiny test fixtures loaded; the wire path was verified with real inference on those
  fixtures. Remediation: load a real `.evo`/GGUF model into ENR and re-run scenario A.
