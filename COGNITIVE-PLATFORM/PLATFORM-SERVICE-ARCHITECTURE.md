# PLATFORM SERVICE ARCHITECTURE

Native Java/OSGi service layer implementing the cognitive capabilities. Placement policy: **no new bundles for services** — everything lands in `eu.kalafatic.evolution.controller` (runtime services) or `eu.kalafatic.evolution.servers` (persistence/auth extensions), following the existing bundle structure. MCP adapters are thin facades over these services (same implementation for internal and external consumers).

## 1. Service layout

```text
eu.kalafatic.evolution.controller
 └─ eu/kalafatic/evolution/controller/cognitive/   (NEW package)
     ├─ CognitiveContextService.java        (facade, OSGi service)
     ├─ MemoryServiceExt.java               → delegates to memory.MemoryService (no fork)
     ├─ ConversationHistoryService.java
     ├─ SessionContinuityService.java
     ├─ ProjectKnowledgeService.java
     ├─ UserContextService.java
     ├─ PersonalizationService.java
     ├─ KnowledgeExtractionService.java
     ├─ MemoryConsolidationService.java
     ├─ ContextReconstructionService.java
     ├─ ContextQualityService.java
     ├─ TokenEstimationService.java
     └─ model/ (ContextRequest, ContextPackage extension, ContextAssessment, sources)

eu.kalafatic.evolution.servers
 └─ eu/kalafatic/evolution/servers/ (EXT)
     ├─ service/IdentityService.java        (apps + tokens over existing repositories)
     └─ model/ApplicationIdentity.java, ApplicationAccess.java
```

