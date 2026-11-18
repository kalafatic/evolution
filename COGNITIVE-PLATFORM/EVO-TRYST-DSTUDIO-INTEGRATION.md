# EVO, TRYST AND DSTUDIO INTEGRATION

How each EVO-family application consumes shared cognitive services via the EVO MCP server (wire-only, no EVO-internal class dependencies).

## 1. Integration contract summary

| Aspect | EVO | TRYST | DStudio | DCode |
|---|---|---|---|---|
| Authentication | native (AuthService) | EVO-issued application token (Bearer) | EVO-issued application token | EVO-issued application token |
| Application id | `evo` | `tryst` | `dstudio` | `dcode` |
| Session identity | native `CognitiveSession` + SessionManager | `Session`/`AgentTask` + `evo_cognitive_session_id` column (new migration) | `executions` + `external_session_id` column (new migration V3) | `sessions` + metadata correlation |
| Project identity | `projectRef` (canonical path) | sandbox/repo path | workspace path | workspace path |
| Context request | native service call | MCP `context_get` (merged in `AiContextManager`) | MCP `context_get` (new assembly point in `DefaultExecutionService.runTask`) | MCP or ingest endpoint (per DCode-ENR doc) |
| Recording | native (events→consolidation) | MCP `context_record`/`memory_store` on task completion | same | same (optional) |
| Snapshot | native `session_snapshot` | `session_snapshot` at task boundaries | `session_snapshot` at execution end | — |
| Restore | native `session_restore` | read session + history resource on task resume | — (DStudio fail-fast policy kept in Phase 1) | — |

## 2. TRYST changes (in `eu.kalafatic.tryst.evo.integration`)

1. **MCP client** `EvoMcpClient` (new class, mirrors `TeipClient` patterns: Java 21 HttpClient, bearer header, retry, correlation ids). Config: reuse `ConnectedApplicationRegistry` (V17) or new `evo_mcp_configurations` migration — recommend extending `evo_configurations` (V13) with `mcpEndpoint`/`mcpToken` (additive ALTER).
2. **Context merge point**: `domain/ai/AiContextManager.assembleConversationContext` gains an external source hook: when EVO MCP configured, call `context_get` (categories: user/project/memory/continuity) and prepend the result as a bounded system section (respecting the existing 4096 budget by shrinking local history first).
3. **Correlation**: on task start, `session_create(kind=TASK, correlationId=<tryst session id>)`; store returned id in new column `evo_cognitive_session_id` on `sessions` (mirrors V13 `evo_executions.evo_session_id` precedent).
4. **Recording**: on task completion (success/failure), `context_record` + `memory_store` (episode, source=TOOL/outcome) — async post-completion, never blocks the pipeline.
5. **Preferences**: TRYST `notification_preferences` (V10) stay TRYST-local; user-level cognitive preferences live in EVO and are read via `user_preferences_get` when building instructions.

## 3. DStudio changes

1. **MCP client** in `eu.kalafatic.dstudio.teip` (new small subsystem: `EvoMcpClient`, wire models for ContextPackage). Config in `DStudioConfiguration` (endpoint/token props).
2. **Identity**: Phase 1 = application token (app-level identity; `Caller.operator` string passed as metadata hint). Per-user identity in DStudio requires the EVO user concept inside DStudio — deferred (see risks); Phase 1 keeps the single-token model and treats the operator as the EVO user when `dstudio.evo.user` config is set.
3. **Context assembly point**: `DefaultExecutionService.runTask` builds the instruction string today; add optional pre-step: `context_get` (project/memory/session categories) → append section before harness dispatch.
4. **Correlation**: `execution_idempotency`-style new column `external_session_id` on `executions` (migration V3) + `session_create` at submit.
5. **Continuity**: Phase 1 keeps `reconcileAfterRestart` fail-fast semantics; optionally reads `evo://sessions/{id}/history` for diagnostics. Resumption via EVO snapshots is Phase 2 (requires DStudio execution-resume work beyond cognitive scope).

## 4. EVO changes (native + MCP adapter)

- Native: services per PLATFORM-SERVICE-ARCHITECTURE.md; `OrchestratorServiceImpl`/`ContextBuilder` additive hooks.
- MCP: `CognitiveMcpToolProvider` + authenticator wiring on 28280; TEIP 1.1 unchanged.
- Darwin: `context_get` before engine orchestration in `KernelFacade.handle` (additive; empty store ⇒ identical behavior).

## 5. Data flow (TRYST example)

```text
TRYST TaskWorker (task start)
  │ session_create(kind=TASK, correlationId=trystSessionId, project=sandboxPath)
  │        → EVO cognitive_sessions row
  │ context_get {categories:[project,memory,continuity], budgetTokens:3500}
  │        → ContextPackage → AiContextManager prepends section
  ├─► DCode/ENR execution (unchanged)
  │ (task end)
  │ context_record {summary, outcome}
  │ memory_store {type:EPISODE, scope:PROJECT, source:TOOL, importance: outcome-based}
  │ session_snapshot {sessionId, kind:CONTEXT}
```

## 6. Isolation across apps (reminder)

- TRYST token scopes: `context.get`, `memory.read/write`, `session.create/read/write`, `project.read`, `decision.read` — its own sessions only.
- DStudio token scopes: same minus `decision.write` initially.
- EVO internal: SYSTEM bypass (existing).
- All cross-app reads beyond own scope require `*.any` scopes (admin only).

## 7. Acceptance criteria

1. TRYST task run with EVO MCP configured produces an EVO session + episode memory entry (end-to-end test, mock MCP or live EVO).
2. DStudio execution calls `context_get` and the section appears in the harness instruction (integration test with live EVO MCP).
3. Correlation ids survive restart in both apps (migration + repository round-trip tests, mirroring existing `DCodeConfigurationRepositoryTest` style).
4. With EVO unavailable: both apps degrade gracefully (existing fallback policies unchanged — TRYST local CodeAgent fallback, DStudio local execution).
5. No EVO internal classes referenced by TRYST/DStudio (checked by build + bundle-import inspection).
