# EXISTING ARCHITECTURE AUDIT

Phase A discovery. All findings are source-verified (file paths given). Date: 2026-11-17.
Scope: EVO (`evolution-121126`), with consumer views of DCode (`DCoder-121126`), TRYST (`tryst-121126`), DStudio (`DStudio-121126`).

---

## A1. Platform structure

### Bundles (EVO reactor root, Tycho)

| Bundle | Role |
|---|---|
| `eu.kalafatic.evolution.controller` | Core orchestration, sessions, Darwin engines, memory, context, tools, servers (primary HTTP 28080, TEIP), MCP tool provider |
| `eu.kalafatic.evolution.view` | SWT UI (MultiPageEditor, pages, editors) |
| `eu.kalafatic.evolution.model` | EMF `orchestration` EPackage (evolution.ecore), generated model + edit/editor |
| `eu.kalafatic.evolution.servers` | Secondary portal server; MCP server classes (`servers/mcp/`); auth/user/role/permission + SQLite persistence |
| `eu.kalafatic.evolution.enr` | ENR 1.0 inference facade + HTTP/SSE (28780) |
| `eu.kalafatic.evolution.supervisor` | Supervisor process (REST 28480, control 28580) — no auth |
| `eu.kalafatic.evolution.repository` | p2 update-site/product build (not a code analyzer) |
| `eu.kalafatic.evolution.forge.*` | Forge/local-LLM family: agent, agent.api, controller, data, math, model, observability, runtime, tokenizer, trainer, lab |
| `eu.kalafatic.evolution.selfdev.genome` | Genome milestone generation, repository scanning, genome docs |
| `eu.kalafatic.evolution.utils` | Shared utils, `semantic` (EvoMetadata/AIContextTool), preferences |
| `eu.kalafatic.evolution.gpu`, `.media`, `.creatic`, `.module` | GPU detection; media; creatic analysis; module utils |
| tests | `.controller.tests`, `.model.tests`, `.quality.tests`, `.selfdev.genome.tests` |

### Persistence mechanisms (all exist today)

1. **SQLite** (`sqlite-jdbc 3.45.2.0`), only in `eu.kalafatic.evolution.servers`: `DatabaseManager` → `data/evolution.db` (WAL). Tables: `users`, `sessions`, `roles`, `permissions`, `user_roles`, `role_permissions`, `resources`, `resource_access`, `audit_log`. Also used by TEIP persistence (`teip/store/TeipPersistenceStore`, `teip/store/TeipEventStore`).
2. **EMF XMI** resources (`orchestrator.xml`) via `ResourceManager.getOrchestrator()` and `ProjectModelManager` — the configuration model (Orchestrator, ChatSession/ChatMessage, Task, Iteration, Git/Maven/ServerSettings…). Default `~/git/evolution/data/orchestrator.xml`.
3. **JSON files**: `MemoryService` → `${WORKSPACE}/shared/memory/user_memory.json`; `IterationMemoryService` → `<projectRoot>/orchestrator/memory/*.json` + `audit_trail.jsonl`; `ConnectedApplicationRegistry` JSON; genome outputs (`genome/current/genome.json`, `genome-docs/`).
4. **In-memory**: SessionManager, EventBus, trajectory registry, cognitive state, semantic workspace, ConversationOutputController buffers, LocalGenomeRepository.

### Server lifecycle (verified)

- `ServerManager.startServer()` (controller) starts primary `EvolutionServer` (28080) with routes: `/api/auth`, `/api/admin`, `/teip/v1`, `/ui/*` (MCP dispatcher), `/server/*`, `/task`, `/conversation/*`, `/forge/*`, `/develop`, `/mutation`, `/workspace/*` etc. (`orchestration/EvolutionServer.java`, 2914 lines).
- `ServerManager` also starts `McpDemoServerManager` on **28280** (`PortRegistry.EVO_MCP`, default 28280, bind 127.0.0.1) and the servers-bundle portal on 28180; ENR bundle starts `EnrHttpServer` on 28780; supervisor is a separate process (28480/28580).
- `PortRegistry` (`controller/resource/PortRegistry.java`): EVO_HTTP_PRIMARY 28080, secondary 28180, **EVO_MCP 28280**, DEVELOP 28380, supervisor REST 28480, supervisor control 28580, auxiliary 28680, **EVO_ENR 28780**. `PortAllocationManager` + `PortPersistence` make ports configurable and persisted through EMF `ServerSettings.mcpPort`.

