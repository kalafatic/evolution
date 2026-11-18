# FINAL ARCHITECTURE REPORT

Cognitive Services for the EVO Platform — final conclusions and required decisions. Every claim below is backed by the verified audit in EXISTING-ARCHITECTURE-AUDIT.md.

## 1. Required final answers

**1. Can Cognitive Services be implemented directly in the existing EVO Platform?**
YES. All required building blocks exist: SQLite auth/permission/audit infrastructure (`servers` bundle), in-process session architecture (`SessionManager`/`SessionContext`/`IterationManager`), persistent memory (`MemoryService`), context pipelines (`ContextBuilder`/`ContextCurator`/`AttachmentInjector`), MCP implementation with real tool wiring (`EvoMcpToolProvider`, port 28280), repository intelligence (`TargetScanner`, `RepoArchitectureScanner`, genome `MilestoneGenerator`). Nothing requires a new platform or a parallel architecture.

**2. Which existing components should be reused?**
- `servers`: AuthService, AuthorizationService, UserService, Role/Permission/ResourceAccessPolicy, AuditLog, DatabaseManager (SQLite).
- `controller`: SessionManager/SessionContainer/SessionContext, IterationManager + checkpoint machinery, MemoryService/MemoryEntry/MemoryQuery/MemoryContextProvider, ContextBuilder/ContextPackage, ContextCurator/PromptSynthesizer (mediation), AttachmentInjector, RuntimeEventBus/RuntimeEventType, ContextAssistant/intent stack, Evaluator, EvoMcpToolProvider/ToolRegistry/ResourceRegistry/JsonRpcDispatcher, PortRegistry/PortAllocationManager/ResourceManager, SemanticWorkspace decay semantics, ConversationState, ProjectModelManager path discovery.
- `selfdev.genome`: MilestoneGenerator + genome outputs (import source).
- `enr`: EnrRuntime/EnrHttpServer (extended with metadata fields only).
- `forge.tokenizer`: tokenizers for the estimation service.

**3. Which components need extension?**
- MemoryService (versioning, validity, relationships, events, FTS mirror) — backward compatible.
- ContextPackage (assessment, sources, budget fields, schema version).
- RuntimeEventType (+MEMORY_*/SESSION_*/CONTEXT_* types).
- JsonRpcDispatcher (authenticator hook, prompt authz gate).
- DatabaseManager (additive cognitive tables).
- User (profile columns); ConversationOutputController (append hook); ContextBuilder (optional cognitive call); Evaluator (build/test recording); ContextCurator (token estimator call-site); ENR records + HTTP serialization (usage/contextLength); DCode (`/sessions/{id}/context`, metadata_json round-trip); TRYST `AiContextManager` (external context source); DStudio `DefaultExecutionService.runTask` (context section).

**4. Which genuinely new components are necessary?**
Small, cohesive additions only: `SessionIdService`, `SessionContinuityService`, `ConversationHistoryService`, `UserContextService`, `PersonalizationService`, `ProjectKnowledgeService`, `TokenEstimationService`, `ContextReconstructionService`, `ContextQualityService`, `MemoryConsolidationService`, `KnowledgeExtractionService`, `CognitiveContextService` (facade), servers-bundle `IdentityService` (application tokens), MCP `CognitiveMcpToolProvider`, TRYST/DStudio `EvoMcpClient`s. Total ≈ 1 new package in controller + 1 in servers + 2 small client modules.

**5. How should the existing MCP server expose these capabilities?**
Extend the existing server on port 28280 (`PortRegistry.EVO_MCP`): register `CognitiveMcpToolProvider` tools (context_get/search/record, user_get, user_preferences_get/update, session_create/get/search/snapshot/restore, memory_store/search/update/retract, decision_record/search/get, project_context_get, project_state_get/update, project_knowledge_search) and `evo://...` resources; install bearer authentication + scope authorization; keep JSON-RPC 2.0 / protocol `2024-11-05` / HTTP POST transport unchanged (SSE is an optional later additive transport).

**6. What is the canonical identity and session integration strategy?**
Identity: existing SQLite `users` remain canonical for humans; new `applications` + hashed `application_tokens` bind (app, user, scopes) for external apps; bearer tokens on MCP; params never trusted for identity. Sessions: `cognitive_sessions` registry with UUID ids (legacy aliases accepted), layered snapshot manifests (conversation/context/kernel) referencing the existing kernel checkpoint format; `SessionManager` remains the runtime registry; `IterationManager` remains the only state-transition authority.

