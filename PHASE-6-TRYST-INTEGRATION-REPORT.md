# PHASE-6-TRYST-INTEGRATION-REPORT.md

Phase 6 — TRYST integration (WP-6.6)
Date: 2026-11-17

## Status

| Acceptance item | Status |
|---|---|
| DCode executes coding tasks | **Implemented** — `DCodeCodingHarnessAdapter` drives DCode sessions through `/api/v1` |
| ENR provides inference | **Implemented** — `EnrProvider` (`AIProvider` SPI) registered at startup |
| Sandbox isolation intact | **Preserved** — workspace containment stays with TRYST; DCode executes inside the TRYST workspace and the adapter enforces the workspace path |
| Review and approval remain under TRYST control | **Preserved** — DCode never commits; `WAITING_FOR_APPROVAL` maps to a completed result whose changes flow into the existing review/approval/commit pipeline |
| Task lifecycle unchanged | **Preserved** — TaskOrchestrator → TaskQueue → TaskWorker → review/approval/commit untouched; delegation happens at the existing IMPLEMENTING seam (`HarnessAiDelegator`) |
| Harness + inference independence | **Implemented** — any `CodingHarnessAdapter` × any `AIProvider` combination remains selectable; DCode/ENR defaults are product configuration, not code coupling |

## Changes (tryst-121126)

1. `plugins/eu.kalafatic.tryst.domain`:
   - `model/DCodeConfiguration.java` (new) — persisted DCode connection config
     (endpoint, providerId, model, enabled, timeoutSeconds; token runtime-only).
   - `repository/DCodeConfigurationRepository.java` (new).
2. `plugins/eu.kalafatic.tryst.infrastructure`:
   - `dcode/DCodeHttpClient.java` (new) — wire client (workspaces, repositories,
     sessions, start/cancel, state, messages, changes; regex JSON per TRYST convention).
   - `dcode/DCodeCodingHarnessAdapter.java` (new) — implements `CodingHarnessAdapter`;
     id `dcode`, type `DCODE`, capabilities `CODE_IMPLEMENTATION` + `CODE_ANALYSIS`;
     config keys endpoint/authToken/providerId/model/timeoutSeconds/enabled; persisted
     via the repository; polls with cancellation/timeout; collects changed files;
     WAITING_FOR_APPROVAL → completed with "pending review".
   - `ai/EnrProvider.java` (new) — `AIProvider` implementation over the ENR 1.0 wire
     contract (health, model discovery, chat, SSE streaming); endpoint
     `tryst.enr.endpoint`/`TRYST_ENR_ENDPOINT`, default `http://localhost:28780`.
   - `persistence/SchemaMigrationService.java` — migration **V20**
     (`dcode_configurations`).
   - `persistence/repository/SqliteDCodeConfigurationRepository.java` (new).
   - MANIFEST: export `eu.kalafatic.tryst.infrastructure.dcode`.
3. `plugins/eu.kalafatic.tryst.application`:
   - `lifecycle/ApplicationLifecycleCoordinator.java`:
     - registers the DCode adapter (with SQLite config repo) into the existing
       `ConnectedApplicationRegistry`;
     - when nothing is explicitly selected, **DCode becomes the selected harness**
       (explicit operator selections, including a prior EVO selection, are respected);
     - registers the ENR provider in `AiModelRegistryService` (`enr-default`) and runs
       model discovery; the Ollama provider (`ollama-default`) remains registered as the
       alternative; a missing ENR runtime degrades gracefully (warn + continue).
4. Tests (updated): migration version assertions 19 → 20 in `SchemaMigrationTest`,
   `DatabaseInitializationTest`, `DatabaseAvailabilityAndPersistenceVerificationTest`,
   `ServerBootstrapAndNetworkingTest` (V20 is the new forward-only head).

## Verification

- Full reactor build: BUILD SUCCESS (compile).
- `DCodeCodingHarnessAdapterTest` 10/10 green (stub `/api/v1` server: identity,
  connection test, completed execution with files+logs, WAITING_FOR_APPROVAL mapping,
  failure propagation, cancellation with cancel request, token masking, config
  persistence, disabled config).
- `EnrProviderTest` 7/7 green (stub ENR server: connection, model discovery, blocking
  chat, SSE streaming chunk assembly, failure reporting).
- Full test suite run: 354 tests executed; all pre-existing suite-order flakiness and the
  migration-assertion updates addressed; one remaining flaky port-bind test
  (`CommunicationServiceTest.testStartupFailureReportingWhenHttpPortOccupiedAndFallbackDisabled`,
  BindException under parallel suite load) is environment/order dependent and unrelated
  to Phase 6 changes. See `PHASE-6-TEST-REPORT.md`.

## Known items

1. Live DCode delegation (real agent run) requires a running DCode headless with a
   configured LLM provider — not executed in this environment; wire behavior covered by
   stub tests on both sides.
2. ENR model selection: TRYST's `ModelSelectionService` picks from discovered ENR models
   when available; per-task model policy (override per task) is configurable through the
   existing model registry, no schema change required.
