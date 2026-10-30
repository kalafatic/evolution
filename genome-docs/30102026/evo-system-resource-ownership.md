# EVO System Resource Ownership & Concurrency Model

**Date:** October 30, 2026
**Document ID:** `genome-docs/30102026/evo-system-resource-ownership.md`

---

## 1. Resource Ownership Matrix

| Resource Type | Owner | Lifecycle & Disposal Policy |
| :--- | :--- | :--- |
| **ExecutorServices** | `SessionContainer` / `OrchestratorServiceImpl` | Closed during session shutdown (`SessionContainer.shutdown()`). |
| **Git Worktrees** | `ADarwinEngine` / `GitManager` | Removed via `removeWorktree` and `pruneWorktrees` post-evaluation. |
| **Log Streams** | `SessionLogger` | Flushed and closed on session disposal; written to `session_<id>.log`. |
| **SQLite Connections** | `DatabaseManager` | Closed upon server shutdown (`stopServer()`). |
| **HTTP Servers** | `EvolutionServer` / `EVOSupervisorServer` | Stopped during `stopServer()` / `SupervisorManager.stop()`. |

---

## 2. Concurrency Isolation

- Parallel trajectory variant evaluations run in isolated Git worktree directories (`evo-variant-<id>`), preventing filesystem race conditions.
- `DarwinEngineRegistry` and `SessionManager` maintain thread-safe `ConcurrentHashMap` registries.