**7. How should persistent memory be stored?**
Extended `user_memory.json` (authoritative entries, backward compatible) + SQLite `memory_index` FTS mirror for search + append-only `versions/<entryId>.jsonl` for audit; episodic/semantic/procedural are `MemoryType` values, not separate stores; consolidation feeds MemoryService from conversations/outcomes. No new database (SQLite via existing DatabaseManager; ADR-003).

**8. How should DCode consume reconstructed context?**
Additive `POST /api/v1/sessions/{id}/context`; `ToolCallingAgentLoop` injects the platform context as a SYSTEM block and shrinks its 40-message history window according to `contextWindowTokens − tokenEstimate`; fix `metadata_json` round-trip so correlation survives restart. DCode keeps its provider SPI and FHC 1.0 role — no EVO classes imported.

**9. How should Darwin Engine participate?**
Darwin remains the orchestrator and cognitive execution mechanism. Additive hooks: `KernelFacade.handle`/`ContextBuilder` request a ContextPackage before engine orchestration (identical behavior when the store is empty); Darwin delegates coding execution to DCode (existing adapters) and inference to ENR (existing LlmRouter slot). Darwin does not implement memory itself — it consumes and records through the same native services.

**10. How should ENR integrate without owning memory?**
ENR stays stateless (spec §8). It gains metadata only: per-response `promptTokens`/`completionTokens` (from Ollama counters already available in `OllamaService`), per-model `contextLength` + `contextLengthKnown`, and (Phase 2) a stateless `/enr/v1/embeddings` endpoint behind a capability flag. Zero new persistence in ENR (guarded by test).

**11. How should TRYST and DStudio consume the services?**
Wire-only MCP clients (`EvoMcpClient` in TRYST `evo.integration`, DStudio `teip`), EVO-issued bearer tokens, correlation columns in their local DBs (TRYST session row `evo_cognitive_session_id`; DStudio `executions.external_session_id` V3), context merged into TRYST `AiContextManager` and DStudio instruction assembly, recordings after task completion, graceful degradation when EVO is down.

**12. What is the smallest viable first implementation?**
WP1+WP2+WP3+WP4 (+ENR metadata subset of WP7): SQLite foundation, memory extension, reconstruction engine, authenticated MCP with the Phase-1 tool set. Deliverable: an external client can `context_get` a budgeted ContextPackage and `memory_store` an entry over 28280 with a valid token, fully audited. Everything else layers on without architectural change.

**13. What are the major architectural risks?**
Consolidation quality (LLM-derived memory — mitigated by confidence/provenance/retract), token estimation accuracy (mitigated by headroom + measured reporting), SQLite single-writer scaling (documented, measured before any migration), expectation mismatch on session restore depth (documented three-layer scope), contract drift across repos (schema versions + golden fixtures), DStudio's missing user identity (app-token Phase 1), pre-existing weak credentials (first-run change + external tokens only).

**14. What should be implemented first, and why?**
WP1 (domain model + persistence) then WP2 (native services): every other work package depends on the store and service layer; both are fully additive and cannot regress existing behavior. WP3+WP4 follow immediately because the authenticated MCP surface is the contract that unblocks TRYST/DStudio consumption (WP8) and validates the design end-to-end with minimal total code.

## 2. Final recommendation

**ACCEPT WITH LIMITATIONS.**

The architecture is implementable inside the existing EVO Platform with high reuse, additive changes only, and no new infrastructure. Limitations (all documented, none blocking):
- Semantic/vector search deferred to Phase 2 (ENR embeddings); Phase 1 retrieval is FTS5 + relationship + temporal.
- Session restore covers conversation/context/kernel layers only — no UI/selection state.
- DStudio per-user identity deferred (application token in Phase 1).
- Performance targets are goals pending measurement; SQLite scaling is the first thing to validate under real load.
- The existing MCP implementation's dead-code paths and demo unauthenticated behavior are preserved for the demo surface but closed for cognitive tools.

Rejecting factors that would change the recommendation: none found. The verified audit shows no fundamental conflict with the existing Darwin orchestration, MCP implementation, or persistence architecture.

## 3. Responsibility boundaries (final statement)

- **EVO Platform** owns identity, sessions, memory, project knowledge, context reconstruction, personalization — the cognitive infrastructure.
- **MCP** (28280, extended) is the external integration protocol; internal EVO communication stays OSGi/Java.
- **DCode** is the coding harness: consumes context, executes tools, persists its own run state.
- **ENR** is the inference runtime: stateless, metadata-rich, memory-free.
- **Darwin Engine** is the orchestration and cognitive execution mechanism: consumes and records, never a second memory platform.
- **TRYST / DStudio / future apps** are consumers: connect to EVO MCP, request context, record outcomes, restore continuity.