OSGi registration: services are registered in the existing activator pattern (controller's `ControllerActivator` / wherever `OrchestratorService` is registered — verified: singleton patterns dominate; new services follow `@Component`-less manual registration used by this codebase, e.g. `ServerManager`/`KernelFactory` wiring style). Singleton statics are the codebase norm (`MemoryService.getInstance()`, `SessionManager.INSTANCE`, `AuthorizationService.getInstance()`); cognitive services follow the same norm **plus** OSGi service registration for external-bundle consumption.

## 2. Service responsibilities

### CognitiveContextService (facade)
- **Responsibility**: single entry point for native context requests; delegates to reconstruction/personalization/project knowledge; returns ContextPackage.
- **Interface**: `ContextPackage getContext(ContextRequest req)`, `ContextPackage searchContext(...)`, `void recordContext(...)`.
- **Dependencies**: SessionContinuityService, MemoryService (existing), ProjectKnowledgeService, UserContextService, ContextReconstructionService.
- **Lifecycle**: stateless, per-call; created on demand (singleton).
- **Consumers**: MCP tool adapter (`CognitiveMcpToolProvider`), Darwin `ContextBuilder` integration (additive), TRYST/DStudio via MCP.
- **Events**: publishes `CONTEXT_RECONSTRUCTED` RuntimeEvent (bus of the requesting session when available).

### MemoryService (EXISTING — extended)
- Extends `controller/memory/MemoryService` with: versioning, lastVerifiedAt, validity, relationships, FTS index row mirror, CRUD events (`MEMORY_STORED/UPDATE_RETRACTED`), consolidation hooks. File format stays `user_memory.json` (backward compatible readers: `ContextBuilder`, `ContextualHelpService`, `NeuronContextService`, `DreamEngine`).
- Sub-services Episodic/Semantic/ProceduralMemoryService are **query facades over MemoryType** (no separate stores): `listByType(MemoryType)`, `search(...)`, `getProceduresFor(goal)`.

### MemoryConsolidationService
- **Responsibility**: asynchronous post-interaction consolidation: conversation summary → EPISODE entry; repeated tool outcomes → SOLUTION/EXPERIENCE; repeated user corrections → PREFERENCE (inferred).
- **Interface**: `void consolidate(ConsolidationJob job)` (job = conversationId/sessionId + outcome signals).
- **Dependencies**: ConversationHistoryService, MemoryService, TokenEstimationService (summary truncation), `forge.controller` extraction patterns reused conceptually.
- **Lifecycle**: triggered post-session (executor); never blocks inference.
- **Events**: `MEMORY_CONSOLIDATED`.

### KnowledgeExtractionService
- **Responsibility**: extract structured facts (triples) from conversations/artifacts; propose MemoryEntries with confidence; propose ArchitecturalDecisions/KnownIssues into ProjectKnowledgeService.
- **Dependencies**: memory.MemoryService, ProjectKnowledgeService, LLM via existing `LlmRouter`/`AiService` (only when model available; deterministic fallback: keyword/tag extraction).
- **Events**: `KNOWLEDGE_EXTRACTED`.

### UserContextService
- **Responsibility**: assemble user context block (profile, preferences filtered by scope, relevant memory, app access info).
- **Dependencies**: servers IdentityService (users/preferences), MemoryService (scope=USER).
- **Consumers**: CognitiveContextService, MCP `user_*` tools.

### PersonalizationService
- **Responsibility**: typed preferences with EXPLICIT/INFERRED sources, precedence enforcement (explicit always wins; see PERSONALIZATION-AND-LEARNING.md), contradiction resolution, correction handling.
- **Interface**: `getEffectivePreferences(userId, projectRef)`, `updateExplicit(...)`, `recordInference(...)`, `handleCorrection(...)`.
- **Persistence**: SQL `user_preferences` + MemoryEntry PREFERENCE mirror (preferences with provenance).

### SessionContinuityService
- **Responsibility**: durable session registry (cognitive_sessions), snapshot creation/restore, relationship recording; delegates kernel-state snapshot to the existing `IterationMemoryService` checkpoint mechanism.
- **Interface**: `beginSession(kind, owner, project) → id`, `snapshot(sessionId, kind)`, `restore(sessionId) → SessionRestoreResult`, `listSessions(filter)`, `relate(a,b,kind)`.
- **Dependencies**: SessionManager (runtime), IterationManager (kernel restore — reuses `loadCheckpoint`/`restoreStateFromCheckpoint`), DatabaseManager.
- **Events**: `SESSION_REGISTERED`, `SESSION_SNAPSHOT_CREATED`, `SESSION_RESTORED`.

### ConversationHistoryService
- **Responsibility**: durable append-only conversation store + indexed search; writes structured message/tool records; serves compaction summaries.
- **Interface**: `append(conversationId, msg)`, `list(conversationId, from, limit)`, `search(query)`, `compact(conversationId, budgetTokens) → summary`.
- **Persistence**: JSONL `<workspace>/shared/conversations/<id>.jsonl` + SQLite `conversation_messages` (FTS5 for search).
- **Integration**: `ConversationOutputController` gains one call to append (existing buffering preserved); EMF ChatSession unchanged.

### ProjectKnowledgeService
- **Responsibilities**: CRUD/query for components, ADRs, known issues, build/test results, snapshots; import from `MilestoneGenerator`/`RepoArchitectureScanner`/`Evaluator`.
- **Interface**: `getProject(projectRef)`, `recordDecision(...)`, `searchDecisions(...)`, `recordBuild(...)`, `recordTest(...)`, `importGenome(projectRoot)`, `getState(projectRef) → ProjectStatePackage`.
- **Persistence**: SQLite tables (see domain model).

### ContextReconstructionService
- **Responsibility**: the pipeline (see CONTEXT-RECONSTRUCTION-DESIGN.md): classify → intent → gather (user/session/project/memory/decisions/issues/repo) → conflict detection → freshness → ranking → dedup → budget → assemble.
- **Dependencies**: all services above + existing `ContextCurator`/`ContextResolver`/`AttachmentInjector`/`MemoryContextProvider` reused as source adapters.
- **Interface**: `ContextPackage reconstruct(ContextRequest)`.
- **Events**: `CONTEXT_RECONSTRUCTED`.

### ContextQualityService
- **Responsibility**: ContextAssessment: completeness vs requested categories, conflict report, staleness (lastVerifiedAt/updatedAt vs now), budget utilization, warnings.
- **Interface**: `ContextAssessment assess(ContextRequest, ContextPackage)`.

### TokenEstimationService
- **Responsibility**: single token counter: tries trained BPE (`forge.tokenizer`), falls back to char/4; per-section budget enforcement helper.
- **Interface**: `int estimateTokens(String)`, `boolean fits(String, int)`.

### IdentityService (servers bundle extension)
- **Responsibility**: ApplicationIdentity CRUD, token issuance/validation, scope enforcement binding; reuses `AuthService`/`AuthorizationService`/repositories.
- **Interface**: `ApplicationAccess issueAccess(appId, userId, scopes)`, `Principal validateToken(token)`.

## 3. Explicit reuse vs new

| Reuse as-is | Extend in place | New (small) |
|---|---|---|
| SessionManager, SessionContainer/SessionContext | MemoryService (+versioning/validity/events) | CognitiveContextService (facade) |
| IterationManager + checkpoint machinery | ContextPackage (+assessment/sources) | ConversationHistoryService |
| MemoryContextProvider, ContextResolver, ContextCurator, AttachmentInjector | MemorySource/MemoryType enums (+values) | SessionContinuityService |
| servers AuthService/AuthorizationService/UserService/repositories | JsonRpcDispatcher (+bearer authn, +prompt authz) | ProjectKnowledgeService |
| MCP ToolRegistry/ResourceRegistry/dispatcher | EvoMcpToolProvider registration list (+cognitive provider) | ContextReconstructionService, ContextQualityService, TokenEstimationService |
| PortRegistry 28280 / PortAllocationManager | User table (+profile columns) | MemoryConsolidationService, KnowledgeExtractionService, PersonalizationService |
| ResourceManager paths, DatabaseManager | DatabaseManager (+cognitive tables) | IdentityService extension |
| RuntimeEventBus + RuntimeEventType (+MEMORY_*/CONTEXT_* types) | ENR records (additive fields) | — |

## 4. Dependency diagram

```text
                    MCP (28280)                       Darwin orchestration
 CognitiveMcpToolProvider (adapters)          ContextBuilder (additive call)
        │  tool calls                               │
        ▼                                           ▼
 CognitiveContextService (facade) ◄──────── ContextReconstructionService
   │           │                │                     │
   │           ▼                │                     ▼
   │  UserContextService        │            TokenEstimationService (forge.tokenizer)
   │  (IdentityService,         │
   │   MemoryService USER)      ▼
   │                  ContextQualityService
   ▼
 SessionContinuityService ──► SessionManager (runtime) + IterationMemoryService (checkpoints)
   │                          + cognitive_sessions (SQLite)
   ▼
 ConversationHistoryService ──► JSONL + SQLite FTS
   │
   ▼
 MemoryConsolidationService ──► MemoryService (user_memory.json + index)
 KnowledgeExtractionService ──► MemoryService + ProjectKnowledgeService
 ProjectKnowledgeService ─────► SQLite (project_*) ←─ imports: MilestoneGenerator, RepoArchitectureScanner, Evaluator
 PersonalizationService ──────► SQL user_preferences + MemoryService PREFERENCE mirror
```

## 5. Sequence (context_get via MCP)

```text
Client ── POST /mcp tools/call context_get ──► JsonRpcDispatcher
  1. authenticate bearer → IdentityService.validateToken → principal (user, app, scopes)
  2. authorize "MCP_TOOL" "context_get" EXECUTE → AuthorizationService (audit)
  3. CognitiveMcpToolProvider.execute → CognitiveContextService.getContext(request)
     a. SessionContinuityService.resolve(sessionId) [or create]
     b. UserContextService.build(userId, projectRef, scope filter)
     c. MemoryService.retrieveRelevant(MemoryQuery(scope,project,timewindow))
     d. ProjectKnowledgeService.getState(projectRef) + searchDecisions + knownIssues
     e. ConversationHistoryService.list(conversationId, N) / compact
     f. ContextReconstructionService: conflict+freshness+rank+dedup+budget(assemble)
     g. ContextQualityService.assess → ContextAssessment
  4. → ContextPackage (contextId, schemaVersion, tokenEstimate, warnings) → MCP result
```

## 6. Testing hooks

- Services are plain Java (no SWT dependency) → JUnit unit tests in `eu.kalafatic.evolution.controller.tests` (mirroring existing `UserMemorySubsystemTest`, `CheckpointDeterminismTest` patterns).
- OSGi wiring verified via existing activation tests; MCP path via dispatcher-level tests (existing `EvoMcpToolProvider` test style).
- Persistence via temp-dir SQLite/JSON like existing repository tests (`ConnectedApplicationRegistryTest`).
