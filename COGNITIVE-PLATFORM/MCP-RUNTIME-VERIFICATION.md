# MCP RUNTIME VERIFICATION
Live verification of the EVO MCP server (port 28280), executed against the
running EVO RCP product.

## 1. Environment

- EVO started through its existing RCP product and lifecycle
  (`target/products/evolution/linux/gtk/x86_64/evolution/evo`, DISPLAY=:1).
- Startup log confirms: `[MCP 28280] Cognitive Services wired: authentication
  + authorization + tools + resources`, cognitive tools/resources registered.
- Port 28280 listener verified (`ss -tln`); primary 28080 conflict handled by
  the existing PortAvailabilityChecker (logged, non-fatal, deterministic
  ports preserved).
- Driver: Python HTTP client posting JSON-RPC 2.0 to `/mcp` (same wire
  protocol as the TRYST/DStudio EvoMcpClient implementations).

## 2. Protocol

- `initialize` → protocolVersion `2024-11-05`, serverInfo `EvoMcpServer 1.1.0`. PASS

## 3. Authentication matrix (live)

| Case | Result |
|---|---|
| Missing bearer token (context_get) | -32001 UNAUTHORIZED |
| Invalid token | -32001 |
| Revoked token | -32001 |
| Valid application token | tool executes |
| Valid token missing scope (decision_record without decision.write) | -32002 PERMISSION_DENIED |
| Valid token missing scope (user_preferences_write) | -32002 |
| Anonymous tools/list | cognitive tools hidden (5 legacy tools visible) |
| Authenticated tools/list | 29 tools incl. all cognitive tools |

## 4. Tools executed live (41/41 checks)

- context_get: contextId, schemaVersion 1.0, measured tokenEstimate ≤ budget,
  assessment attached.
- memory_store → version 1; memory_update → version 2; memory_retract →
  RETRACTED; memory_search excludes retracted records.
- session_create → UUID; session_get → descriptor + relationships;
  session_snapshot → snapshotId; session_restore → restored layers.
- conversation_history_append (USER + TOOL_CALL) / conversation_history_get.
- decision_record / decision_search / decision_get; project_context_get /
  project_state_get / project_state_update / project_knowledge_search.
- user_get / user_preferences_update / user_preferences_get.
- context_record → contextId.
- Invalid input: memory_store without content → -32602;
  session_get without sessionId → -32602.

## 5. Resources (live)

- `evo://memory/entry?entryId=...` → entry JSON (correct mimeType/text).
- `evo://projects/state?project=...` → state package.
- `evo://sessions/history?sessionId=...` → transcript.
- Missing entry → clean error envelope.

## 6. Auditing (live)

audit_log after the verification runs: 393 rows — AUTHORIZE 205,
CALL_TOOL SUCCESS 142, DENIED 22, PERMISSION_DENIED 9, INVALID_ARGUMENTS 8,
ERROR 4, NOT_FOUND 3. Token values never appear in logs.

## 7. Defects found and fixed during runtime verification

1. Tool results containing org.json values caused HTTP 500 (Jackson
   serialization) → `CognitiveMcpToolProvider.toSerializable` deep
   conversion; regression test added.
2. Resource lookup failed for URIs with query params →
   `ResourceRegistry.readResource` base-URI fallback; regression test added.
3. `searchDecisions` SQL used an ambiguous `id` column and later three-valued
   NULL LIKE logic → qualified columns + COALESCE + token OR-semantics;
   regression tests added.
4. `SecurityException` from tool isolation checks surfaced as -32603 →
   mapped to -32002 PERMISSION_DENIED in the dispatcher.

All fixes verified live: the full 41-check matrix passes after redeployment.
