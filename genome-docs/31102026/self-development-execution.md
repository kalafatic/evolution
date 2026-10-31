# Self-Development Execution & Lifecycle Architecture

**Date:** October 31, 2026
**Document ID:** `genome-docs/31102026/self-development-execution.md`

---

## 1. Overview & 18-Stage Task Execution Graph

The EVO Self-Development Subsystem provides autonomous capability acquisition through an integrated task graph executed by `SelfDevOrchestrator` under external `SelfDevSupervisor` supervision:

```
[1. START_EVO] → [2. GitCheckTask] → [3. CopySourceTask] → [4. BuildEvoTask] → [5. ExportEvoTask]
  → [6. StartEvoTask] → [7. EvoRcpVerifier] → [8. CreateEvoProjectTask] → [9. CreateInferenceTask]
  → [10. Poll Status] → [11. Result Verification] → [12. Analytic Validation]
  → [13. Collect Diagnostics] → [14. Git Commit/Tag] → [15. SelfDevSupervisor]
  → [16. Failure Recovery] → [17. Auto Rollback] → [18. Complete Run]
```

---

## 2. Supervisor Independence Invariant

`SelfDevSupervisor` operates as a separate headless daemon (`EVOSupervisorServer` on port 8089 / control port 28080). It tracks target process PIDs, validates product layout (`EvoProductValidator`), monitors readiness markers (`evo_ready.txt`), and detects target application crashes or unexpected exits independently of target process lifetime.
