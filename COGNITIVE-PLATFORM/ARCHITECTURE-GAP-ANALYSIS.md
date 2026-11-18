# ARCHITECTURE GAP ANALYSIS

Mapping the target Cognitive Services scope against verified existing implementation. Every gap lists the smallest compatible extension (no parallel architecture).

---

## GAP 1 — Canonical identity is EVO-local and not shareable

**Have**: SQLite `users`/`roles`/`permissions`/`resource_access`/`audit_log` + BCrypt + 60-min bearer sessions (`servers` bundle). TEIP static token. Supervisor has no auth.
**Need**: authenticated, auditable identity for external MCP clients; user preference persistence; per-application access.
**Gap**: no external identity API; no token validation on MCP; unverified `x-evo-user` header; DStudio/TRYST have parallel local users with no federation.
**Smallest extension**:
1. Reuse `AuthService`/`UserService`/`AuthorizationService` unchanged as the identity authority.
2. Add app credentials/token issuance over the existing `/api/auth` + admin API surface (`AuthController`/`AdminApiController`): an `ApplicationIdentity` concept issuing bearer tokens bound to (userId, appId) with scope constraints — persisted in new SQLite tables (`applications`, `application_tokens`) managed by the existing `DatabaseManager` migration pattern.
3. Make MCP validate a bearer token against `AuthService.validateSession` or an app token, and derive the execution principal from it (replacing the unverified header). No second user manager.

## GAP 2 — No unified session identity or persistence

**Have**: `SessionManager` (in-memory per-process), kernel checkpoints per sessionId, EMF SelfDevSession resume, SQLite `sessions` table (auth sessions only).
**Need**: sessions that survive restart, are listable, carry ownership (user/app/project), and can be related across applications.
**Gap**: no session ID service; session identity never persisted for orchestration sessions; no session-to-project registry; DStudio executions/TRYST tasks/DCode sessions are unlinked.
**Smallest extension**:
1. Extend `SessionContainer`/`SessionContext` minimally: give sessions stable ids via a new `SessionIdService` (thin utility, UUID-v4 + creation metadata, stored in SQLite `cognitive_sessions` table), and persist a light `SessionDescriptor` (id, type CONVERSATION/TASK/APPLICATION, owner, project ref, timestamps, status, lastSnapshotRef) through the existing `DatabaseManager`.
2. Keep `SessionManager` as the runtime registry; add a load/reconcile step on startup (reuse the `IterationManager` restart-continuity pattern). Do not create a second session manager.

## GAP 3 — Conversation history is not durably stored for all modes

**Have**: EMF ChatSession persisted only on XMI save; `ConversationOutputController` in-memory; headless sessions lost.
**Need**: append-only conversation store, structured tool-call records, search, compaction.
**Gap**: no conversation store; no structured tool-call record.
**Smallest extension**: new `ConversationHistoryService` in the controller bundle writing JSONL per conversation (`<workspace>/shared/conversations/<conversationId>.jsonl`) — same pattern as `IterationMemoryService.audit_trail.jsonl` — plus a SQLite index table (`conversation_messages` with message_id, conversation_id, sequence, role, agent_type, tool_name, tokens, timestamp) for search. EMF ChatSession remains the interactive model; the store is the durable mirror written by `ConversationOutputController` (one new call site) and the new service.

## GAP 4 — Memory is fragmented, event-less, un-consolidated

**Have**: MemoryService (JSON), IterationMemoryService (JSON dir), SemanticWorkspace (in-memory), EMF sharedMemory.
**Need**: unified memory authority with episodic/semantic/procedural types, relationships, versioning, consolidation, decay, events, search.
**Gap**: three disconnected stores; no CRUD events; no consolidation; no relationships; naive query.
**Smallest extension**:
1. Extend `MemoryService` in place (same file/format contract, additive fields + optional relationships array + version + lastVerifiedAt + provenance) — keeps backward compatibility with `user_memory.json` readers (ContextBuilder, ContextualHelpService, DreamEngine, UI).
2. Publish `RuntimeEvent`s on CRUD (reuse `RuntimeEventType`, add MEMORY_* types).
3. Add `MemoryConsolidationService` that turns conversation/tool/outcome data into entries (async, post-session); add `KnowledgeExtractionService` reusing `forge.controller` extraction concepts but scoped to conversations.
4. Federation: `MemoryService` remains the authority; `IterationMemoryService`/`SemanticWorkspace` stay session-scoped and contribute consolidated entries (one direction: into MemoryService). No second memory platform.

## GAP 5 — No semantic search / embeddings / token estimation at runtime

**Have**: substring queries; bytes/4 heuristic; forge.tokenizer BPE on classpath but unwired.
**Need**: semantic retrieval (optional), accurate token budgeting for context reconstruction.
**Gap**: no embedding generation/vector index; no token counting service.
**Smallest extension**:
1. `TokenEstimationService` in the controller wrapping `forge.tokenizer` (char fallback when no trained vocab) — replaces `ContextCurator.estimateTokens` call site; single budget unit = tokens.
2. Embeddings: defer to a later phase; when introduced, reuse ENR (add `/enr/v1/embeddings` behind existing `EnrRuntime`/Ollama embed capability) — no new inference path. Phase 1 retrieval = exact/full-text (SQLite FTS5) + relationship traversal + temporal ranking.

## GAP 6 — No context quality assessment or unified budget management

