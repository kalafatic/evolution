# MCP API REFERENCE
EVO Cognitive Services over the existing MCP server

## 1. Endpoint and protocol

- URL: `POST http://<host>:28280/mcp` (existing `PortRegistry.EVO_MCP`, default 28280)
- Protocol: JSON-RPC 2.0 over HTTP POST; `initialize` returns
  `protocolVersion: "2024-11-05"`, `serverInfo: EvoMcpServer 1.1.0`.
- Authentication: `Authorization: Bearer <token>` REQUIRED for all cognitive
  tools. Accepted tokens: application tokens (IdentityService) and EVO user
  session tokens. Missing/invalid token → error `-32001` (unauthorized);
  valid token without the required scope → `-32002` (permission denied).
- Identity params (`userId`, `applicationId`, `project`, `sessionId`) are HINTS.
  The token principal is authoritative; spoofed params never grant access.

## 2. Tools (all thin adapters over native services)

### Context
| Tool | Scope | Params | Result |
|---|---|---|---|
| context_get | context.get | userId?, applicationId?, project?, sessionId?, taskId?, intent?, budgetTokens?, categories[]?, timeWindow?, accessScope[] | ContextPackage (schemaVersion 1.0) |
| context_search | context.search | query?, project?, types[]?, limit? | {count, results} |
| context_record | context.record | sessionId?, summary?, metadata? | {contextId} |

### Memory
| Tool | Scope | Params | Result |
|---|---|---|---|
| memory_store | memory.write | content*, type?, scope?, project?, importance?, confidence?, source?, associatedId?, metadata?, relationships? | {entryId, version} |
| memory_search | memory.read | query?, type?, scope?, project?, minConfidence?, limit? | {count, results[]} |
| memory_update | memory.write | entryId*, content?, confidence?, importance?, validity? | {entryId, version} |
| memory_retract | memory.write | entryId*, reason? | {entryId, validity: RETRACTED} |

### User
| Tool | Scope | Params | Result |
|---|---|---|---|
| user_get | user.read | userId? | {profile} |
| user_preferences_get | user.preferences.read | userId?, scope?, project? | {preferences[]} |
| user_preferences_update | user.preferences.write | userId?, scope?, project?, key*, value*, overwriteExplicit? | {accepted, preference} |

### Sessions
| Tool | Scope | Params | Result |
|---|---|---|---|
| session_create | session.create | kind?, project?, correlationId?, metadata? | {sessionId, kind, status} |
| session_get | session.read | sessionId* | descriptor + relationships + latestSnapshot |
| session_search | session.read | project?, kind?, status?, limit? | {count, sessions[]} |
| session_snapshot | session.write | sessionId*, kind? (KERNEL/CONTEXT/FULL) | {snapshotId, ref} |
| session_restore | session.write | sessionId* | {restored, restoredAt, layers, missing[]} |

### Decisions and project knowledge
| Tool | Scope | Params | Result |
|---|---|---|---|
| decision_record | decision.write | project*, decisionId?, title*, context?, decision*, rationale?, status? | {decisionId, project, status} |
| decision_search | decision.read | project?, query?, status?, limit? | {count, decisions[]} |
| decision_get | decision.read | project*, decisionId* | decision + supersededBy |
| project_context_get | project.read | project* | components/decisions/issues/state |
| project_state_get | project.read | project* | state package |
| project_state_update | project.write | project*, key*, value* (restricted keys: build, implementation) | {accepted} |
| project_knowledge_search | project.read | project*, query?, kinds[]? | {components, decisions, issues} |

### Conversation history
| Tool | Scope | Params | Result |
|---|---|---|---|
| conversation_history_get | session.read | conversationId*, from?, limit? | {count, records[]} |
| conversation_history_append | session.write | conversationId*, role?, toolName?, content* | {sequence, recordId} |

## 3. Resources

| URI | Content |
|---|---|
| evo://sessions/history?sessionId=... | paginated conversation transcript |
| evo://memory/entry?entryId=... | single memory entry |
| evo://users/profile?userId=... | user profile |
| evo://projects/state?project=... | project state package |

Resource reads are authorized at the dispatcher (RESOURCE READ) like existing
resources; the providers resolve query parameters from the URI.

## 4. ContextPackage response contract (schemaVersion 1.0)

```json
{
  "contextId": "ctx-...", "schemaVersion": "1.0",
  "userContext": {"identity": {...}, "preferences": [...], "inferredPreferences": [...]},
  "sessionContext": {"id": "...", "kind": "...", "relatedSessions": [...]},
  "projectContext": {"project": "...", "lastBuild": {...}, "lastTest": {...}, "known": true},
  "memories": [{"id", "category", "content", "sourceRef", "confidence", "freshness", "tokens", "stale", "extra": {"memoryType", "informationStatus", "validity", "relevance"}}],
  "decisions": [...], "knownIssues": [...], "procedures": [...],
  "continuity": {"resumedFrom": "snapshotId?"},
  "sources": [{"sourceType", "sourceId", "contribution", "confidence", "freshness"}],
  "confidence": 0.82, "freshness": 176..., "tokenEstimate": 9400, "budgetTokens": 12000,
  "warnings": [...], "missingInfo": [...],
  "assessment": {"quality", "completeness", "conflicts", "staleItems", "budgetUtilization"}
}
```

## 5. Error envelope

| Code | Meaning |
|---|---|
| -32602 | invalid params |
| -32601 | unknown method / not found |
| -32603 | internal error |
| -32000 | cancelled / timeout |
| -32001 | unauthorized (missing/invalid token) |
| -32002 | permission denied (missing scope) |
| -32003 | conflict (reserved for preference contradictions) |
| -32004 | not found (reserved) |

## 6. Compatibility

- Demo documentation tools/resources on 28280 keep their legacy behavior
  (unauthenticated listing excludes protected cognitive tools).
- The 28080 UI dispatcher and its authoritative EVO tools are unchanged.
- TEIP 1.1 (28080) is unchanged; its static token model is separate by design
  (normative protocol).
