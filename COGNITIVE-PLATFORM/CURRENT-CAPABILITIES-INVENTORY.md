# CURRENT CAPABILITIES INVENTORY

Capability-by-capability inventory with exact source references. Status: EXISTS (verified implementation), PARTIAL (exists but limited), MISSING (verified absent).

---

## 1. Identity

| Capability | Status | Evidence |
|---|---|---|
| User registry | EXISTS | `eu.kalafatic.evolution.servers/.../model/User.java`, `repository/UserRepository.java`, SQLite `users` table (`database/DatabaseManager.java`) |
| Password auth (BCrypt) | EXISTS | `security/BCryptUtils.java`, `service/AuthService.java` |
| Login sessions (60 min idle TTL) | EXISTS | `AuthService.validateSession`, `model/Session.java`, `repository/SessionRepository.java` |
| Roles / permissions / resource access policies | EXISTS | `model/Role|Permission|ResourceItem|ResourceAccessPolicy`, `repository/*Repository`, `service/AuthorizationService` (DENY overrides ALLOW, ADMIN bypass, audit) |
| Audit log | EXISTS | `model/AuditLogEntry`, `repository/AuditLogRepository`, `audit_log` table |
| User profile (beyond displayName/description) | MISSING | `User.java` has no profile/preference fields |
| User preferences (persistent, typed) | MISSING | only `MemoryType.PREFERENCE` entries in MemoryService; `utils/preferences/ECorePreferences` is Eclipse UI config |
| Shared identity across applications | MISSING | no external identity API, no token federation; TEIP = static token only |
| Identity for external MCP clients | MISSING | MCP principal = unverified `x-evo-user` header |

## 2. Sessions

| Capability | Status | Evidence |
|---|---|---|
| Per-session container (bus, registries, memory, executor) | EXISTS | `orchestration/SessionContainer.java`, `SessionContext.java` |
| Session registry (in-process) | EXISTS | `orchestration/SessionManager.java` (static map) |
| Session ID service / canonical IDs | MISSING | no `SessionIds` class; UUIDs ad hoc (`OrchestratorServiceImpl.handle`) |
| Kernel checkpoint save/restore | EXISTS | `IterationManager.saveFullCheckpoint/restoreStateFromCheckpoint`, `orchestrator/memory/checkpoint_<sessionId>.json` |
| Restart continuity (same sessionId resumes) | PARTIAL | `IterationManager` RESTART CONTINUITY block; requires new request with same id |
| Whole-session serialization (bus subscribers, cognitive state, approvals) | MISSING | checkpoints cover kernel state only |
| Session-to-project binding | PARTIAL | `TaskContext.projectRoot`, `IterationMemoryService.findEffectiveRoot`; no persisted registry |
| Cross-application session identity | MISSING | no shared session concept anywhere in family |

## 3. Conversation history

| Capability | Status | Evidence |
|---|---|---|
| Conversation model | EXISTS | EMF `ChatSession`/`ChatMessage` (`model/orchestration/`) |
| Runtime message production | EXISTS | `ConversationOutputController` (in-memory buffers), `OrchestratorServiceImpl.processLogEntry` agentType inference |
| Persistence | PARTIAL | only EMF XMI save (`orchestrator.xml`) when the editor saves; headless/remote sessions never persisted |
| Structured tool-call records | MISSING | tool output flattened into message text |
| Conversation search | MISSING | — |
| Compaction / summarization | MISSING | no compaction engine (DCode has 40-message windowing only) |
| History survival across restart (all modes) | MISSING | not guaranteed |

## 4. Memory

| Capability | Status | Evidence |
|---|---|---|
| Persistent memory store | EXISTS | `controller/memory/MemoryService` → `${WORKSPACE}/shared/memory/user_memory.json`; `MemoryEntry` with scope/type/source/importance/confidence/associatedId/metadata |
| Memory query (enum + substring) | EXISTS | `MemoryQuery.matches` — naive token containment, no scoring |
| Prompt injection of memory | EXISTS | `MemoryContextProvider.buildMemoryContext` (8 entries), wired in `ContextBuilder` |
| Evolutionary iteration memory | EXISTS | `orchestration/selfdev/IterationMemoryService` (JSON records, audit JSONL, error/hotspot indexes) |
| Failure memory / strategy reliability | PARTIAL | `FailureMemory`, `TrajectoryMemory` — in-memory only |
| Semantic workspace w/ decay + reinforce | PARTIAL | `orchestration/workspace/SemanticWorkspace` (decay 0.95, events) — in-memory only |
| Memory events on CRUD | MISSING | MemoryService publishes no RuntimeEvents |
| Memory consolidation from conversations | MISSING | only `DreamEngine.consolidateResults` writes OBSERVATIONs |
| Memory decay / reinforcement / dedup for MemoryService | MISSING | — |
| Episodic memory (conversation episodes) | MISSING | — |
| Procedural memory (cross-project strategies) | MISSING | TrajectoryMemory is session-scoped, in-memory |
| Unified memory authority | MISSING | three disconnected stores (MemoryService file, IterationMemoryService dir, EMF sharedMemory string) |