### Dependency map (relevant slice)

```
view ──► controller ──► model (EMF), utils, forge.tokenizer, forge.model
controller ◄── servers (auth controllers used by controller's EvolutionServer)
enr ──► controller, servers, forge.model, utils      (EnrActivator → LlmRouter)
selfdev.genome ──► controller (reflection), utils
supervisor ──► (standalone process; REST only)
```

Key integration seams: `LlmRouter` (inference), `ToolRegistry` (MCP tools), `AuthorizationService` singleton (SQLite-backed), `SessionManager` (per-process session registry), `DarwinEngineRegistry` (per-session engines), `RuntimeEventBus` (per-session events).

---

## A2. Identity and user management

### What exists

- **`eu.kalafatic.evolution.servers`** is the only real user/identity implementation:
  - `model/User.java`: id, username, displayName, passwordHash, role (String), enabled, timestamps, description. **No preference/profile fields**.
  - `model/Session.java`: id, UUID sessionId, userId, createdAt, lastAccess, clientIp, workflowType, metadata, status (ACTIVE/TERMINATED/EXPIRED), sessionType (USER/SYSTEM/ANONYMOUS).
  - `model/Role.java` + `model/Permission.java` + `model/ResourceItem.java` + `model/ResourceAccessPolicy.java` (principalType USER/ROLE, action READ/EXECUTE/MANAGE/*, effect ALLOW/DENY) + `model/AuditLogEntry.java`.
  - `service/AuthService.java`: login (BCrypt via `security/BCryptUtils.java`), UUID session id, 60-min idle timeout, logout, cleanup.
  - `service/AuthorizationService.java`: singleton; `authorize(username, resourceType, resourceKey, action)`; SYSTEM/bootstrap bypass; role==ADMIN full access; DENY overrides ALLOW; role-permission fallback; audit logging.
  - `service/UserService.java`: user CRUD (admin API only).
  - HTTP: `controller/AuthController.java` (`/api/auth/login|logout|me`, session via Bearer/cookie/query), `controller/AdminApiController.java` (`/api/admin/users|sessions|roles|permissions|resources|policies|audit-logs`).
- **TEIP 1.1**: static bearer token only (`teip/controller/TeipApiController.java`, sysprop `evo.teip.auth.token`), loopback dev-mode opt-out. No user registry.
- **Supervisor**: no authentication.
- Identity is therefore **EVO-local, single-process, SQLite-backed**; the only external credential is the TEIP static token.

### Missing

- No shared/canonical user identity usable by TRYST/DStudio/DCode; no token validation or identity API for external apps; no user preferences model; `admin`/`admin` seed and `password123` defaults are production hazards; principal on MCP comes from an unverified `x-evo-user` header.

---

## A3. Session architecture

### What exists (all in `controller/orchestration/` unless noted)

- `SessionContainer.java` (interface) / `SessionContext.java` (sole impl): per-session object graph — RuntimeEventBus, SignalBus, SessionLogger, WorkflowStepRegistry, WorkflowGraphManager, EvolutionRegistry, CapabilityRegistry, StepModeController, EvolutionMemoryGraph, FileChangeTracker, SelectionState, SessionCognitiveState, BackpressureController, OrchestrationStatusManager, RuntimeCoordinator, EvolutionaryPressureEngine, EvolutionaryObservabilityManager, RuntimeContextCollector, agent registry, operations map, executor pool, lazy IterationMemoryService, IterationManager ref, TaskContext ref.
- `SessionManager.java`: singleton static `ConcurrentHashMap<String, SessionContainer>`; `getOrCreateSession(sessionId)`; `shutdownSession`; `getAllOperations`. **In-memory only; no session ID service exists (`SessionIds` not found).** IDs come from caller-supplied context, EMF `Orchestrator.id`, or `UUID.randomUUID()` (`OrchestratorServiceImpl.handle`, line 28–29).
- `IterationManager.java` (1828 lines, `@EvolutionComponent(role="state-authority")`): sole authority for iteration transitions (`transition()` line 434); checkpoint save (`saveFullCheckpoint()` line 1096) and restore (`loadCheckpoint`/`restoreStateFromCheckpoint` lines 1137–1219) via `IterationMemoryService` → `orchestrator/memory/checkpoint_<sessionId>.json`; RESTART CONTINUITY block (lines 314–338) resumes EMF `SelfDevSession` RUNNING or restores checkpoint; publishes `SESSION_RESUMED`.
- `KernelFacade` / `KernelFactory`: mode resolution (PromptIntentAnalyzer/ModeRecognizer), per-session `IterationManager` creation, `DarwinEngineRegistry` registration.
- `ADarwinEngine` (selfdev, 3838 lines) + `DarwinEngineFactory` mapping: FORGE→`LLMDarwinEngine`, SIMPLE_CHAT→`ChatEngine`, ASSISTED_CODING→`CodingEngine`, DEVELOPER→`DeveloperEngine`, MEDIATED→`MediatedEngine`, SELF_DEV_MODE→`SelfDevelopmentEngine`, INTENT_RECONSTRUCTION→`IntentReconstructionEngine`, MUTATION→`MutationEngine`, DREAM→`DreamEngine`, DARWIN→`DarwinEngine`.
- Session→project binding: `TaskContext.projectRoot`; `IterationMemoryService.findEffectiveRoot` walks up for `iterations/`/`orchestrator/`/`.git`/`pom.xml`; EMF `ChatSession.targetPath/targetType/outputPath`.
- Events: `RuntimeEventBus` (per-session, throttled, session-invariant enforced) + ~70 `RuntimeEventType`s; `SessionLogger` → `logs/sessions/session_<id>.log` (5 MB × 3 rotation).

### Missing

- No persisted session registry/index, no canonical session-id scheme, no cross-application session identity, no serialization of the whole SessionContext (checkpoints cover kernel state only), no retention policy for `orchestrator/memory/*`.

---

## A4. Conversation history

### What exists

- EMF `ChatSession`/`ChatMessage` (`model/orchestration/`): messages with sender/text/agentType/timestamp/priority/sequenceNumber/turnId/reasoning/responseKind; containment in `AiChat.sessions` on the `Orchestrator` model.
- Runtime writer: `ConversationOutputController` (`controller/orchestration/ConversationOutputController.java`) — in-memory per-turn buffers (300 ms), agentType inferred from log tags in `OrchestratorServiceImpl.processLogEntry`; UI (`view/editors/pages/aichat/ChatGroup.java`) appends to EMF session.
- **Persistence: only via EMF XMI save** (`MultiPageEditor.doSave` → `resource.save`; `ResourceManager.getOrchestrator()` loads `~/git/evolution/data/orchestrator.xml`). Headless/remote conversations are never persisted.
- Tool calls/results are flattened into message text with agentType labels; no structured record.
- Search/compaction: none. `ConversationState` (`orchestration/ConversationState.java`) stores last-5-messages + decisions JSON inside `Orchestrator.sharedMemory` (EMF-persisted, per session id).

### Missing

- Append-only conversation store, structured tool-call records, conversation search, compaction/summarization, guaranteed survival across restart for all modes.

---

## A5. Existing memory and context

### Memory implementations (three disconnected stores)

1. **`MemoryService`** (`controller/memory/`): singleton; JSON file `${WORKSPACE}/shared/memory/user_memory.json`; `MemoryEntry` (id, content, scope USER/PROJECT/SESSION/MODE/TASK, type FACT/PREFERENCE/DECISION/RULE/CONSTRAINT/GOAL/PROJECT_CONTEXT/OBSERVATION/EXPERIENCE/SOLUTION, source USER/SESSION/INFERENCE/TOOL/GIT/BUILD/TEST/LOG/DOCUMENT/TRAINING/SYSTEM, importance LOW..CRITICAL, confidence, timestamps, associatedId, metadata); `MemoryQuery` = exact enum match + naive substring token containment; `MemoryContextProvider` renders prompt blocks. **No events, no decay, no dedup, no consolidation.** Consumers: `ContextBuilder` (8 entries), `ContextualHelpService`, `NeuronContextService`, `DreamEngine` (only automated writer), `DreamContextRetriever`, `DreamTopicSelector`.
2. **`IterationMemoryService`** (`orchestration/selfdev/`): per-project evolutionary memory — `orchestrator/memory/{iteration_*.json,trajectory_*.json,evolution_tree.json,checkpoint_*.json}` + `audit_trail.jsonl`; error index, hotspot index, `FailureMemory`, `TrajectoryMemory` (in-memory: successfulStrategies, recurringFailureLoops, `userPreferredStyles`, strategy reliability), `EvolutionMemoryGraph`.
3. **Semantic workspace**: `orchestration/workspace/SemanticWorkspace` + `WorkspaceArtifact` + `TrajectoryMemory` + `ContextResolver` — decay 0.95, reinforce, relevance scoring, events (`MEMORY_DECAY_APPLIED`, `CONTEXT_RETRIEVED`) — **in-memory only** (documented as persistent in `docs/SEMANTIC_WORKSPACE_ARCHITECTURE.md`; implementation is not).

### Context construction

- `ContextBuilder` pipeline (RAW_INPUT→ANALYSIS→ENRICHMENT→FILTERING→COMPRESSION→ASSEMBLY): `[FILE:...]` extraction, architectural file expansion, file reading, relevance compression (4096-char / first-100-lines heuristics), `ContextResolver` artifact injection, hardcoded constraints, attachments (`attachments/AttachmentInjector`, char budget 2000, hybrid scoring, contradiction suppression), `MemoryContextProvider` (8 entries). **No token budget management; budget units are inconsistent (chars vs bytes/4 heuristic).**
- `ContextAssistant`: LLM pre-pass mode classification with LOW/MEDIUM/HIGH confidence, loads ConversationState.
- Mediation: `mediation/analysis/ContextCurator` (32 000-token budget, authority/influence scoring, `estimateTokens` = bytes/4), `analysis/PromptSynthesizer`, `analysis/SemanticExtractor`, `model/TargetSnapshot`/`TargetRealityModel`/`ArchitecturalFact` (subject/predicate/confidence triples)/`ArchitecturalGene`/`KnowledgeGap`/`Subsystem`. All in-memory per cycle.

### Embeddings / vector / token / compaction (searched repo-wide)

- **No embeddings, no vector store, no cosine similarity** for memory retrieval anywhere.
- Token estimation: only `ContextCurator.estimateTokens` (bytes/4) and char budgets. `forge.tokenizer` (BPE/char/word tokenizers) is on the controller classpath (`MANIFEST.MF`) but **not wired** into any context pipeline.
- **No automatic context compaction/summarization engine.**
- `docs/COGNITIVE_TRACING_ARCHITECTURE.md` describes `IntrospectionEngine`/`ReplayEngine`; only `CognitiveTrace`/`CausalNode` models exist in `orchestration/diagnostics/` — the engines are not implemented.

### Knowledge extraction

- `forge.controller/.../agents/KnowledgeExtractionAgent` — training-data knowledge facts only.
- Conversation→memory consolidation: absent (only `DreamEngine.consolidateResults()` writes OBSERVATION entries).
- Mediation `ArchitecturalFact`/`ArchitecturalGene`/`KnowledgeGap` — ephemeral, not persisted.

### User preferences

- Only `MemoryType.PREFERENCE` entries (manually created), in-memory `TrajectoryMemory.userPreferredStyles`, EMF model fields, and `eu.kalafatic.utils/preferences/ECorePreferences` (Eclipse UI preference pages, not conversational preferences).

---

## A6. MCP architecture

### Three MCP surfaces exist; one is authoritative

1. **`eu.kalafatic.evolution.servers/mcp/`** — full MCP implementation: `server/McpServer` (NanoHTTPD, POST JSON-RPC 2.0), `server/JsonRpcDispatcher` (methods: `initialize` → protocolVersion **2024-11-05**, `tools/list|call`, `resources/list|read`, `prompts/list|get`, `ping`, `shutdown`), `server/McpConfig` (port 28280, `application.json`), `tools/ToolRegistry` + `ToolExecutor`, `resources/ResourceRegistry` + `ResourceProvider`, `prompts/PromptRegistry` + `PromptProvider`, `model/{Tool,Resource,Prompt,Implementation,McpExecutionContext}` (60 s timeout, cancellation flag), `protocol/{McpRequest,McpResponse,McpError}`, `demo/*` (documentation resources). **`startMcpServer()`/`startDemoMcpServer()` in the servers-bundle `EvolutionServer` are dead code — never called.**
2. **Actual 28280 listener**: `controller/orchestration/mcp/McpDemoServerManager` → `demo/DemoDocumentationProvider` (docs resources, DemoTools/DemoResources/DemoPrompts, **no AuthorizationService**, principal defaults to `"admin"` from unverified `x-evo-user` header).
3. **Actual wired MCP surface**: primary `EvolutionServer` (28080) — `DemoDocumentController` at `/ui/*` with `JsonRpcDispatcher` **plus** `EvoMcpToolProvider.registerAuthoritativeTools(...)` and `dispatcher.setAuthorizationService(AuthorizationService.getInstance(dbManager))`. `EvoMcpToolProvider` (`controller/orchestration/mcp/EvoMcpToolProvider.java`) registers real tools: `java.inspect_class|inspect_method|find_references|find_callers|find_implementations|get_diagnostics|get_project_structure|get_dependencies|format` (via `ToolFactory`, `ProjectModelManager.getOrDiscoverWorkspace()`), `evo_project_info`, `evo_file_read` (canonical containment), `evo_git_status` (`GitTool.getCachedLocalRepositories()`), `evo_build_test_status`, `evo_darwin_status` (`DarwinEngineRegistry`), `evo_forge_status`, `evo_genome_inventory` (`FunctionalityInventoryManager`), `evo_process_list`/`evo_process_cancel` (`SessionManager`, ownership-guarded).

### Protocol facts (constraints for any extension)

- Transport: plain HTTP POST JSON-RPC 2.0, Jackson serialization. **No SSE, no stdio, no notifications**, fixed protocol version string `2024-11-05`, no `resources/subscribe`, no sampling/completion.
- Auth on MCP: `JsonRpcDispatcher` calls `AuthorizationService.authorize(principal, "MCP_TOOL", toolName, "EXECUTE")` for `tools/call` and `("RESOURCE","*","READ")` for resources — but only when an AuthorizationService was set; prompts have **no** auth check; the principal is an unverified header; no authentication of the caller.
- Client: `orchestration/mcp/McpClient.java` (UI connection test) and `teip/mcp/TrystMcpClient.java` (outbound to TRYST MCP; fail-closed scope map).

### Missing (MCP)

- No MCP tools/resources exposing users, sessions, memory, conversation history, decisions, or project knowledge; no MCP authentication (bearer/token validation); no SSE streaming; no per-resource URI authz; no async tool execution model.

---

## A7. Repository intelligence

### Exists

- **Mediation scanner** (`controller/mediation/`): `scanner/TargetScanner` (recursive walk, ignores `.git,target,build,.settings,.metadata`, extension classification via `forge.data.SourceAdapterRegistry`, technology detection), `scanner/TargetClassificationDetector`, `analysis/SemanticExtractor` (node summaries, tags, structures, dependencies, `.evo`/`.evodata`/`modelfile` specialization, `EvoMetadata` sidecar repair), `model/TargetSnapshot`/`SemanticNode`/`SemanticEdge`/`TargetRealityModel` (facts/genes/use-cases/gaps/hotspots), `analysis/ContextCurator` (token-budgeted file selection + `FileManifest`), `analysis/PromptSynthesizer`. Consumers: `MediationEngine` (7-step cycle, skeletal delta/hotspot logic), `RealityDiscoveryAgent` (LLM JSON → `TaskContext` orchestration-state metadata), `MetadataAgent` (writes `.ai.json` + navigation maps), `MediatedExportManager` (ZIP export). **No persistence of snapshots/models.**
- **Design scanner** (`orchestration/design/`): `RepoArchitectureScanner` (5-level regex Java/OSGi/Maven graph: REPOSITORY→MODULE→PACKAGE→CLASS→METHOD; relationships incl. OSGI_REQUIRE_BUNDLE, EXTENSION, MAVEN_DEPENDENCY), `DesignModel`/`ComponentRecord`/`RelationshipRecord`, `DesignRenderer`/`DesignExporter` (HTML/JSON). Result cached in `ConversationState` JSON under `orchestrator.sharedMemory` (per session).
- **Genome** (`eu.kalafatic.evolution.selfdev.genome`): `hub/SelfDevGenomeHub`, `milestone/MilestoneGenerator` (staged: GIT_METADATA → SHA-256 FILE INVENTORY → delta vs `genome/current/genome.json` → EvoMetadata scan → architecture discovery (reflective `RepoArchitectureScanner`) → artifacts `genome/current/{genome.json,architecture.md,use_cases.md,milestone_v1.md,...}` + history + `genome-docs/<date>/SYSTEM_ANALYSIS...md`), `repository/LocalGenomeRepository` (**in-memory only**), bundled seed knowledge markdown (`genome/decisions/DESIGN_DECISIONS.md`, `genome/limitations/KNOWN_LIMITATIONS.md`, `architecture/DARWIN.md` etc.).
- **EMF project state**: `EvoProject`/`Orchestrator`/`Git`/`Maven`/`Iteration`/`EvaluationResult` (success, testPassRate, errors, decision)/`SelfDevSession`/`ChangeSet`/`FileChange`/`DiffHunk`/`ReviewSession`/`GenomeSnapshot` — XMI persistence only.
- **Build/test capture**: `tools/MavenTool` (raw stdout), `selfdev/Evaluator` (tiered RealityLevel; parses BUILD SUCCESS / `Tests run:`; emits EvaluationSignal; builds EMF EvaluationResult) — **results are not historically persisted** (no active `Iteration.setEvaluationResult` callers).
- VCS: `vcs/GitVersionControlProvider` (git log/diff/show/status via ProcessBuilder), `trajectory/GitEmfReconciler`.

### Missing

- No persistent queryable Project Knowledge store; no ADR/KnownIssue entities (only markdown); genome repository in-memory; mediation snapshots ephemeral; build/test outcomes not archived; no content-level repository snapshots; `.git` history ignored by all scanners.

---

## A8. DCode and ENR

### DCode (`DCoder-121126`)

- 9 bundles: `core` (domain/event bus/lifecycle), `config` (LLM provider SPI `LlmProvider`, `DefaultProviderRegistry`, `DefaultModelRouter`, credential vault), `repositories` (JGit), `persistence` (SQLite `dcode.db`, `SchemaMigrationService` V1, `SqliteSessionService`, `EventLog` with cursor replay, `SessionRecoveryService`: RUNNING/WAITING_FOR_APPROVAL → FAILED on restart), `harness` (`ToolCallingAgentLoop`, `ContextEngine`, `ToolEngine` + 11 tools), `java.tooling` (13 JDT/PDE tools), `server` (NanoHTTPD `/api/v1` + SSE, port 48080), `client`, `ui`.
- Context assembly: `ContextEngine.buildSystemPrompt` (identity, task, JSON tool protocol, tool catalog, repository summary, rules) + one-shot `historySummary` = **last 40 messages × 2000 chars, no summarization**. Tool results appended as user messages in-loop; persisted to `messages` table (USER/ASSISTANT/TOOL/SYSTEM).
- Tool history: `ToolCall` in-memory + events; `tool_calls`/`agent_runs` tables exist in schema but **no repository writes them**.
- No token estimation; `ModelDefinition.contextWindowTokens` is 0 for all providers; fixed `maxTokens=4096`.
- No user identity (sessions have no principal), no memory/preferences/cross-session continuity beyond per-session SQLite.
- FHC 1.0 (`PHASE-6-FHC-1.0.md`, DCode = spec caretaker): harness delegation via `/api/v1`; TEIP server surface in DCode deferred.

### ENR (`eu.kalafatic.evolution.enr`)

- Normative: `docs/protocol/ENR-1.0.md` — stateless per request; ENR owns inference runtime state only.
- `EnrInferenceEngine` (contract + wire records), `EnrRuntime` (routes `ollama:`/`evo-native:`/`llama-cpp:` prefixes to `OllamaService`/`NativeEvoProvider`/`LlamaService`; GPU via `gpu.diag.NvidiaGpuDetector`), `EnrHttpServer` (`/enr/v1/{health,models,models/{id}/load|unload,models/pull,chat,chat/stream,runtime,providers,capabilities}`, global bearer via `evo.enr.auth.token`, SSE chunks), `EnrLlmProvider` (Darwin `ILlmProvider` bridge, registered in `LlmRouter` by `EnrActivator`).
- **Gaps**: no token usage fields in `EnrChatResponse` (Ollama `prompt_eval_count`/`eval_count` discarded), no `contextLength`/context-window metadata in `EnrModel` (DCode reads `contextLength` → always 0), no embeddings endpoint, no structured-output support, no persistence at all (verified).

---

## Consolidated picture

- EVO already owns the pieces of a cognitive platform — identity DB, per-session orchestration state, memory files, context pipelines, repository intelligence — but they are **fragmented across three persistence silos (SQLite servers DB, EMF XMI, ad-hoc JSON), three MCP surfaces, and zero cross-application contracts**.
- The existing MCP server is demo/documentation-oriented on 28280; the authoritative tools live on the 28080 UI dispatcher. Neither exposes cognitive services.
- External apps (TRYST, DStudio, DCode) each have local identity/persistence and no consumption path for shared cognitive services.
