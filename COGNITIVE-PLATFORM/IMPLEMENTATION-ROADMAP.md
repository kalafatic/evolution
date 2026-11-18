# IMPLEMENTATION ROADMAP

Phased plan. Dependencies, affected bundles/repos, deliverables, tests, acceptance criteria, risks, complexity. Complexity: S/M/L (small/medium/large change surface). All phases are additive; no phase changes existing orchestration semantics.

## WP1 — Cognitive domain model + persistence foundation (EVO)
- **Scope**: new SQLite tables (applications, cognitive_sessions, session_relationships, session_snapshots, conversations, conversation_messages+FTS, memory_index+FTS, user_profiles, user_preferences, project_*, context_snapshots); `MemoryEntry` additive fields (version, lastVerifiedAt, validity, relationships, provenanceChain) with backward-compatible JSON; MigrationService-free additive `CREATE TABLE IF NOT EXISTS` in `DatabaseManager`.
- **Dependencies**: none.
- **Affected**: `eu.kalafatic.evolution.servers` (DatabaseManager), `eu.kalafatic.evolution.controller` (memory package).
- **Deliverables**: tables + `MemoryService` extension + version JSONL + FTS index.
- **Tests**: memory round-trip with new fields; old-file load defaults; FTS search; DB fault injection.
- **Acceptance**: existing memory consumers/tests green; new entries queryable by FTS.
- **Risks**: none structural. **Complexity**: M.

## WP2 — Native platform cognitive services (EVO)
- **Scope**: `SessionIdService`, `SessionContinuityService`, `ConversationHistoryService`, `UserContextService`, `PersonalizationService`, `ProjectKnowledgeService`, `TokenEstimationService`, `CognitiveContextService` facade, `ContextQualityService` (controller/cognitive package); IdentityService extension in servers bundle (application tokens).
- **Dependencies**: WP1.
- **Affected**: controller, servers.
- **Deliverables**: OSGi+singleton services with interfaces; events (MEMORY_*, SESSION_*, CONTEXT_RECONSTRUCTED) added to `RuntimeEventType`.
- **Tests**: per-service unit suites; isolation/precedence/budget tests (matrix rows 4–11).
- **Acceptance**: service layer usable headless; audit rows for mutations.
- **Risks**: scope creep (mitigated: facade only calls existing producers). **Complexity**: L.

## WP3 — Context reconstruction engine (EVO)
- **Scope**: `ContextReconstructionService` pipeline (16 stages), budget optimization, ContextPackage extension + schema 1.0, `ContextQualityService` assessment.
- **Dependencies**: WP2.
- **Affected**: controller (orchestration/ContextBuilder additive hook, mediation ContextCurator token-estimator call-site swap).
- **Deliverables**: ContextPackage 1.0 JSON schema; pipeline tests.
- **Tests**: budget trimming, conflict surfacing, staleness, provenance classes.
- **Acceptance**: `context_get` under budget; Darwin prompt unchanged when store empty.
- **Risks**: token estimator accuracy (BPE vocab availability) — fallback char/4 documented. **Complexity**: L.

