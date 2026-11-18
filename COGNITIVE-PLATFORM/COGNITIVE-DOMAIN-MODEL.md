# COGNITIVE DOMAIN MODEL

Unified domain model for EVO Platform cognitive services. Every entity below either **extends an existing implementation** or is a **new minimal type** with a concrete persistence home. EMF usage is deliberate: EMF remains the *configuration/editor* model (Orchestrator tree) and is NOT extended for high-volume cognitive data; the cognitive store uses the existing SQLite `DatabaseManager` + JSONL file patterns (see DATA-PERSISTENCE-STRATEGY.md for rationale).

Legend: [EXT] = additive extension of existing type; [NEW] = new type; SQL = `data/evolution.db`; JSON = JSON file; JSONL = append-only.

---

## 1. Identity

| Entity | Kind | Fields | Home |
|---|---|---|---|
| UserIdentity | [EXT] of `servers/model/User` | + preferredAppIds, ecosystemProfileId | SQL `users` (new columns) |
| UserProfile | [NEW] | id, userId, displayName, avatarHint, bio, locales, timezone, workingHints | SQL `user_profiles` |
| UserPreference | [NEW] | id, userId, key, valueJson, scope (USER/PROJECT/APPLICATION), source (EXPLICIT/INFERRED), confidence, updatedAt | SQL `user_preferences` |
| ApplicationIdentity | [NEW] | id, name, kind (EVO/TRYST/DSTUDIO/DCODE/EXTERNAL_MCP), ownerUserId, enabled, createdAt | SQL `applications` |
| ApplicationAccess | [NEW] | id, applicationId, userId, grantedScopes (JSON list of RESOURCE:ACTION), revoked, issuedAt, expiresAt | SQL `application_tokens` + scope JSON |

Rules: one canonical UserIdentity per human across apps; ApplicationIdentity is per product instance; ApplicationAccess binds (app, user) to scopes. Scopes enumerated in SECURITY-AND-AUTHORIZATION.md.

## 2. Sessions

Logical distinctions only — **one** session runtime remains (`SessionManager`/`SessionContainer`). The `cognitive_sessions` table is the durable registry, not a new session manager.

| Entity | Kind | Fields | Home |
|---|---|---|---|
| CognitiveSession | [NEW] | id (UUID v4), kind (USER/APPLICATION/CONVERSATION/TASK), ownerUserId, applicationId, projectRef, status (ACTIVE/PAUSED/COMPLETED/FAILED/ARCHIVED), startedAt, lastActivityAt, endedAt, parentSessionId, metadata | SQL `cognitive_sessions` |
| SessionRelationship | [NEW] | id, fromSessionId, toSessionId, kind (CONTINUES/SPAWNED/SUBSUMES/RELATED) | SQL `session_relationships` |
| SessionSnapshot | [NEW] | id, sessionId, kind (KERNEL/CONTEXT/FULL), version, schemaVersion, stateJson, createdAt | JSONL `<workspace>/shared/sessions/<sessionId>/snapshot_<ts>.jsonl` + row in `session_snapshots` |

Reuse: kernel checkpoints (`orchestrator/memory/checkpoint_<sessionId>.json` via `IterationMemoryService`) remain authoritative for Darwin state; SessionSnapshot references them by id instead of duplicating.

## 3. Memory

One authority: the extended `MemoryService`. `MemoryEntry` gains additive fields; episodic/semantic/procedural are values of `type`, not new stores.