**Have**: ContextBuilder/ContextCurator/AttachmentInjector each with own budgets (chars/bytes/32k tokens).
**Need**: one ContextPackage contract with schema version, sources, confidence, freshness, token estimate, warnings.
**Gap**: no assessment stage; inconsistent units; no cross-stage budget.
**Smallest extension**: new `ContextReconstructionService` orchestrating existing producers (MemoryContextProvider, ContextResolver, ContextCurator, AttachmentInjector) into a single `ContextPackage` (extended with provenance/budget fields), and `ContextQualityService` producing `ContextAssessment`. Budget enforcement centralized in one place (TokenEstimationService + per-category quotas).

## GAP 7 — MCP does not expose cognitive services and lacks authentication/streaming

**Have**: JSON-RPC dispatcher + registries + real tools (EvoMcpToolProvider) + AuthorizationService hooks + PortRegistry 28280.
**Need**: cognitive tools/resources (context_get, memory_store, session_*, user_*, project_*, decision_*), bearer auth on 28280, possibly SSE.
**Gap**: no cognitive tools; no caller authentication; prompts unprotected; no streaming transport.
**Smallest extension**:
1. New `CognitiveMcpToolProvider` in `controller/orchestration/mcp/` mirroring `EvoMcpToolProvider.registerAuthoritativeTools` — thin adapters over the native services (single implementation, both interfaces).
2. Register it in `McpDemoServerManager`/`DemoDocumentationProvider` path (28280) with `dispatcher.setAuthorizationService(...)` + bearer validation against AuthService; keep 28080 UI wiring as is.
3. Resources via `ResourceRegistry` with `evo://...` URIs; prompts left as-is (document that prompts are not authorization-critical, or add the same authz check — smallest change is adding it to `JsonRpcDispatcher`).
4. SSE: additive transport on the existing server if protocol clients require it; not required for Phase 1 (MCP 2024-11-05 over HTTP POST is sufficient).

## GAP 8 — No persistent project knowledge store

**Have**: genome markdown/JSON generation, mediation models (ephemeral), RepoArchitectureScanner, EMF EvaluationResult, Evaluator parsing.
**Need**: queryable ProjectKnowledge (components, ADRs, known issues, build/test history, snapshots).
**Gap**: no store; scanner outputs ephemeral; build/test outcomes unarchived.
**Smallest extension**: `ProjectKnowledgeService` in the controller persisting into SQLite (`project_components`, `architectural_decisions`, `known_issues`, `build_results`, `test_results`, `repository_snapshots` — same DatabaseManager migration pattern). Producers: `MilestoneGenerator` output (components), `Evaluator` (build/test rows — one new call site), mediation `TargetRealityModel` (decisions/issues when persisted via `MediatedExportManager` or a new save call). Reuses `RepoArchitectureScanner`/`TargetScanner` as-is.

## GAP 9 — ENR lacks token/context metadata; DCode lacks context ingest

**Have**: ENR stateless facade; DCode local context engine + SQLite.
**Need**: ENR token usage + context window metadata (budget planning); DCode ability to consume externally reconstructed context.
**Gap**: `EnrChatResponse` has no usage; `EnrModel` has no contextLength; DCode has no ingest endpoint.
**Smallest extension**: additive wire fields on ENR records + HTTP serialization (no new ownership); DCode additive `POST /api/v1/sessions/{id}/context` + `metadata_json` round-trip fix in `SqliteSessionService.mapSession`.

## GAP 10 — TRYST/DStudio have no consumption path

**Have**: TRYST TEIP client + local identity + AiContextManager; DStudio TEIP + harness/inference registries.
**Need**: consume EVO MCP cognitive services without coupling to EVO internals.
**Gap**: no MCP client in either app; DStudio has no user identity at all.
**Smallest extension**: small MCP client modules (JSON-RPC over HTTP, mirroring `TeipClient` patterns) in TRYST `evo.integration` and DStudio `dstudio.teip`; identity consumption via EVO-issued bearer; merge external context into TRYST `AiContextManager` and a new DStudio context-assembly point. No changes to EVO internals for them.

---

## Consolidated gap matrix

| # | Gap | Severity | Existing anchor | Smallest extension |
|---|---|---|---|---|
| 1 | Shareable identity + app tokens | HIGH | servers auth stack | applications/tokens tables + MCP bearer validation |
| 2 | Session identity/persistence | HIGH | SessionManager + checkpoints | SessionIdService + cognitive_sessions table |
| 3 | Durable conversation history | HIGH | ConversationOutputController | ConversationHistoryService (JSONL + SQLite index) |
| 4 | Unified memory authority | HIGH | MemoryService | extend MemoryService + consolidation service + events |
| 5 | Semantic search / token estimation | MEDIUM (search) / HIGH (tokens) | forge.tokenizer | TokenEstimationService; FTS5; embeddings deferred to ENR |
| 6 | Context quality/budget | HIGH | ContextBuilder/Curator | ContextReconstructionService + ContextQualityService |
| 7 | MCP cognitive surface + auth | HIGH | MCP registries + EvoMcpToolProvider | CognitiveMcpToolProvider + bearer validation |
| 8 | Project knowledge store | MEDIUM | genome + scanners + Evaluator | ProjectKnowledgeService (SQLite) |
| 9 | ENR metadata / DCode ingest | MEDIUM | ENR records; DCode API | additive wire fields; ingest route |
| 10 | TRYST/DStudio consumption | MEDIUM | TeipClient patterns | MCP clients + context merge points |
