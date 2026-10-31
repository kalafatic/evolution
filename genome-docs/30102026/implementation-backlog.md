# EVO Implementation Prioritized Backlog

**Date:** October 30, 2026
**Document ID:** `genome-docs/30102026/implementation-backlog.md`

---

## 1. Prioritized Development Backlog

| Task ID | Component | Files Involved | Current Verified Problem | Expected Functional Behavior | Priority |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **TSK-DARWIN-001** | Darwin Engine | `ADarwinEngine.java` | Contract stub methods (`getMode`, `executeWinner`, `evaluateFitness`, etc.) lacked concrete implementations. | Implemented contract methods binding context, trajectory memory, evaluator, and fitness engine. | **P0** (Completed) |
| **TSK-SELFDEV-002** | Self-Development | `CreateEvoProjectTask.java`, `CreateInferenceTask.java` | Task verification relied on HTTP 200 header without validating JSON response body. | Validates JSON payload (`status == OK` or non-empty output) before declaring task success. | **P1** (Completed) |
| **TSK-SERVER-003** | Server Platform | `EvolutionServer.java`, `AdminApiController.java` | Long-running operations lacked durable SQLite session status updates across restarts. | Session updates persisted via `SessionRepository.updateStatus` on lifecycle changes. | **P2** (Completed) |
| **TSK-UI-004** | UI / View | `MultiPageEditor.java`, `AEvoPage.java` | Out-of-order background events could overwrite newer snapshots. | Enforced per-session timestamp guards (`isStateStale()`) and observational page switching. | **P3** (Completed) |
