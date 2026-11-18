# MCP API SPECIFICATION — EVO COGNITIVE SERVICES

Extension of the **existing** EVO MCP server (`servers/mcp/*` implementation, port **28280** via `PortRegistry.EVO_MCP`). Protocol facts verified: JSON-RPC 2.0 over HTTP POST, protocolVersion `2024-11-05`, tool/resource/prompt registries, dispatcher authz hooks (`AuthorizationService`). This spec adds tools/resources and hardens authentication — it does not change transport or protocol version.

## 1. Transport and lifecycle

- Endpoint: `POST http://localhost:28280/mcp` (existing `McpDemoServerManager`/`DemoDocumentationProvider` path; `McpServer.serve` when the servers-bundle path is activated).
- `initialize` unchanged: `protocolVersion: "2024-11-05"`, serverInfo `EvoMcpServer` 1.0.0 → version bump to `1.1.0` when cognitive tools land; capabilities add `tools.listChanged: true` (already), and metadata `cognitive.schemaVersion: "1.0"`.
- Authentication: `Authorization: Bearer <token>` REQUIRED for cognitive tools (see SECURITY-AND-AUTHORIZATION.md). Missing/invalid token → JSON-RPC error `-32001` (`UNAUTHORIZED`). Session-less system loopback (SWT runtime) keeps existing bypass behavior only for non-cognitive tool categories.

## 2. Tool taxonomy

Division between tools and resources: **tools = operations with arguments and side effects; resources = stable, addressable, read-mostly data (history, profiles, states).** Memory/decisions are tools (write operations); session history, profiles, project state are resources.

### 2.1 Context

| Tool | Params | Returns | Notes |
|---|---|---|---|
| `context_get` | `{applicationId, userId?, project?, sessionId?, taskId?, intent, budgetTokens?, categories[]?, timeWindow?, accessScope}` | ContextPackage (see 2.5) | Principal (user/app) comes from the authenticated token, NOT from params — params are hints only |
| `context_search` | `{query, scope?, project?, types[]?, limit?}` | `[{entryId, type, content, confidence, relevance, source}]` | memory + decisions + conversations |
| `context_record` | `{sessionId?, summary, metadata?}` | `{contextId}` | stores a ContextSnapshot for replay/audit |

### 2.2 User

| Tool | Params | Returns |
|---|---|---|
| `user_get` | `{}` or `{userId?}` (self only unless scope user.read.any) | profile + preferences (scope-filtered) |
| `user_preferences_get` | `{scope?, project?}` | `[{key, value, source, confidence}]` |
| `user_preferences_update` | `{scope, project?, key, value, overwriteExplicit?}` | `{accepted, effective, conflicts?}` — explicit writes always win; inferred writes rejected if an explicit value exists (overwriteExplicit=false) |

### 2.3 Session

| Tool | Params | Returns |
|---|---|---|
| `session_create` | `{kind, project?, metadata?}` | `{sessionId, kind, status}` |
| `session_get` | `{sessionId}` | session descriptor + relationships + latest snapshot ref |
| `session_search` | `{query?, owner?, project?, kind?, from?, to?}` | `[sessionDescriptor]` |
| `session_snapshot` | `{sessionId, kind?}` (KERNEL/CONTEXT/FULL) | `{snapshotId, ref}` — kernel snapshots delegate to the existing `IterationMemoryService` checkpoint |
| `session_restore` | `{sessionId, snapshotId?}` | `{restored, restoredAt, missing[]}` |

### 2.4 Memory

| Tool | Params | Returns |
|---|---|---|
| `memory_store` | `{content, type?, scope?, project?, importance?, confidence?, source?, associatedId?, relationships?[]}` | `{entryId, version}` |
| `memory_search` | `{query?, type?, scope?, project?, since?, minConfidence?, limit?}` | `[MemoryEntry]` (with confidence/freshness) |
| `memory_update` | `{entryId, content?, confidence?, importance?, validity?}` | `{entryId, version}` — creates a new version + MemoryVersion record |
| `memory_retract` | `{entryId, reason?}` | `{entryId, validity: RETRACTED}` — auditable, never hard-delete (hard delete only via admin API) |

### 2.5 Decisions (project knowledge)

| Tool | Params | Returns |
|---|---|---|
| `decision_record` | `{project, decisionId?, title, context, decision, rationale, status?}` | `{decisionId}` |
| `decision_search` | `{project, query?, status?}` | `[ArchitecturalDecision]` |
| `decision_get` | `{project, decisionId}` | decision + supersession chain |

### 2.6 Project