## 5. Context reconstruction

| Capability | Status | Evidence |
|---|---|---|
| Context pipeline (files+memory+attachments+constraints) | EXISTS | `ContextBuilder` (6-stage), `ContextPackage` |
| Attachment curation w/ budget + relevance | EXISTS | `orchestration/attachments/AttachmentInjector` (char budget 2000) |
| Token-budgeted file curation (mediation) | EXISTS | `mediation/analysis/ContextCurator` (32k tokens, scoring, FileManifest) |
| Intent analysis / mode routing | EXISTS | `PromptIntentAnalyzer`, `ModeRecognizer`, `CognitiveAnalysisPipeline`, `ContextAssistant`, `HybridAtomicIntentClassifier` |
| Semantic search | MISSING | no embeddings/vector store anywhere (verified repo-wide) |
| Runtime token estimation service | MISSING | only bytes/4 heuristic (`ContextCurator.estimateTokens`); `forge.tokenizer` BPE unwired |
| Unified token budget management | MISSING | budgets inconsistent (chars vs tokens), no budget manager across memory+attachments+code |
| Context quality assessment (confidence/freshness/warnings) | MISSING | no ContextAssessment |
| Conflict/contradiction detection in memory | MISSING | attachment contradiction suppression only |

## 6. MCP server

| Capability | Status | Evidence |
|---|---|---|
| MCP implementation (JSON-RPC 2.0, 2024-11-05) | EXISTS | `servers/mcp/server/{McpServer,JsonRpcDispatcher}` + `protocol/*` |
| Tool/Resource/Prompt registries | EXISTS | `tools/ToolRegistry`, `resources/ResourceRegistry`, `prompts/PromptRegistry` |
| Real EVO tools over MCP | EXISTS | `orchestration/mcp/EvoMcpToolProvider` (java.*, evo_project_info, evo_file_read, evo_git_status, evo_build_test_status, evo_darwin_status, evo_forge_status, evo_genome_inventory, evo_process_list/cancel) |
| MCP port 28280 (PortRegistry.EVO_MCP) | EXISTS | `resource/PortRegistry.java` line 22; started via `McpDemoServerManager` |
| MCP authorization (tool-level) | PARTIAL | `AuthorizationService` on dispatcher only in primary server wiring; prompts unprotected; no caller authentication |
| MCP authentication (token/OAuth) | MISSING | principal from unverified header, defaults "admin" |
| SSE / streaming transport | MISSING | plain HTTP POST only |
| Cognitive tools/resources (user/session/memory/context) | MISSING | none registered |
| Server-initiated notifications | MISSING | no `notifications/*` support |

## 7. Repository intelligence

| Capability | Status | Evidence |
|---|---|---|
| Target scanner (file walk, classification) | EXISTS | `mediation/scanner/TargetScanner`, `TargetClassificationDetector` |
| Semantic extraction + metadata sidecars | EXISTS | `mediation/analysis/SemanticExtractor`, `agents/MetadataAgent` (`.ai.json`) |
| Architecture reality model (facts/genes/gaps) | EXISTS | `mediation/model/TargetRealityModel` (ephemeral) |
| Deep architecture graph scanner | EXISTS | `orchestration/design/RepoArchitectureScanner` (5-level, OSGi/Maven aware) |
| Genome milestone pipeline (git+hash inventory+delta) | EXISTS | `selfdev.genome/milestone/MilestoneGenerator` |
| Genome repository persistence | MISSING | `repository/LocalGenomeRepository` in-memory |
| Project knowledge store (components/decisions/issues) | MISSING | no ADR/KnownIssue entities; markdown only |
| Build/test result history | MISSING | `Evaluator` parses stdout into EMF EvaluationResult; not archived |
| Repository snapshots (content-level) | MISSING | genome inventory is hash-level; `.git` ignored by scanners |

## 8. DCode

| Capability | Status | Evidence |
|---|---|---|
| Native agent loop + 24 tools | EXISTS | `harness/orchestration/ToolCallingAgentLoop`, `ToolEngine`, `java.tooling` |
| SQLite session/message persistence + recovery | EXISTS | `persistence/db/SchemaMigrationService`, `SqliteSessionService`, `SessionRecoveryService` |
| Event log with cursor replay | EXISTS | `persistence/events/EventLog` |
| Context assembly (local) | EXISTS | `harness/context/ContextEngine` |
| Context ingest API (external reconstructed context) | MISSING | no `POST /sessions/{id}/context` |
| Compaction/summarization | MISSING | 40-message/2000-char windowing only |
| Token estimation / budget enforcement | MISSING | fixed maxTokens 4096; contextWindowTokens always 0 |
| User identity / cross-session memory | MISSING | sessions have no principal |

