# PHASE-6-FAMILY-CAPABILITY-REPORT.md

Phase 6 — Family tool and capability discovery (WP-6.8)
Date: 2026-11-17

## Principle

Family capability discovery is layered on the **existing** discovery mechanisms of each
product. No parallel tool registry, no global family registry, no new protocol was
introduced:

- EVO: Darwin `CapabilityRegistry` (per-session) + TEIP 1.1 capabilities endpoint +
  `ConnectedApplicationRegistry` (harness selection) + `LlmRouter` (inference selection).
- TRYST: `ConnectedApplicationRegistry` (harness selection, persisted, HTTP-managed via
  `/api/integration/applications`) + `AiModelRegistryService`/`ModelSelectionService`
  (inference providers + models).
- DStudio: `HarnessRegistry` (IDE + TEIP) + new `InferenceEngineRegistry` (IDE +
  `/api/inference` headless).
- DCode: its own `/api/v1/tools`, `/api/v1/models`, `/health` surface (FHC 1.0 mapping).
- ENR: `/enr/v1/capabilities`, `/enr/v1/providers`, `/enr/v1/models` (ENR 1.0).

## Default availability (verified)

| Product | DCode discoverable | DCode default | ENR discoverable | ENR default |
|---|---|---|---|---|
| EVO | `ConnectedApplicationRegistry` (registered at startup) + `DCodeFamilyCapability` + TEIP `DCODE_HARNESS` | yes (selection persisted; explicit choices respected) | `EnrFamilyCapability` + TEIP `ENR_INFERENCE` + `LlmRouter` slot | selectable via `LlmRouter` (no silent re-routing of established paths) |
| TRYST | `ConnectedApplicationRegistry` (`/api/integration/applications`) | yes | `AiModelRegistryService` (`enr-default`) | yes (registered + discovery at startup; Ollama alternative) |
| DStudio | `HarnessRegistry` (IDE + TEIP capabilities) | yes | `InferenceEngineRegistry` (IDE + `/api/inference`) | yes |

## Selection semantics

- **Harness selection** (EVO/TRYST): persisted per product; DCode auto-selected on first
  run when nothing selected; explicit operator selections always win; disabled or
  unavailable harnesses are skipped (caller falls back per policy).
- **Inference selection**:
  - EVO: existing `LlmRouter` policies unchanged; ENR engaged explicitly (metadata
    `provider=enr` / `engine=enr` / `enr:` model prefix) — no silent switching.
  - TRYST: `ModelSelectionService` picks from discovered ENR models when available;
    Ollama models remain published and selectable; per-task model override unchanged.
  - DStudio: `InferenceEngineRegistry.selectDefaultEngine` (ENR); engines independently
    enabled/disabled; TEIP executions are harness-scoped and unchanged.
- **Capability descriptors** (TEIP 1.1): EVO's capabilities endpoint now includes
  `DCODE_HARNESS` and `ENR_INFERENCE` so TRYST/DStudio/future members can discover the
  family defaults through the normative family protocol.

## Extensibility (verified paths to alternatives)

- New harness: implement the product-local `CodingHarnessAdapter`/`HarnessAdapter` SPI
  and register it — no orchestration change (OpenCode, DStudio TEIP, EVO TEIP all still
  registered and selectable).
- New inference engine: implement `InferenceEngineAdapter` (DStudio), `AIProvider`
  (TRYST), or register an `ILlmProvider` (EVO); ENR itself wraps the existing
  Ollama/llama.cpp/native-EVO stack without replacing it.
- Other family modules can consume DCode via FHC 1.0 (REST mapping) and ENR via ENR 1.0
  (HTTP/SSE + OSGi service) — both documented, both wire-only from the consumer side.

## Verification

- EVO: `FamilyCapabilitiesTest` 2/2 (capability contracts + health), TEIP capabilities
  endpoint covered by `TeipExecutionManagerTest`/`TeipApiEndpointTest` (green).
- TRYST: connected-applications HTTP management suite green (`ConnectedApplicationsHttpSecurityTest`
  and registry tests pass in the full run); ENR provider discovery covered by
  `EnrProviderTest`.
- DStudio: `InferenceEngineRegistryTest` + `InferenceHttpApiTest` green; TEIP
  capabilities still aggregate all registered harnesses (DCode included).
