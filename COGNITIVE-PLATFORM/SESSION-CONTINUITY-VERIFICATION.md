# SESSION CONTINUITY VERIFICATION
Durable sessions, conversation history, snapshots and restart behavior.

## 1. Conversation history (live + unit)

- Message, tool-call, tool-result, decision and error records persist
  (JSONL transcript + SQLite index + FTS5).
- History survives service restart (ConversationHistoryServiceTest
  `historySurvivesServiceRestart` PASS).
- FTS search finds content; LIKE fallback verified.
- Compaction produces bounded summaries without truncating the log.
- Live: conversation_history_append/get round-trip over MCP PASS.

## 2. Session registry (live + unit)

- beginSession issues canonical UUIDs; legacy aliases resolve to the
  canonical session.
- Session → application/user binding recorded; relationships (CONTINUES)
  recorded on parent linkage.
- Live: session_create/session_get/session_search over MCP PASS.
- Restart: sessions persisted across EVO restart (4 sessions found after
  restart in the live check).

## 3. Snapshots and restore

- FULL snapshot manifest references conversation + context + kernel layers;
  kernel checkpoints are never duplicated.
- getLatestSnapshot merges the manifest layers for restore.
- session_restore returns restored layers + missing list (live PASS:
  conversationRef restored, missing: kernel checkpoint + context snapshot).
- Unit: snapshot/restore round-trip, finishSession status transitions,
  reconcileAfterRestart marks checkpoint-less ACTIVE sessions FAILED
  (SessionContinuityServiceTest 6/6 PASS).

## 4. Architecture invariants

- Runtime session registry remains SessionManager (no second manager).
- IterationManager remains the only iteration-transition authority
  (verified by code inspection; no new transition code was added).
- Kernel checkpoints remain the Darwin state snapshot format; manifests
  reference them by id.

## 5. Isolation

- Cross-application session reads denied with -32002 (live E2E negatives:
  DStudio→TRYST session and conversation, TRYST→DStudio session).
- Fix applied: session ownership now requires application+user match
  (user match alone was insufficient — defect found by the live E2E).