| Entity | Kind | Fields | Home |
|---|---|---|---|
| MemoryEntry | [EXT] of `controller/memory/MemoryEntry` | existing (id, content, scope, type, source, importance, confidence, createdAt, updatedAt, associatedId, metadata) + **version, lastVerifiedAt, validity (VALID/OUTDATED/SUPERSEDED/RETRACTED), relationships[], provenanceChain[]** | JSON `${WORKSPACE}/shared/memory/user_memory.json` (+ SQLite `memory_index` for search) |
| EpisodicMemory | value of `MemoryEntry.type=EPISODE` | + episodeStart/End, participantAppIds in metadata | same |
| SemanticMemory | value of `MemoryEntry.type=FACT|PROJECT_CONTEXT` | — | same |
| ProceduralMemory | value of `MemoryEntry.type=RULE|SOLUTION|EXPERIENCE` | + strategyId, outcome metadata | same |
| MemoryRelationship | [NEW] | id, fromEntryId, toEntryId, kind (SUPERSEDES/CONTRADICTS/SUPPORTS/DERIVES_FROM/REINFORCES) | inside MemoryEntry.relationships[] |
| MemorySource | existing enum + APP_MCP, CONSOLIDATION values | — | `MemorySource` |
| MemoryVersion | [NEW] | entryId, version, contentSnapshot, changedAt, changedBy | JSONL `<workspace>/shared/memory/versions/<entryId>.jsonl` |

Lifecycle states (explicit persistence only for RETRACTED/SUPERSEDED; others derived): DISCOVERED (confidence<threshold), VALIDATED (lastVerifiedAt recent), ACTIVE (default), UPDATED (version>1), SUPERSEDED (relationship SUPERSEDES exists), ARCHIVED (=RETRACTED flag).

## 4. Project Knowledge

| Entity | Kind | Fields | Home |
|---|---|---|---|
| ProjectKnowledge | [NEW] | id, projectKey (canonical path hash), projectName, rootPath, lastScannedAt | SQL `project_knowledge` |
| ProjectComponent | [NEW] | id, projectKnowledgeId, componentPath, componentType (BUNDLE/MODULE/PACKAGE/CLASS), summary, tags, criticality, sourceScanner (GENOME/ARCH_SCANNER) | SQL `project_components` |
| ArchitecturalDecision | [NEW] | id, projectKnowledgeId, decisionId (ADR id), title, context, decision, rationale, status (PROPOSED/ACCEPTED/SUPERSEDED/REJECTED), supersededBy, createdAt, decidedAt | SQL `architectural_decisions` |
| KnownIssue | [NEW] | id, projectKnowledgeId, title, description, severity, status (OPEN/RESOLVED/MITIGATED), workaround, relatedComponentIds | SQL `known_issues` |
| DevelopmentTask | [EXT] of EMF `Task`? | NO — new light type: id, projectKnowledgeId, summary, status, outcome, sessionRef | SQL `development_tasks` |
| ImplementationHistory | [NEW] | id, projectKnowledgeId, changeSummary, fileCount, commitRef, sessionId, timestamp | SQL `implementation_history` |
| RepositorySnapshot | [NEW] | id, projectKnowledgeId, snapshotRef (genome milestone id or TargetSnapshot id), rootPath, fileCount, hashInventoryRef, createdAt | SQL `repository_snapshots` |
| BuildResult | [NEW] | id, projectKnowledgeId, command, success, durationMs, summary, logRef, sessionId, timestamp | SQL `build_results` |
| TestResult | [NEW] | id, projectKnowledgeId, suite, run, failures, errors, skipped, passRate, failingTestsJson, buildResultId | SQL `test_results` |

Producers: `MilestoneGenerator` (components, snapshots), `RepoArchitectureScanner` (components/relationships), `Evaluator` (build/test rows), mediation `TargetRealityModel` (decisions/issues). Existing outputs remain unchanged; the service reads/imports them.

## 5. Context

