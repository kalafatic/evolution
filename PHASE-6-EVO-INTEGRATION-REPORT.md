# PHASE-6-EVO-INTEGRATION-REPORT.md

Phase 6 — EVO integration (WP-6.5, WP-6.8)
Date: 2026-11-17

## Status

| Acceptance item | Status |
|---|---|
| DCode + ENR registered in default capability/tool sets | **Implemented** — Darwin `CapabilityRegistry` (per-session, via `KernelFactory`) + TEIP capability descriptors + connected-application registry |
| Darwin can discover and select their capabilities | **Implemented** — `CapabilityDiscovery`/`CognitiveLoopEngine` read the existing registry; DCode selected by default in `ConnectedApplicationRegistry`; ENR selectable through `LlmRouter` provider slot |
| Developer Mode uses DCode | **Implemented** — Developer Mode delegates through the existing `CodingHarnessTool` → `ConnectedApplicationRegistry` seam; DCode is the registered default |
| Darwin inference can use ENR | **Implemented** — `LlmRouter.setEnrProvider`/`sendEnrRequest`/`isEnrAvailable` + `EnrLlmProvider` bridge registered by the ENR bundle |
| Existing orchestration intact | **Preserved** — `OrchestratorServiceImpl → KernelFacade → KernelFactory/SessionContainer/IterationManager` chain untouched; all changes additive |
| Architecture Scanner, Selective Genome, JDT/PDE tooling preserved | **Preserved** — untouched |

## Changes (evolution-121126)

### New bundle `eu.kalafatic.evolution.enr` (ENR 1.0)
See `PHASE-6-ENR-INTEGRATION-REPORT.md`. Added to the root reactor and to
`eu.kalafatic.evolution.feature`.

### Controller changes (additive)

1. `resource/PortRegistry.java` — new `EVO_ENR` entry (service id `ENR`, default 28780,
   original 8780, HTTP, autoStart false).
2. `resource/ResourceManager.java` — ENR service descriptor added to the EVO service
   catalog (discoverable in the existing service list).
3. `manager/OllamaService.java` — new public `streamChat(userInput, sessionId,
   chunkConsumer)` (stream=true over the existing chat request builder). No change to
   existing methods.
4. `orchestration/llm/LlmRouter.java` — ENR provider slot
   (`setEnrProvider`/`getEnrProvider`/`isEnrAvailable`/`sendEnrRequest`);
   `sendLocalRequest` routes to ENR only when explicitly requested (context metadata
   `provider=enr` / `engine=enr` or `enr:` model prefix). Default routing unchanged.
5. `harness/DCodeClient.java` (new) — wire client for the DCode `/api/v1` surface.
6. `harness/DCodeCodingHarnessAdapter.java` (new) — FHC 1.0 adapter, id `dcode`.
7. `harness/ConnectedApplicationRegistry.java` — fallback preference DCode → OpenCode
   when no explicit selection exists.
8. `tools/ToolFactory.java` — registers the DCode adapter and selects DCode on first run
   when no selection is persisted.
9. `orchestration/capability/DCodeFamilyCapability.java` (new) — `family.dcode.harness`
   (contracts: `harness.delegation`, `harness.discovery`).
10. `orchestration/capability/EnrFamilyCapability.java` (new) — `family.enr.inference`
    (contracts: `inference.generation`, `inference.streaming`,
    `inference.model.lifecycle`).
11. `orchestration/KernelFactory.java` — both family capabilities registered per kernel
    session (inside the existing registration block).
12. `teip/service/TeipExecutionManager.java` — capability catalog extended with
    `DCODE_HARNESS` and `ENR_INFERENCE` descriptors (family discovery over TEIP 1.1).

### Docs

- `docs/protocol/ENR-1.0.md` (new normative contract).

## Verification

- Build: `mvn -o -pl eu.kalafatic.evolution.enr,eu.kalafatic.evolution.controller -am
  compile` — BUILD SUCCESS.
- New tests (Tycho surefire, JUnit 4): `EnrRuntimeTest` 9/9, `EnrHttpServerTest` 12/12,
  `PortRegistryEnrTest` 2/2, `FamilyCapabilitiesTest` 2/2, `DCodeCodingHarnessAdapterTest`
  9/9 — **34/34 green**.
- Regression on directly-affected suites: `ConnectedApplicationRegistryTest` 10/10,
  `EvoCoreLlmLifecycleTest` 4/4, `TeipApiEndpointTest` 7/7, `TeipExecutionManagerTest`
  6/6, `TeipDtoSerializationTest` 10/10 — all green.
- Full controller.tests suite: 61 pre-existing failures/errors in classes that reference
  none of the modified code (live-LLM/environment/UI-dependent and previously-red unit
  tests, e.g. `TargetScannerTest`, `DreamExperienceSelectionAndContextTest`,
  `SelfDevHttpTasksTest`, `PlatformModeFunctionalTest`). See `PHASE-6-TEST-REPORT.md`.
- Runtime verification: ENR HTTP server exercised by wire tests in-process; full RCP
  runtime launch not executed in this headless environment.

## Known items

1. ENR becomes the *selectable* ENR provider in `LlmRouter`; the existing direct
   Ollama/native paths remain the unchanged default (no silent switching). Product-owner
   decision needed to make ENR the unconditional default for all EVO inference paths.
2. EVO product runtimes must include the feature with the ENR bundle for in-process ENR;
   otherwise EVO can consume a remote ENR endpoint (documented in ENR 1.0).