## WP4 — Existing MCP server integration (EVO)
- **Scope**: `CognitiveMcpToolProvider`; bearer authenticator hook in `JsonRpcDispatcher` (`setAuthenticator`); prompt authz gate; resource URI scope checks; register cognitive tools/resources in `McpDemoServerManager` (28280) with AuthorizationService+authenticator; `serverInfo` 1.1.0 + capabilities metadata.
- **Dependencies**: WP2–WP3.
- **Affected**: controller (orchestration/mcp), servers (mcp/server, demo).
- **Deliverables**: Phase-1 tool set (context_get/search, memory_store/search, user_get, user_preferences_get/update, session_create/get/snapshot, decision_record/search, project_context_get, project_knowledge_search) + resources (evo://users/sessions/projects/memory).
- **Tests**: MCP contract fixtures; authN/authZ matrix (row 12–14); loopback live call.
- **Acceptance**: external client (curl/JSON-RPC) gets context with token; spoofed params rejected.
- **Risks**: 28280 currently demo path — wire carefully without breaking demo UI on 28080. **Complexity**: M.

## WP5 — Session continuity + conversation persistence hardening (EVO)
- **Scope**: session registry lifecycle hooks in `OrchestratorServiceImpl`/`SessionManager`; restart reconciliation; FULL snapshot manifests; `ConversationOutputController` append hook; compaction summaries; `session_restore`.
- **Dependencies**: WP1–WP2.
- **Affected**: controller (orchestration).
- **Deliverables**: durable sessions + conversations for all modes; restore flow.
- **Tests**: restart simulation; history recovery; snapshot round-trip (rows 2,3).
- **Acceptance**: conversation survives restart for headless modes; kernel checkpoint behavior unchanged.
- **Risks**: interaction with existing RESTART CONTINUITY block — additive only. **Complexity**: M.

## WP6 — Consolidation, knowledge extraction, personalization learning (EVO)
- **Scope**: `MemoryConsolidationService`, `KnowledgeExtractionService`, preference inference thresholds, feedback pipeline on FLOW_COMPLETED/TASK_FAILED events; project knowledge import from genome (`importGenome`) + `Evaluator` build/test recording.
- **Dependencies**: WP2, WP5.
- **Affected**: controller (cognitive), selfdev.genome (no change — import only).
- **Deliverables**: async consolidation jobs; ADR/known-issue stores fed by mediation model persistence hook.
- **Tests**: consolidation from fixture transcript; explicit-beats-inferred; correction downgrade; build/test archival.
- **Acceptance**: after a simulated task, an EPISODE + build/test rows exist; nothing blocks inference.
- **Risks**: LLM-dependent extraction — deterministic fallback required. **Complexity**: L.

## WP7 — ENR metadata extension + DCode context ingest
- **Scope**: ENR `promptTokens`/`completionTokens`, `contextLength` (+known flag); DCode `POST /sessions/{id}/context`, `metadata_json` round-trip fix, budget-aware window in `ToolCallingAgentLoop`.
- **Dependencies**: none (parallel with WP4–6).
- **Affected**: EVO `eu.kalafatic.evolution.enr`; DCode `persistence`/`server`/`harness` bundles.
- **Deliverables**: wire changes + tests in both repos (DCode suite stays green).
- **Tests**: rows 17–18; token parity with Ollama counters.
- **Acceptance**: non-zero contextWindowTokens via ENR; ingest survives restart.
- **Risks**: ENR statelessness regression — guarded by no-new-persistence grep test. **Complexity**: S/M.

## WP8 — TRYST and DStudio consumption
- **Scope**: TRYST `EvoMcpClient` + `AiContextManager` merge + correlation column; DStudio `EvoMcpClient` + runTask context section + `external_session_id` (V3).
- **Dependencies**: WP4 (stable MCP contract).
- **Affected**: TRYST `evo.integration`/`domain`/`infrastructure`; DStudio `teip`/`core`/`infrastructure`.
- **Deliverables**: end-to-end flows (session_create/context_get/context_record/memory_store).
- **Tests**: rows 19–20; degradation tests with EVO down; no-EVO-classes import check.
- **Acceptance**: cross-repo E2E green; local behavior unchanged without EVO.
- **Risks**: DStudio per-user identity deferred (documented). **Complexity**: M.

## WP9 — Security hardening and audits (EVO)
- **Scope**: token issuance/revocation admin API; mandatory first-run credential change prompt; scope enforcement at service layer; sensitive-entry handling; prompt-injection rendering rules; audit completeness.
- **Dependencies**: WP2, WP4.
- **Affected**: servers (IdentityService, controllers), controller (cognitive authorizer).
- **Deliverables**: security test suite (row 14), threat-model checklist verified.
- **Acceptance**: all spoofing/unauthorized tests green; every mutation audited.
- **Risks**: breaking existing 28080 demo auth assumptions — keep bypass for non-cognitive paths. **Complexity**: M.

## WP10 — Performance and validation (all repos)
- **Scope**: latency measurements, retention sweeps, FTS performance fixtures, budget benchmarks; full family build + test runs; documentation finalization.
- **Dependencies**: WP1–9.
- **Affected**: all.
- **Deliverables**: measured performance report; final acceptance report.
- **Acceptance**: targets met or explicitly renegotiated with data.

## Dependency graph

```text
WP1 ─► WP2 ─┬─► WP3 ─► WP4 ─► WP8
             ├─► WP5 ─► WP6
             └─► WP9
        WP7 (parallel)          WP10 (final)
```

## Suggested sequencing rationale

WP1→WP2 first because every other package depends on the store and services; WP3→WP4 next because MCP is the external contract that TRYST/DStudio consume (WP8); WP5/WP6 harden continuity and learning; WP7 is independent and unblocks budget-awareness in DCode; WP9 is continuous hardening; WP10 finalizes. MVP = WP1+WP2+WP3+WP4 (+WP7 metadata subset): a working `context_get`/`memory_store` over authenticated MCP.
