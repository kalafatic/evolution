# PHASE-6-DSTUDIO-INTEGRATION-REPORT.md

Phase 6 — DStudio integration (WP-6.7)
Date: 2026-11-17

## Status

| Acceptance item | Status |
|---|---|
| DCode available as default harness | **Implemented** — `DCodeHarnessAdapter` registered + selected by default; OpenCode/Aider remain registered |
| DCode manageable via IDE | **Implemented** — registry-driven HarnessView covers DCode (select/enable/test/configure) |
| ENR available as default inference engine | **Implemented** — new `core.inference` subsystem; ENR adapter default, Ollama alternative |
| Inference management via IDE | **Implemented** — new `InferenceView` (engines, models, set-default/enable/test/refresh/load/unload) |
| Inference management via headless server | **Implemented** — `/api/inference` on the DStudio server (engines, models, load/unload, runtime, chat) |
| Harness and inference engine independently configurable/replaceable | **Implemented** — separate registries (`HarnessRegistry`, `InferenceEngineRegistry`), separate stores, no coupling |
| Sessions and execution history persist | **Preserved** — existing execution/event/review persistence; DCode executions flow through the same `ExecutionService` |
| Headless operation (EVO/TRYST consumers) | **Preserved** — TEIP 1.1 unchanged and verified; new inference surface additive |

## Changes (DStudio-121126)

### Core (`eu.kalafatic.dstudio.core`)
- New `core.inference` package (exported):
  - `InferenceEngineAdapter` (SPI + model/chat/stream/runtime/connection records),
  - `InferenceEngineRegistry` + `DefaultInferenceEngineRegistry`
    (register/unregister, enable, configure, select default, test connection,
    persisted via store),
  - `InferenceEngineDescriptor` (status/enabled/timestamps).
- New `core.store.InferenceEngineStore` persistence contract.

### Infrastructure
- `SqliteInferenceEngineStore` (SQLite) — descriptors, selected-engine state,
  per-engine configuration JSON.
- `SchemaMigrationService` V2 (forward-only): `inference_engines`,
  `inference_engine_state`, `inference_engine_config`.

### Harness adapters (`eu.kalafatic.dstudio.harness`)
- `dcode/DCodeHarnessAdapter` — FHC 1.0 wire adapter over DCode `/api/v1`
  (workspace/repository/session/start/poll/cancel/changes); sink events; returns
  modified files + diff text for the existing ReviewView flow; token masked;
  WAITING_FOR_APPROVAL → completed with "pending review".
- `enr/EnrInferenceEngineAdapter` — ENR 1.0 wire adapter (health, model discovery,
  load/unload, chat, SSE streaming, runtime status).
- `ollama/OllamaInferenceEngineAdapter` — alternative engine (tags, chat, streaming,
  keep_alive unload).

### Application + headless
- `DStudioLifecycleCoordinator`: registers DCode harness (default when unset),
  builds the inference-engine registry (ENR default, Ollama alternative), passes it
  to the server.
- `DStudioServer`: mounts `InferenceHttpHandler` under `/api/inference`; TEIP handler
  parameter generalized to `HttpHandler` (callers unchanged).
- `InferenceHttpHandler` (teip bundle): `GET /engines`, `GET /models?engine=`,
  `POST /engines/{id}/load|unload`, `GET /engines/{id}/runtime`, `POST /chat`.

### UI (`eu.kalafatic.dstudio.ui`)
- `InferenceView` (view id `eu.kalafatic.dstudio.ui.view.inference`, registered in
  plugin.xml, added to the Harness perspective) + `DStudioUiPlugin`
  `inferenceEngineRegistry()` accessor.

## Verification

- Full reactor `mvn -o clean verify`: **BUILD SUCCESS — 102 tests, 0 failures,
  0 errors, 0 skipped** (73 pre-existing + 29 new).
- New tests: `DCodeHarnessAdapterTest` 9/9, `InferenceEngineAdapterWireTest` 8/8
  (ENR + Ollama stubs), `InferenceEngineRegistryTest` 5/5 (persistence, selection,
  enablement), `InferenceHttpApiTest` 7/7 (headless API over the real DStudioServer).
- Existing suites unaffected: `TeipApiEndpointTest`, `TeipOpenCodeEndToEndTest`,
  lifecycle coordinator tests, harness tests all green (constructor updates applied
  where the server signature gained the inference-registry parameter).
- Live ENR inference: not executed (no live model server in this environment);
  wire contracts covered by stub servers on both sides.

## Known items

1. DCode harness adapter uses DCode's own provider configuration for LLM selection
   (providerId/model config keys); DStudio's ENR registry manages inference
   independently — the two remain separable by design.
2. `InferenceView` model operations run via `Display.asyncExec` (SWT thread rule);
   headless consumers use `/api/inference` directly.
