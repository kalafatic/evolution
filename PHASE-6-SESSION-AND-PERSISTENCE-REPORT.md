# PHASE-6-SESSION-AND-PERSISTENCE-REPORT.md

Phase 6 — Session ownership, persistence, lifecycle and recovery (WP-6.9)
Date: 2026-11-17

## Ownership boundaries (verified unchanged)

| Owner | State | Persistence |
|---|---|---|
| EVO | Darwin sessions + orchestration | EMF configuration + existing trajectory/memory stores; connected-application selection persisted as JSON in the EVO data directory |
| TRYST | Tasks, task sessions, sandboxes, repositories, reviews | SQLite (forward-only migrations, now V1–V20) |
| DStudio | IDE sessions/workspaces, executions, reviews | SQLite (`~/.dstudio/dstudio.db`, V1–V2) + `dstudio.properties` |
| DCode | Harness sessions, tools, changes | SQLite (`~/.dcode/dcode.db`, WAL), event log, recovery on restart |
| ENR | Inference runtime + model lifecycle | Stateless per request; provider/model state owned by underlying engines (Ollama registry, native artifacts) |

No global family session manager was introduced. Every delegated operation carries the
originating application identity (TEIP `caller` for TEIP delegations; FHC workspace/
session creation per originating app for DCode delegations).

## What Phase 6 added (persistence-wise)

- **EVO**: DCode adapter connection configuration persists through the existing
  `ConnectedApplicationRegistry` JSON persistence (selected application id + descriptor,
  secrets masked). ENR is in-process; its HTTP settings are system properties
  (`evo.enr.port`, `evo.enr.auth.token`).
- **TRYST**: migration V20 `dcode_configurations` (endpoint, providerId, model, enabled,
  timeout) via `SqliteDCodeConfigurationRepository`; the DCode bearer token is runtime
  only and never persisted (same rule as the DStudio TEIP token, V18). Connected-app
  selection persists through V17 as before.
- **DStudio**: migration V2 `inference_engines` + `inference_engine_state` +
  `inference_engine_config` via `SqliteInferenceEngineStore`; engine enablement,
  default selection and per-engine configuration survive restarts. Harness state
  (V1 tables) now also stores the DCode descriptor/config.

## Lifecycle and recovery (verified)

- **DCode**: in-flight sessions reconciled to FAILED on restart (Phase 5, unchanged).
- **DStudio**: `DefaultExecutionService.reconcileAfterRestart()` marks open executions
  FAILED; DCode executions run through the same path (no new code path).
- **TRYST**: task checkpoints + recovery unchanged; delegation failures fall through to
  the local CodeAgent per existing `HarnessAiDelegator` policy; a DCode failure returns
  `handled` error and the task transitions through the existing error handling.
- **ENR**: no durable state of its own; unreachable providers degrade health to
  DEGRADED and remain non-fatal (verified in `EnrRuntimeTest`/`EnrHttpServerTest`).

## Cross-restart persistence tests executed

- TRYST: `DCodeCodingHarnessAdapterTest.configurationPersistsAcrossAdapterInstances`
  (V20 round-trip) — green; full `SchemaMigrationTest`/`DatabaseInitializationTest`/
  `DatabaseAvailabilityAndPersistenceVerificationTest` suites green with the V20 head.
- DStudio: `InferenceEngineRegistryTest.configurationPersists` +
  `selectionPersistsAcrossRegistryInstances` (V2 round-trip) — green;
  `SqliteStoresTest` (existing persistence) — green.
- EVO: registry persistence logic unchanged (existing `ConnectedApplicationRegistryTest`
  green).
