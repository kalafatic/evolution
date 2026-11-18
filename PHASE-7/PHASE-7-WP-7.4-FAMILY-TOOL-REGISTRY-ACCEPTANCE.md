# PHASE-7-WP-7.4-FAMILY-TOOL-REGISTRY-ACCEPTANCE.md

Phase 7 — WP-7.4 Family Tool Registry and Default Module Acceptance
Date: 2026-11-17
Status: COMPLETE

## 1. Default availability matrix (verified by inspection + executed tests)

| Requirement | EVO | TRYST | DStudio | DCode |
|---|---|---|---|---|
| DCode available by default | `ToolFactory` registers `DCodeCodingHarnessAdapter` at startup | `ApplicationLifecycleCoordinator` registers DCode adapter at startup | `DStudioLifecycleCoordinator` registers `DCodeHarnessAdapter` at startup | native harness |
| ENR available by default | `EnrFamilyCapability` + `LlmRouter` ENR slot (`EnrLlmProvider`) | `enr-default` provider registered + startup model discovery | `enr` engine registered, `selectDefaultEngine(enr)` | **Phase 7:** `EnrProvider` registered as default in `ConfigActivator` |
| Discoverable | TEIP 1.1 `DCODE_HARNESS` + `ENR_INFERENCE` capabilities; `CapabilityRegistry` per session | `/api/integration/applications`; `/api/models` | HarnessView/InferenceView + TEIP capabilities + `/api/inference` | `/api/v1/providers`, `/api/v1/models`, `/api/v1/tools` |
| Capabilities reported | `DCodeFamilyCapability`, `EnrFamilyCapability` (contracts + health) | adapter `getCapabilities()` | `aggregateCapabilities()` in TEIP | FHC 1.0 tool list + provider metadata |
| Lifecycle state reported | capability health (`family.dcode.harness`, `family.enr.inference`) | descriptor `ApplicationStatus` (UNKNOWN/CONNECTED/...) | descriptor status + `testConnection` | `/api/v1/status`, `/health` |
| Selectable | `ConnectedApplicationRegistry.selectApplication` | `/api/integration/applications/select` | `HarnessRegistry.selectDefaultHarness` | per-session providerId/modelId |
| Configurable | endpoint config in registry file | `/api/integration/applications/{id}/config` (token never persisted) | harness/inference config tables | `updateConfiguration` (token masked) |
| Replaceable | OpenCode + DStudio adapters registered | `opencode` + `evo` + DStudio adapters registered | OpenCode + Aider harnesses; Ollama engine | Ollama + OpenAI-compatible providers registered |
| Other tools remain | yes (MCP, Java tools, shell, git...) | yes (MCP tools, reviewers...) | yes | yes |

## 2. Scenario tests (evidence per scenario)

| # | Scenario | Result | Evidence |
|---|---|---|---|
| 1 | Fresh installation | PASS | auto-selection only fires when nothing is selected: EVO `ToolFactory` 48–60, TRYST coordinator 494–501, DStudio coordinator 142–145, DCode `ConfigActivator` (ENR first) |
| 2 | Existing installation upgrade | PASS | registries load persisted selection/descriptors before defaulting; new tables added by forward-only migrations (TRYST V20, DStudio V2) |
| 3 | Default configuration | PASS | DCode default harness everywhere; ENR default engine in TRYST/DStudio/DCode |
| 4 | Modified configuration | PASS | persisted user selections win over defaults (all three registries; DStudio `selectionPersistsAcrossRegistryInstances` test green) |
| 5 | Missing DCode | PASS | adapters report `testConnection` failure; TRYST `findSelectedAdapter` requires enabled+available; EVO registry skips unavailable adapters; DStudio `disabledAdapterIsNotSelected` green; diagnostic messages surface in `ConnectionTestResult`/descriptor status |
| 6 | Missing ENR | PASS | TRYST ENR discovery is best-effort (startup proceeds, health OFFLINE); DStudio engine availability flag + `/api/inference/engines` status; DCode `EnrProvider.testConnection()` returns a clear error message |
| 7 | Alternative coding harness | PASS | OpenCode/DStudio/Evo/Aider adapters all still registered and selectable (suite-tested) |
| 8 | Alternative inference provider | PASS | Ollama registered everywhere; OpenAI-compatible in DCode; EVO providers unchanged |
| 9 | Multiple registered tools | PASS | 24 DCode tools; EVO default tools + Java tools + MCP; TRYST MCP tools — one registry each, no conflicts |
| 10 | Repeated application restarts | PASS | registries upsert by id (DStudio `registerAdapter` keeps existing descriptor; TRYST `computeIfAbsent`); selection stored as a single id-keyed row; `DefaultProviderRegistry`/`DefaultToolRegistry` reject duplicates within a run; no duplicate registration across restarts |

## 3. Non-interference guarantees

- Default registration never overwrites user configuration: default selection is
  guarded by `if nothing selected` in all three products (code evidence above).
- Unavailable components produce diagnostics: connection-test results and descriptor
  status fields, plus ENR 503 `DEGRADED` health.
- No parallel tool registry was introduced anywhere (WP-7.1 §6 audit).

## 4. Verification executed

- DCode: full suite 124/124 + `EnrProviderTest` + live `EnrLiveIntegrationTest` 2/2.
- TRYST: full suite 354/354 (incl. `ConnectedApplicationsHttpSecurityTest`,
  `DCodeCodingHarnessAdapterTest` 10/10, `EnrProviderTest` 7/7).
- DStudio: full suite 102/102 (incl. `DefaultHarnessRegistryTest` 7/7,
  `InferenceEngineRegistryTest` 5/5, `InferenceHttpApiTest` 7/7).
- EVO: `FamilyCapabilitiesTest`, TEIP capabilities endpoint tests (see WP-7.10).
