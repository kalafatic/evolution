# SESSION CONTINUITY DESIGN

Cross-session and cross-application continuity on top of the existing session architecture. **Constraints**: `SessionManager` stays the in-process runtime registry; `IterationManager` stays the sole authority for Darwin iteration transitions; checkpoints (`orchestrator/memory/checkpoint_<sessionId>.json`) stay the kernel-state snapshot format.

## 1. Identity of a session

- `CognitiveSession.id` = UUID v4 issued by `SessionContinuityService.beginSession` (new `SessionIdService` utility). Legacy ad-hoc ids (caller-supplied, Orchestrator.id, random) are accepted on `resolve` and aliased (legacyId → cognitive id) so nothing breaks.
- Attributes: kind (USER/APPLICATION/CONVERSATION/TASK), ownerUserId, applicationId, projectRef, parentSessionId, status, timestamps, metadata.
- Persistence: SQLite `cognitive_sessions` + `session_relationships` (see COGNITIVE-DOMAIN-MODEL.md).

## 2. Layered snapshots (what "restore" means per layer)

| Layer | Snapshot | Producer | Restore |
|---|---|---|---|
| Conversation | transcript + summary | ConversationHistoryService (JSONL) | replay messages; compact summary into next context |
| Context | ContextPackage | ContextReconstructionService (`context_record`) | reuse as prior-context seed; mark `continuity.resumedFrom` |
| Darwin kernel | checkpoint JSON | EXISTING `IterationManager.saveFullCheckpoint` / `IterationMemoryService` | EXISTING `loadCheckpoint` + `restoreStateFromCheckpoint` (RESTART CONTINUITY block) — reused verbatim |
| UI/selection | out of scope Phase 1 | — | — |
| Full | metadata manifest referencing the above | SessionContinuityService | orchestrates the three restores |

Rule: `session_snapshot(kind=FULL)` stores a manifest `{conversationRef, contextRef, checkpointRef, schemaVersion, createdAt}` — it never duplicates kernel state (avoids the existing checkpoint drift problem).

## 3. Lifecycle integration points (existing code, additive)

- **Creation**: `OrchestratorServiceImpl.handle()` — one call `SessionContinuityService.resolve(sessionId, ownerFromRequest, kind)` before `SessionManager.getOrCreateSession`. Existing behavior unchanged when service absent.
- **Termination**: `SessionManager.shutdownSession` gains a hook (called by `SessionContinuityService.finishSession`) to mark status COMPLETED/FAILED and trigger post-session consolidation.
- **Restart reconciliation**: on EVO startup, `cognitive_sessions` rows with status ACTIVE/PAUSED are reconciled: kernel-checkpoint-present → PAUSED (resumable); else FAILED (mirrors DCode `SessionRecoveryService` and DStudio `reconcileAfterRestart` semantics). No automatic re-run (existing platform policy: never silently resume execution).
- **Resume flow**: new request with `sessionId` → `KernelFacade` path already re-creates IterationManager; add a pre-step: if a checkpoint exists, `SessionContinuityService` loads it (existing `IterationManager` constructor does this anyway) and `ConversationHistoryService` supplies the last N messages + summary into context.

## 4. Session relationships

- `session_relationships` kinds: CONTINUES (follow-up conversation), SPAWNED (task from conversation), SUBSUMES, RELATED (same project/task).
- Produced when: `beginSession(parentSessionId)` (CONTINUES), Darwin task execution from a conversation (SPAWNED), DCode/TRYST/DStudio delegate with a correlation id (RELATED, via `session_create` metadata `correlationId`).
- Queryable via `session_search` and rendered in ContextPackage.sessionContext.relatedSessions.

## 5. Cross-application continuity matrix

| App | Session concept today | Integration |
|---|---|---|
| EVO | Darwin session (SessionManager) | native: SessionContinuityService calls in OrchestratorServiceImpl |
| TRYST | `Session` + `AgentTask` (SQLite V1) | MCP: `session_create(metadata={correlationId: <tryst session id>})`; store `evo_cognitive_session_id` on the tryst session row (new column/V21, mirrors `evo_executions.evo_session_id` V13 precedent) |
| DStudio | `executions` (SQLite V1) | MCP: same correlation pattern; new `external_session_id` column (V3 migration) |
| DCode | harness `sessions` (SQLite) | MCP via EVO→DCode adapter or direct client; DCode `Session.metadata` fixed round-trip; correlation id in metadata |

Scope isolation still applies: apps only see their own sessions unless the token carries `session.read.any` (see SECURITY-AND-AUTHORIZATION.md).

## 6. Sequence diagram (continuity restore)

```text
Client ── tools/call session_restore {sessionId} ──► MCP dispatcher (authn+authz)
   ──► SessionContinuityService.restore(sessionId)
         ├─ read cognitive_sessions row (status PAUSED?)
         ├─ manifest = latest FULL snapshot
         ├─ conversation: ConversationHistoryService.list(from checkpoint seq)
         ├─ context: load ContextSnapshot JSON
         ├─ kernel: IterationMemoryService.loadCheckpoint(sessionId)
         │      └─ (next request path) IterationManager.constructor RESTART CONTINUITY
         └─ return {restored: true, restoredAt, missing: [...]}
Client ── tools/call context_get {sessionId} ──► ContextPackage.continuity.resumedFrom
```

## 7. Acceptance criteria

1. Session survives EVO restart: `session_get` after restart returns PAUSED/COMPLETED with full history (not FAILED when checkpoint present).
2. `session_snapshot` + `session_restore` round-trip restores conversation context into the next `context_get` (verified by test).
3. Darwin kernel checkpoint round-trip unchanged (existing `CheckpointDeterminismTest` stays green).
4. TRYST task and EVO session correlate via `session_relationships` (RELATED) after delegation.
5. No second session manager introduced: grep-able invariants — only `SessionManager` owns runtime session map; only `IterationManager` transitions iteration state.
