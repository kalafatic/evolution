# EVO System Recovery, Reconciliation & Resilience

**Date:** October 30, 2026
**Document ID:** `genome-docs/30102026/evo-system-recovery.md`

---

## 1. System Failure Recovery Strategies

1. **Target Runtime Crash**:
   - `SelfDevSupervisor` detects process termination, records exit code and logs, marks run status as `RUNNING_FAILED`, and triggers automated Git rollback (`GitManager.rollback()`) to restore source working tree to last known good commit.
2. **Server Restart Reconciliation**:
   - `DatabaseManager` and `SessionRepository` load persisted HTTP sessions upon server startup. Unfinished operations whose background worker processes no longer exist are reconciled to `INTERRUPTED` or `FAILED`.
3. **Darwin Loop Iteration Failure**:
   - Bounded retries (up to 3 attempts) are executed before initiating Git rollback and marking the iteration as `FAILED`.
