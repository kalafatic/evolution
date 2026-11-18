# PHASE-6-ENR-INTEGRATION-REPORT.md

Phase 6 — ENR (Evolution Neural Runtime) as the default family inference engine
Date: 2026-11-17

## Status

| Acceptance item | Status |
|---|---|
| ENR works as an independent inference engine | **Implemented** — new bundle `eu.kalafatic.evolution.enr` (ENR 1.0) |
| Model discovery and inference work | **Implemented** — Ollama models, native `.evo` artifacts, llama.cpp `.gguf` artifacts; blocking chat + SSE streaming |
| EVO uses ENR as default inference engine | **Partially implemented / verified** — ENR is registered as an OSGi service and wired into the Darwin `LlmRouter` as the ENR provider slot (selectable via provider metadata `enr`/`enr:` models). The direct Ollama/native Darwin paths remain the unchanged default until operators select ENR per the no-silent-switching rule. |
| TRYST uses ENR as default inference engine | **Implemented** — `EnrProvider` (`AIProvider` SPI) registered at startup alongside Ollama; model discovery runs against ENR; Ollama stays available as alternative |
| DStudio uses ENR as default inference engine | **Implemented** — ENR is the default engine in the new inference-engine registry; Ollama stays registered as alternative; headless `/api/inference` + IDE `InferenceView` |
| Alternative providers remain supported | **Implemented** — Ollama (all apps), OpenAI-compatible remote providers (DCode), llama.cpp + native EVO (ENR internal engines) |
| Other family modules can consume ENR | **Implemented** — ENR 1.0 wire contract (`/enr/v1`, port 28780) + OSGi service; TRYST and DStudio consume it over HTTP |
| ENR independently deployable | **Implemented** — `EnrMain` standalone entry point; the ENR bundle + dependencies form a deployable runtime without the EVO RCP UI |

## ENR platform (evolution-121126, bundle `eu.kalafatic.evolution.enr`)

Contract (ENR 1.0, `docs/protocol/ENR-1.0.md`):

- `EnrInferenceEngine` — stable in-process contract: health, model discovery/lifecycle
  (load/unload/delete/pull), blocking chat, streaming chat, providers, runtime status,
  capabilities. Registered as an OSGi service by `EnrActivator`.
- `EnrRuntime` — default implementation unifying the existing EVO inference stack:
  - Ollama: `OllamaService`/`OllamaManager` (discovery, pull/delete/unload, chat,
    `streamChat` — new additive streaming method on `OllamaService`);
  - native EVO: `NativeEvoProvider` / `ReferenceEvoInferenceEngine` (token-level
    streaming via `TokenStreamListener`);
  - llama.cpp: `LlamaService` (blocking generation; streaming degrades to chunked
    delivery);
  - GPU: reuses `eu.kalafatic.evolution.gpu` reflectively when present; CPU/memory from
    JDK MBeans.
  - Engine routing: `ollama:` / `evo-native:` / `llama-cpp:` prefixes plus the existing
    `LlamaService.detectInferenceEngine` rules.
- `EnrHttpServer` — NanoHTTPD surface: `GET /health`, `GET|POST|DELETE /enr/v1/models*`,
  `POST /enr/v1/chat`, `POST /enr/v1/chat/stream` (SSE), `GET /enr/v1/runtime`,
  `GET /enr/v1/providers`, `GET /enr/v1/capabilities`; family TEIP error envelope;
  optional bearer token (`evo.enr.auth.token`), dev-mode loopback default; port from
  `evo.enr.port` or `PortRegistry.EVO_ENR` (28780) with +100 fallback allocation.
- `EnrLlmProvider` — `ILlmProvider` bridge registered into the existing `LlmRouter`
  (ENR provider slot; no change to default routing).
- `EnrMain` — standalone entry point.

## EVO wiring

- `PortRegistry.EVO_ENR` (28780, original 8780) added to the authoritative registry;
  `PortAllocationManager` serves it automatically; ResourceManager service catalog now
  lists the ENR service.
- `EnrActivator` (Bundle-Activator): OSGi service registration, LlmRouter bridge, HTTP
  server start (disabled via `evo.enr.server.enabled=false` / `evo.enr.router.enabled=false`).
- Darwin capability `EnrFamilyCapability` (`family.enr.inference`) registered in
  `KernelFactory` — discoverable through the existing `CapabilityRegistry`.
- TEIP 1.1 capabilities endpoint now advertises `ENR_INFERENCE`.

## TRYST wiring

- `infrastructure/ai/EnrProvider.java` — `AIProvider` SPI implementation against the ENR
  wire contract (health, `/enr/v1/models` discovery, `/enr/v1/chat`, SSE
  `/enr/v1/chat/stream`); endpoint `tryst.enr.endpoint` / `TRYST_ENR_ENDPOINT`, default
  `http://localhost:28780`.
- Registered in `ApplicationLifecycleCoordinator` alongside the Ollama provider; model
  discovery is best-effort (missing ENR never disables TRYST; Ollama remains the
  registered alternative). Model policies stay product-local per TRYST (independent from
  EVO configuration).

## DStudio wiring

- `core.inference` SPI: `InferenceEngineAdapter` (+ model/chat/stream/runtime records),
  `InferenceEngineRegistry`, `InferenceEngineDescriptor`, `InferenceEngineStore`
  (+ SQLite impl, migration V2).
- `harness/enr/EnrInferenceEngineAdapter` (default engine) and
  `harness/ollama/OllamaInferenceEngineAdapter` (alternative).
- Headless: `InferenceHttpHandler` under `/api/inference` on the DStudio server —
  engines, model discovery, load/unload, runtime status, chat — available without the
  IDE (WP-6.7 §7.4).
- IDE: `InferenceView` (engines table + model table + set-default/enable/test/refresh/
  load/unload) added to the Harness perspective.

## Verification

- EVO: `EnrRuntimeTest` 9/9, `EnrHttpServerTest` 12/12, `PortRegistryEnrTest` 2/2
  (JUnit 4, Tycho surefire) — all green.
- TRYST: `EnrProviderTest` 7/7 green (stub ENR wire server incl. SSE streaming).
- DStudio: `InferenceEngineAdapterWireTest` (ENR + Ollama) green,
  `InferenceEngineRegistryTest` green, `InferenceHttpApiTest` green (headless API).
- Live inference against a real model: **not executed** (no live model server with a
  functioning chat model in this environment; Ollama live tests remain opt-in per family
  convention). GPU status verified as graceful "not detected" in the test environment.

## Known items

1. EVO's existing Darwin inference paths remain the unchanged default; ENR is the
   *selectable default engine* through the LlmRouter provider slot. Making ENR the
   unconditional default for every EVO inference path would silently re-route established
   flows (prohibited by the no-silent-switching rule) — flagged for product-owner policy
   decision.
2. llama.cpp has no token-level streaming surface; ENR delivers its response chunked.
3. ENR model deletion is Ollama-only (provider-managed artifacts are not deletable) —
   reported as CONFLICT.
