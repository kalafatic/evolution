# Asynchronous Server Operation Lifecycle & Recovery Model

**Date:** October 30, 2026
**Document ID:** `genome-docs/30102026/server-operation-lifecycle.md`

---

## 1. Asynchronous Operation Model

To prevent HTTP client timeouts during long-running inference, coding, or self-development tasks:
1. **Immediate Acceptance**: Upon POST `/task` or REST submission, `OrchestratorServiceImpl` creates an operation record (`taskId`, `sessionId`), persists it in SQLite `sessions`, and returns HTTP 202 `ACCEPTED` with the tracking ID.
2. **Asynchronous Execution**: Execution runs in background `ExecutorService` worker threads, publishing real-time logs to `SessionLogger` (`session_<id>.log`).

---

## 2. Operation Lifecycle State Machine

```
CREATED → ACCEPTED → RUNNING → WAITING_FOR_INPUT → SUCCEEDED / FAILED / CANCELLED
```

- **Restart Recovery**: Upon server restart, `DatabaseManager` and `SessionRepository` load persisted sessions. Operations whose underlying worker process exited without terminal state transition are reconciled to `INTERRUPTED` or `FAILED`.
