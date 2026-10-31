# Server Operation Persistence & Restart Recovery

**Date:** October 31, 2026
**Document ID:** `genome-docs/31102026/server-operation-persistence.md`

---

## 1. Persistence Model

- **Session Records**: Persisted in SQLite `sessions` table by `SessionRepository` and `DatabaseManager`.
- **Session Logs**: Written incrementally to `<logDir>/sessions/session_<sessionId>.log` by `SessionLogger`.
- **Restart Reconciliation**: During server startup, `DatabaseManager` discovers sessions recorded as `ACTIVE`. If the underlying worker process is no longer active, the status is reconciled to `TERMINATED` or `INTERRUPTED` to prevent stale `RUNNING` indicators.