| Entity | Kind | Fields | Home |
|---|---|---|---|
| ContextRequest | [NEW] | applicationId, userId, projectRef, sessionId, taskId, intent, budgetTokens, categories[], timeWindow, accessScope | MCP request DTO (not persisted; audit row optional) |
| ContextPackage | [EXT] of `orchestration/ContextPackage` | existing (goal, step, architectureContext, scope, code, dependencies, constraints, attachmentContext, memoryContext, attempt, lastFeedback) + **contextId, schemaVersion, userContext, sessionContext, projectContext, decisions[], knownIssues[], procedures[], continuity, sources[], confidence, freshness, tokenEstimate, warnings[], missingInfo[]** | in-memory response DTO; snapshots optionally JSON |
| ContextSource | [NEW] | sourceType (USER/SESSION/PROJECT/MEMORY/DECISION/REPOSITORY), sourceId, contribution, confidence, freshness | embedded in ContextPackage.sources[] |
| ContextItem | [NEW] | id, category, content, sourceRef, confidence, freshness, tokens | embedded items |
| ContextAssessment | [NEW] | contextId, quality (0..1), completeness, conflicts[], staleItems[], budgetUtilization, warnings[] | embedded in ContextPackage + audit row |
| ContextSnapshot | [NEW] | id, contextId, schemaVersion, packageJson, requestedBy, createdAt | SQL `context_snapshots` + JSON |

## 6. Relationships (cross-domain)

```
UserIdentity 1─n UserProfile / UserPreference / ApplicationAccess
ApplicationIdentity n─m UserIdentity (via ApplicationAccess.scopes)
CognitiveSession n─1 UserIdentity; n─1 ApplicationIdentity; n─1 ProjectKnowledge (optional); n─m CognitiveSession (SessionRelationship)
CognitiveSession 1─n SessionSnapshot; 1─n MemoryEntry (scope=SESSION, associatedId=sessionId)
MemoryEntry n─m MemoryEntry (MemoryRelationship: SUPERSEDES/CONTRADICTS/SUPPORTS/DERIVES_FROM)
ProjectKnowledge 1─n ProjectComponent / ArchitecturalDecision / KnownIssue / BuildResult / TestResult / RepositorySnapshot / ImplementationHistory
ProjectComponent n─m ProjectComponent (derived from RepoArchitectureScanner RelationshipRecord)
ContextPackage 1─n ContextItem; 1 ContextAssessment; n─1 CognitiveSession; n─1 ProjectKnowledge
```

## 7. Diagram (domain relationship)

```text
┌─────────────┐        ┌──────────────────────┐
│ UserIdentity│1─────n │UserProfile           │
│ (extends    │        │UserPreference        │
│  User)      │        └──────────────────────┘
└──────┬──────┘
       │1                    ApplicationAccess (scopes)
       ├──────────── n ───────┐
       │n                     │n
┌──────┴───────┐        ┌─────┴──────────────┐
│Application   │        │ApplicationIdentity │  EVO / TRYST / DSTUDIO / DCODE / MCP client
└──────┬───────┘        └────────────────────┘
       │1
       │n
┌──────▼──────────────────────────────┐
│CognitiveSession (cognitive_sessions)│◄── SessionRelationship (CONTINUES/SPAWNED)
└───┬───────────┬────────────┬────────┘
    │1          │1           │1
    │n          │n           │n
SessionSnapshot MemoryEntry  ContextPackage ── ContextItem / ContextAssessment
                 (scopes USER/            │
                  PROJECT/SESSION/TASK)   │
                 relationships: SUPERSEDES│
                 CONTRADICTS / SUPPORTS   │
┌────────────────▼───────────────────────▼──┐
│ProjectKnowledge (project_*)                │
│ ProjectComponent / ArchitecturalDecision   │
│ KnownIssue / BuildResult / TestResult      │
│ RepositorySnapshot / ImplementationHistory │
└────────────────────────────────────────────┘
```

## 8. Versioning and schema policy

- ContextPackage carries `schemaVersion` (semver) — first version `1.0`; MCP tools return the schema version in `initialize` capabilities metadata.
- Memory entries carry `version`; every mutation appends a `MemoryVersion` record (JSONL) — satisfies "auditable memory operations".
- SQL migrations follow the existing `DatabaseManager` additive ALTER pattern (servers bundle) — no ORM, no schema framework replacement.
