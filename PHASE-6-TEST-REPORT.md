# PHASE-6-TEST-REPORT.md

Phase 6 — Test and verification report (WP-6.10/WP-6.11)
Date: 2026-11-17

All builds use Maven 3.9.12 (wrapper) + Java 21 + Tycho, offline mode.

## 1. DCode (DCoder-121126)

Command: `mvn -o clean verify`
Result: **BUILD SUCCESS — 117 tests, 0 failures, 0 errors, 1 skipped**
(skip = opt-in live-model completion; local Ollama hosts only EVO experiment models).
No code changes in this repository (FHC 1.0 document only); build re-verifies the
Phase 5 baseline.

## 2. TRYST (tryst-121126)

Command: `mvn -o clean verify`
Result: **BUILD SUCCESS — 354 tests, 0 failures, 0 errors, 0 skipped**

New tests:
- `DCodeCodingHarnessAdapterTest` — 10/10 (stub `/api/v1` server: identity,
  connection test ok/fail, completed execution with files+logs, WAITING_FOR_APPROVAL
  mapping, failure propagation, cancellation sends cancel, token masking, V20 config
  round-trip, disabled config).
- `EnrProviderTest` — 7/7 (stub ENR server: connection ok/fail, model discovery,
  blocking chat, SSE stream chunk assembly, failure reporting).

Updated tests (V20 migration head): `SchemaMigrationTest`, `DatabaseInitializationTest`,
`DatabaseAvailabilityAndPersistenceVerificationTest`, `ServerBootstrapAndNetworkingTest`
— all green in the full run.

## 3. DStudio (DStudio-121126)

Command: `mvn -o clean verify`
Result: **BUILD SUCCESS — 102 tests, 0 failures, 0 errors, 0 skipped**
(73 pre-existing + 29 new)

New tests:
- `DCodeHarnessAdapterTest` — 9/9 (identity, connection, completion with diff+events,
  WAITING_FOR_APPROVAL, failure, cancellation, missing workspace, token masking).
- `InferenceEngineAdapterWireTest` — 8/8 (ENR stub: identity/connection/models/chat/
  lifecycle/runtime; Ollama stub chat; unreachable reporting for both).
- `InferenceEngineRegistryTest` — 5/5 (registration/selection, enablement+status,
  config persistence, selection persistence across instances, unknown-engine
  rejection).
- `InferenceHttpApiTest` — 7/7 (headless `/api/inference`: engines, models,
  load/unload, runtime, chat, 404s).
Updated: `TeipApiEndpointTest`/`TeipOpenCodeEndToEndTest` (server constructor gained
the inference-registry parameter; passing null where the TEIP flow is under test).

## 4. EVO (evolution-121126)

Commands:
- `mvn -o -pl eu.kalafatic.evolution.enr,eu.kalafatic.evolution.controller -am compile`
  → BUILD SUCCESS
- Targeted new tests (`-Dtest=EnrRuntimeTest,EnrHttpServerTest,DCodeCodingHarnessAdapterTest,
  FamilyCapabilitiesTest,PortRegistryEnrTest`): **34/34 green**
  - `EnrRuntimeTest` 9/9 (offline-safe contract/health/discovery/lifecycle/runtime)
  - `EnrHttpServerTest` 12/12 (wire: health, models, load/unload, chat, SSE, runtime,
    providers, capabilities, auth token, error envelopes)
  - `DCodeCodingHarnessAdapterTest` 9/9 (stub server: identity, connection, completion,
    WAITING_FOR_APPROVAL, failure, cancellation, missing target, token masking)
  - `FamilyCapabilitiesTest` 2/2, `PortRegistryEnrTest` 2/2
- Regression on directly-affected suites (full run): `ConnectedApplicationRegistryTest`
  10/10, `EvoCoreLlmLifecycleTest` 4/4, `TeipApiEndpointTest` 7/7,
  `TeipExecutionManagerTest` 6/6, `TeipDtoSerializationTest` 10/10 — green.
- Full `controller.tests` suite: 333 tests in 72 classes — **27 failures + 18 errors
  in 20 pre-existing classes**. Audit: every failing class references none of the
  modified code (verified by inspection + failure-mode analysis). Failure families:
  - live-LLM/environment-dependent (OllamaFallbackTest, PlatformModeFunctionalTest,
    OrchestratorFlowTest, SelfDevHttpTasksTest, SelfEvoForgingIntegrationTest,
    MediatedModeIntegrationTest),
  - pre-existing unit-level red tests (TargetScannerTest, DesignRendererTest,
    RepoArchitectureScannerTest, ChatShortCircuitFixTest,
    DreamExperienceSelectionAndContextTest, EvoGlobalContextMenuTest,
    EvoNativeProtocolTestSuiteTest, FunctionalityInventoryTest,
    ImplementationPlannerTest, MavenBuildExecutorTest, MutationIntegrationTest,
    RemoteDatasetDiscoveryTest, StructuredConversationAndOasstTest,
    DevelopIntegrationTest).
  These failures are outside Phase 6 scope and were not introduced by it; they are
  recorded here for product-owner backlog.

## 5. What was NOT verified (disclosed)

- Live DCode agent execution end-to-end (no live LLM model available in this
  environment; wire contracts verified by stub tests on every consumer + DCode's own
  suite).
- Live ENR inference against a real model (same reason); GPU status verified as
  graceful degradation.
- EVO RCP runtime launch / Developer Mode UI flow (headless environment; the Developer
  Mode delegation seam is covered by adapter + registry tests).
- TRYST live task pipeline with DCode delegation (requires a running DCode headless
  with a working model).