## 9. ENR

| Capability | Status | Evidence |
|---|---|---|
| Inference facade over Ollama/native/llama.cpp | EXISTS | `enr/EnrRuntime`, `EnrHttpServer`, ENR-1.0.md |
| Model discovery/providers/capabilities/runtime | EXISTS | `/enr/v1/{models,providers,capabilities,runtime}` |
| Streaming (SSE) | EXISTS | `/enr/v1/chat/stream` |
| Token usage in responses | MISSING | `EnrChatResponse` lacks promptTokens/completionTokens |
| Context window metadata per model | MISSING | `EnrModel` lacks contextLength (DCode reads it, gets 0) |
| Embeddings endpoint | MISSING | — |
| Structured output | MISSING | — |
| Memory/context/history storage | MISSING (by design) | stateless per request (spec §8) |

## 10. TRYST

| Capability | Status | Evidence |
|---|---|---|
| Users, hashed bearer sessions, RBAC | EXISTS | `domain/security/AuthenticationService`, `domain/model/User`, migrations V3 |
| Task sessions + conversation messages (SQLite) | EXISTS | `sessions`/`conversation_messages` tables (V1), `SqliteConversationMessageRepository` |
| Task recovery / checkpoints | EXISTS | `TaskRecoveryService`, V15 `task_recovery_checkpoints` |
| Context assembly (local, 4096-token budget) | EXISTS | `domain/ai/AiContextManager`, `PromptContextBuilder` |
| MCP server (scoped tools) | EXISTS | `evo.integration/mcp/McpServer`, `ScopedMcpTools` |
| MCP client | MISSING | — |
| TEIP client to EVO | EXISTS | `evo.integration/client/TeipClient`, V13 correlation |
| DCode adapter (V20) / ENR provider | EXISTS | `infrastructure/dcode/DCodeHttpClient` + `DCodeCodingHarnessAdapter`; `infrastructure/ai/EnrProvider` |
| Shared identity / external memory consumption | MISSING | users local-only; no identity federation; no external memory source |

## 11. DStudio

| Capability | Status | Evidence |
|---|---|---|
| Executions + reviews persistence | EXISTS | `core/service/DefaultExecutionService`, `SqliteExecutionStore`, migrations V1–V2 |
| Restart reconciliation (fail-fast) | EXISTS | `DefaultExecutionService.reconcileAfterRestart()` |
| TEIP 1.1 server (48080) | EXISTS | `dstudio.teip` bundle (`DStudioServer`, `TeipHttpHandler`, `TeipAuth`) |
| Harness + inference registries | EXISTS | `core/harness/HarnessRegistry`, `core/inference/InferenceEngineRegistry`, DCode + ENR adapters |
| `/api/inference` | EXISTS | `teip/server/InferenceHttpHandler` |
| User identity / roles | MISSING | single static bearer token (`TeipAuth`); no users table |
| Conversation/memory/preferences | MISSING | verified absent |
| MCP client | MISSING | — |

---

## Cross-cutting inventory summary

**Reusable platform assets**: SQLite user/permission/audit infrastructure (servers bundle), MemoryService + MemoryEntry enums, IterationMemoryService JSON pattern, RuntimeEventBus/RuntimeEventType event vocabulary, EMF model + XMI resource machinery, MCP registries + dispatcher + protocol model, PortRegistry/PortAllocationManager, ResourceManager path facade, ContextBuilder/ContextCurator/AttachmentInjector context machinery, mediation scanner + genome pipeline, TEIP auth pattern, Darwin engine registry + SessionManager.

**Verified missing capabilities (must be built or consciously waived)**:
1. Canonical, shareable user identity + external auth for MCP clients.
2. User profile/preference service (typed, persistent).
3. Cross-application session identity + session registry persistence.
4. Guaranteed conversation persistence + structured tool-call records + compaction.
5. Unified memory authority with events, consolidation, decay, relationships, versioning.
6. Embeddings/semantic search (optional, phased) + runtime token estimation service.
7. Unified context budget management + context quality assessment.
8. MCP cognitive tools/resources + MCP bearer authentication + SSE transport.
9. Persistent project knowledge store (components, ADRs, known issues, build/test history).
10. ENR token usage + context-window metadata + embeddings capability (additive).
11. DCode context-ingest route + metadata round-trip fix.
12. MCP clients in TRYST/DStudio + identity consumption points.