| Tool | Params | Returns |
|---|---|---|
| `project_context_get` | `{project}` | components, decisions (active), known issues, last build/test summary, snapshot refs |
| `project_state_get` | `{project}` | `{state: {lastScanAt, snapshotRef, openIssues, lastBuild, lastTest}}` |
| `project_state_update` | `{project, key, value}` (restricted keys) | `{accepted}` |
| `project_knowledge_search` | `{project, query, kinds[]?}` | ranked components/decisions/issues |

## 3. Resources

| URI | Content | Backing |
|---|---|---|
| `evo://users/{userId}/profile` | UserProfile JSON | `user_profiles` |
| `evo://users/{userId}/preferences` | preferences list (scope-filtered by caller) | `user_preferences` |
| `evo://sessions/{sessionId}` | session descriptor JSON | `cognitive_sessions` |
| `evo://sessions/{sessionId}/history` | conversation transcript (paginated `?from&limit`) | `conversation_messages` / JSONL |
| `evo://projects/{project}/architecture` | components graph summary (from `RepoArchitectureScanner`/genome) | `project_components` |
| `evo://projects/{project}/decisions` | ADR list | `architectural_decisions` |
| `evo://projects/{project}/state` | project state package | `project_knowledge` + latest build/test |
| `evo://memory/{memoryId}` | single MemoryEntry (versioned) | MemoryService |

Resource authz: `resources/read` currently authorizes `("RESOURCE","*","READ")`. New rule: cognitive resources check per-URI scope — `RESOURCE:cognitive.evo://<root>` with caller scopes (see SECURITY-AND-AUTHORIZATION.md). Prompts: add the same authz gate to `prompts/list|get` in `JsonRpcDispatcher` (currently unprotected).

## 4. ContextRequest contract (stable)

```json
{
  "schemaVersion": "1.0",
  "applicationId": "dstudio",           // hint; validated against token
  "userId": "alice",                    // hint; must match token principal or be omitted
  "project": "/home/petr/git/evolution-121126",
  "sessionId": "uuid",                  // optional; continuation
  "taskId": "task-abc",                 // optional
  "intent": "extend MCP server with memory tools",
  "budgetTokens": 12000,
  "categories": ["user","session","project","memory","decisions","issues","procedures","continuity"],
  "timeWindow": {"from": "2026-11-01", "to": "2026-11-17"},
  "accessScope": ["PROJECT", "SESSION"]
}
```

## 5. ContextPackage response contract

```json
{
  "contextId": "ctx-uuid",
  "schemaVersion": "1.0",
  "userContext":      {"identity": {...}, "preferences": [...], "explicit": true},
  "sessionContext":   {"sessionId": "...", "kind": "CONVERSATION", "summary": "...",
                       "lastMessages": [...], "relatedSessions": [...]},
  "projectContext":   {"project": "...", "components": [...], "state": {...}},
  "memories":         [{"entryId", "type", "content", "confidence", "freshness", "source"}],
  "decisions":        [{"decisionId", "title", "decision", "status"}],
  "knownIssues":      [{"title", "severity", "workaround"}],
  "procedures":       [{"entryId", "content", "reliability"}],
  "continuity":       {"resumedFrom": "snapshotId?", "gaps": []},
  "sources":          [{"sourceType", "sourceId", "contribution", "confidence", "freshness"}],
  "confidence": 0.82,
  "freshness": "2026-11-17T10:00:00Z",
  "tokenEstimate": 9400,
  "warnings":         ["memory entry MEM-42 superseded by MEM-77 (included)"],
  "missingInfo":      ["no build results recorded for project"]
}
```

## 6. Error envelope (existing `McpError`)

- `-32602` invalid params; `-32601` unknown method; `-32000` cancelled/timeout; `-32603` internal; `-32001` unauthorized (existing); NEW `-32002` `PERMISSION_DENIED` (scope insufficient); `-32003` `CONFLICT` (e.g. preference contradiction requiring explicit resolution); `-32004` `NOT_FOUND`.

## 7. Compatibility and sequencing

- Phase 1 tools: `context_get`, `context_search`, `memory_store/search`, `user_get`, `user_preferences_get/update`, `session_create/get/snapshot`, `decision_record/search`, `project_context_get`, `project_knowledge_search`.
- Phase 2 adds: `session_restore`, `memory_update/retract`, `context_record`, `project_state_*`, `session_search`.
- All tools are thin adapters over the native services in `PLATFORM-SERVICE-ARCHITECTURE.md` — single implementation, two interfaces (OSGi + MCP).
- Example call/response pairs live in the test strategy (TEST-AND-VALIDATION-STRATEGY.md §MCP).
