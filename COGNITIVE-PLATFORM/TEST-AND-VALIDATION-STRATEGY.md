# TEST AND VALIDATION STRATEGY

Verification plan across unit / integration / end-to-end levels. Tests assert behavior (round-trips, isolation, precedence, budgets), not class existence. Existing suites that must stay green are listed per area.

## 1. Layers

- **Unit** (JUnit, no OSGi runtime): services with temp-dir SQLite/JSON fixtures — mirrors existing `UserMemorySubsystemTest`, `CheckpointDeterminismTest`, `ConnectedApplicationRegistryTest` patterns.
- **Integration** (OSGi-free service wiring + SQLite file DB): dispatcher + authenticator + one real service; MCP JSON-RPC in-process via the existing `DemoDocumentController`/`McpServer` classes (no socket needed — or loopback socket like existing server tests).
- **End-to-end**: live EVO MCP on 28280 against a real workspace; cross-repo with TRYST/DStudio test suites (both have established E2E harness patterns; EVO↔DStudio TEIP live E2E exists as precedent).

## 2. Coverage matrix (maps to the 20 required validation areas)

| # | Validation area | Test | Level |
|---|---|---|---|
| 1 | User identity consistency across apps | same userId from TRYST and DStudio tokens maps to one profile/preferences; `user_get` from both returns identical profile | E2E |
| 2 | Session continuity after restart | create session → snapshot → restart persistence layer → `session_get` returns PAUSED + history intact; `session_restore` returns restored manifest | Integration |
| 3 | Conversation history recovery | write 100 messages → reload store → `list` + FTS `search` match; JSONL replay equals SQL index | Unit |
| 4 | Cross-session memory retrieval | memory stored in session A (scope=PROJECT) appears in `context_get` of session B same project; scope=SESSION entry does not | Integration |
| 5 | Project memory isolation | project X entry invisible to context for project Y (scope enforcement at service layer, both projects owned by same user) | Unit |
| 6 | User preference persistence | explicit preference survives store reload; round-trip via `user_preferences_get/update` | Unit + MCP |
| 7 | Explicit vs inferred precedence | inferred write rejected with explicit present; `context_get` renders explicit in instruction section, inferred in observation section | Unit |
| 8 | Decision history + supersession | record A then B superseding A → `decision_get` returns chain; `project_context_get` includes only ACCEPTED | Unit |
| 9 | Contradiction detection | two memories with CONTRADICTS relationship → `context_get` warning lists both, higher confidence first | Unit |
| 10 | Stale memory detection | entry older than TTL with lastVerifiedAt old → flagged stale, downranked, still present | Unit |
| 11 | Context budget optimization | request budget 2000 tokens → `tokenEstimate ≤ 2000` + trimming order verified (low relevance first) | Unit (fixture with known scores) |
| 12 | MCP tool integration | `tools/list` contains cognitive tools; `tools/call context_get` returns valid ContextPackage JSON conforming to schema | Integration |
| 13 | MCP resource integration | `resources/read evo://sessions/{id}/history` returns transcript; unauthorized URI returns -32002 | Integration |
| 14 | AuthN/AuthZ | no token → -32001; app token without `memory.write` → -32002 on `memory_store`; spoofed `userId` param rejected | Integration |
| 15 | Concurrent sessions | 20 parallel sessions storing/querying memory → no lost updates (SQLite WAL + synchronized writes); audit log complete | Integration |
| 16 | Persistence recovery | DB closed mid-write (fault injection) → next open consistent (WAL recovery); JSONL lines individually parseable | Unit |
| 17 | DCode integration | ingest endpoint round-trip + loop budget-aware windowing; metadata_json survives restart | DCode suite |
| 18 | ENR integration | chat response carries usage tokens matching Ollama; `/enr/v1/models` carries contextLength; no new ENR persistence | ENR suite (EVO) |
| 19 | TRYST integration | full task run with live EVO MCP produces correlated session + episode; graceful degradation with EVO down | TRYST suite + E2E |
| 20 | DStudio integration | execution consumes context section; correlation persisted; restart reconciliation unchanged | DStudio suite |

## 3. Regression anchors (existing suites that must remain green)

- EVO: `UserMemorySubsystemTest`, `MemoryMenuIntegrationTest`, `ContextualHelpServiceTest`, `DreamExperienceSelectionAndContextTest`, `CheckpointDeterminismTest`, `DarwinExecutionLifecycleTest`, `ConnectedApplicationRegistryTest`, `RepoArchitectureScannerTest`, `SemanticExtractorTest`, `TargetScannerTest`, `FamilyCapabilitiesTest`, `TeipExecutionManagerTest`, `TeipApiEndpointTest`, `ForgeSessionManagerIncrementalTest`, `IntentModeComprehensiveIntegrationTest`.
- TRYST: schema migration suite (`SchemaMigrationTest`, `DatabaseInitializationTest`, `DatabaseAvailabilityAndPersistenceVerificationTest`), `ConnectedApplicationsHttpSecurityTest`, `DCodeCodingHarnessAdapterTest.configurationPersistsAcrossAdapterInstances`, `EnrProviderTest`.
- DStudio: `InferenceEngineRegistryTest`, `InferenceHttpApiTest`, `SqliteStoresTest`.
- DCode: full reactor test suite (117 tests baseline per Phase 5).

## 4. Test infrastructure

- New test bundle: extend `eu.kalafatic.evolution.controller.tests` (no new test bundle). SQLite fixtures: temp dir + `DatabaseManager` pointed at it (existing tests already do this for `ConnectedApplicationRegistryTest`-style JSON; DB tests follow TRYST/DStudio `SchemaMigrationService` test patterns).
- MCP contract tests: reuse `JsonRpcDispatcher` + Jackson serialization asserts; golden JSON fixtures for ContextPackage schema version 1.0.
- Latency assertions: soft targets only (see CONTEXT-RECONSTRUCTION-DESIGN.md §8), recorded as measurements in test reports — never asserted as hard limits until measured in CI.

## 5. Definition of done per work package

1. Unit + integration tests for the package green.
2. Regression anchors green.
3. MCP contract tests green (schema fixtures).
4. Audit trail assertions present for every mutating tool.
5. Cross-repo test for consumer packages (TRYST/DStudio/DCode) green in their repositories.
