# PHASE-6-ARCHITECTURAL-RECONCILIATION.md

Phase 6 — WP6.1 Architectural reconciliation
Date: 2026-11-17
Repositories: EVO (`evolution-121126`), TRYST (`tryst-121126`), DStudio (`DStudio-121126`), DCode (`DCoder-121126`)

## 1. Confirmed baseline (what actually existed before Phase 6)

### DCode (Phase 5 — implemented)
- 9 OSGi bundles (core, config, repositories, persistence, harness, java.tooling, server,
  client, ui) + feature + product (RCP + headless) + 29 test classes.
- Native agent loop (`ToolCallingAgentLoop`), 24 tools (11 generic + 13 JDT/PDE),
  SQLite persistence with recovery, REST `/api/v1` + SSE on 48080 (fallback range),
  provider SPI (Ollama + OpenAI-compatible) with credential vault.
- **No TEIP server surface, no FHC classes** — planned (WP5.12) but never delivered.
- Verified this phase: full reactor build, 117 tests, 0 failures (1 live-model skip).

### ENR — did not exist
- Zero occurrences of "ENR"/"Evolution Neural Runtime" in any repository.
- The functional building blocks existed inside EVO: `EvoInferenceEngine` /
  `ReferenceEvoInferenceEngine` (native JVM inference, token streaming),
  `OllamaService`/`OllamaManager` (model discovery, pull/delete/unload, chat),
  `LlamaService` (llama.cpp), `NativeEvoProvider` (native artifact resolution),
  `ILlmProvider`/`LlmRouter` (provider routing), `ProjectModelManager` (model
  discovery), `eu.kalafatic.evolution.gpu` (NVIDIA detection).
- Decision (recorded): ENR is implemented as a new EVO bundle
  `eu.kalafatic.evolution.enr` that **unifies** these existing services behind a stable
  published contract. No new inference path, no second model registry, no new engine —
  ENR is the published facade over the existing stack plus a standalone HTTP/SSE surface.

### EVO
- TEIP 1.1 server implemented (`/teip/v1/*`, 28080) with capabilities endpoint.
- `CodingHarnessAdapter` SPI + `ConnectedApplicationRegistry` (product-local) with
  OpenCode + DStudio adapters registered in `ToolFactory`.
- Darwin `CapabilityRegistry` (per-session) with `KernelScheduler` + `ActivationResolver`
  registered in `KernelFactory`.
- `PortRegistry` 28080–28680 authoritative; ResourceManager service catalog.
- DStudio TEIP harness delegation from EVO verified in Phase 4 (live E2E).

### TRYST
- Full async task pipeline (TaskOrchestrator → TaskQueue → TaskWorker → review/approval/
  commit) with sandbox isolation and `CodingHarnessAdapter` SPI + `ConnectedApplicationRegistry`
  (SQLite, migration V17), adapters: opencode (CLI), evo (TEIP), dstudio (TEIP 1.1,
  migration V18). Default selection was EVO when EVO enabled.
- Inference: `AIProvider` SPI with Ollama impl (`OllamaProvider`), model registry +
  selection service. Schema migrations up to V19.

### DStudio (Phase 4 — implemented, contrary to the Phase 5 DCode baseline assumption)
- 6 plugins + feature + product; TEIP 1.1 provider server (48080) with auth, idempotency,
  event cursors; `HarnessAdapter` SPI + `HarnessRegistry` (SQLite V1) with OpenCode +
  Aider adapters; execution service with persistence and restart reconciliation; IDE
  (perspectives/views/dialogs). 73/73 tests green before this phase.
- No inference-engine subsystem, no DCode adapter, models were adapter-config keys.

## 2. Contract reconciliation (WP6.2)

| Contract | State before | Phase 6 action |
|---|---|---|
| TEIP 1.1 | Normative, implemented (EVO server; TRYST/DStudio clients; DStudio server) | Unchanged (verified). Capability descriptors extended with DCODE_HARNESS and ENR_INFERENCE family entries. |
| FHC 1.0 | No written spec; de-facto = Phase 3 `CodingHarnessAdapter` SPI | **Canonicalized** in `DCoder-121126/PHASE-6-FHC-1.0.md` (DCode = spec caretaker). Defines delegation semantics + the DCode `/api/v1` wire mapping. |
| ENR 1.0 | Did not exist | **Created**: `docs/protocol/ENR-1.0.md` (EVO) — inference contract (OSGi interface + `/enr/v1` HTTP/SSE). |

The harness/inference distinction is now normative in both documents: harnesses delegate
coding execution; inference engines provide model discovery/lifecycle/generation; no
hardcoded DCode→ENR dependency exists anywhere (DCode keeps its own provider SPI).

## 3. Family port reconciliation

- EVO 28xxx (28080–28680) — unchanged; **+28780 ENR HTTP** added to `PortRegistry`.
- TRYST 38xxx (38080–38580) — unchanged.
- DStudio 48xxx TEIP (48080, diagnostics reserved 48180) — unchanged.
- DCode 48xxx (48080 HTTP, fallback range 500) — unchanged.
- **Documented risk:** DCode HTTP and DStudio TEIP share convention port 48080 (both
  loopback, both with fallback allocation). Product-owner confirmation of the DCode 48xxx
  proposal remains open (Phase 5 open item). No code change made.
- ENR default endpoint for remote consumers: `http://localhost:28780`.

## 4. Ownership matrix (verified, unchanged)

- EVO: Darwin sessions + orchestration (KernelFacade/SessionContainer/IterationManager).
- TRYST: tasks, sandboxes, repositories, review/approval/commit.
- DStudio: IDE sessions/workspaces, executions, review, persistence.
- DCode: harness execution state (sessions, tools, changes).
- ENR: inference runtime state (providers, model lifecycle).
- No global family session manager introduced anywhere.

## 5. Phase 6 change surface (what this phase implemented)

- **EVO:** new `eu.kalafatic.evolution.enr` bundle (ENR 1.0); `PortRegistry.EVO_ENR`;
  `LlmRouter` ENR provider slot; `OllamaService.streamChat`; `DCodeClient` +
  `DCodeCodingHarnessAdapter`; DCode default selection in `ConnectedApplicationRegistry` +
  `ToolFactory`; `DCodeFamilyCapability` + `EnrFamilyCapability` in `KernelFactory`;
  TEIP capability descriptors; ResourceManager ENR service entry; ENR-1.0 doc; tests.
- **TRYST:** `DCodeHttpClient` + `DCodeCodingHarnessAdapter` (infrastructure/dcode);
  `DCodeConfiguration` + repository + migration V20; `EnrProvider` (AIProvider impl);
  lifecycle wiring (DCode default selection, ENR provider registration); tests.
- **DStudio:** `DCodeHarnessAdapter` (harness/dcode); `core.inference` SPI
  (`InferenceEngineAdapter`, registry, descriptor) + `InferenceEngineStore` +
  SQLite impl + migration V2; `EnrInferenceEngineAdapter` + `OllamaInferenceEngineAdapter`;
  `InferenceHttpHandler` mounted under `/api/inference` (headless); `InferenceView` (IDE);
  DCode default harness selection; tests.
- **DCode:** FHC 1.0 canonical document (spec caretaker); no code changes required.
