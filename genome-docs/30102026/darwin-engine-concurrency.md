# Darwin Engine Concurrency & Session Isolation Model

**Date:** October 30, 2026
**Document ID:** `genome-docs/30102026/darwin-engine-concurrency.md`

---

## 1. Concurrency Model & Instance Isolation

- **Concurrent Engine Registry**: `DarwinEngineRegistry` utilizes a thread-safe `ConcurrentHashMap` mapping `sessionId` to `ADarwinEngine` instances, enabling concurrent process executions without instance lookup locks.
- **Parallel Variant Materialization**: Variants are evaluated in parallel using `evaluationCandidates.parallelStream().forEach(...)`.
- **Git Worktree Isolation**: Each parallel candidate variant executes in a temporary, isolated Git worktree directory (`evo-variant-<id>`), ensuring file system modifications do not interfere across competing proposals or contaminate the main working tree.
- **Thread Approval Locks**: The `approvalLock` object monitors decision authority synchronization per engine instance without global thread blocking.
