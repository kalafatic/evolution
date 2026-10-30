# Self-Development Loop Architecture & Execution Graph

**Date:** October 30, 2026
**Document ID:** `genome-docs/30102026/self-development-architecture.md`

---

## 1. Overview & Subsystem Boundaries

The Self-Development subsystem enables EVO to autonomously acquire its canonical source code, construct an isolated build workspace, execute Maven/Tycho product builds, export RCP application artifacts, start and monitor an independent external EVO runtime, verify runtime readiness via REST/OSGi, and execute project/inference workflows.

---

## 2. Component Responsibilities

| Component | Responsibility |
| :--- | :--- |
| `SelfDevOrchestrator` | Coordinates the 18-stage task execution graph and task context lifecycle. |
| `SelfDevSupervisor` | External, independent process supervision and monitoring loop on port 8089/28080. |
| `GitCheckTask` / `GitManager` | Canonical Git repository verification and isolated workspace cloning. |
| `CopySourceTask` | Copies canonical source to isolated working directory (`<workspace>/self-dev-run/`). |
| `BuildEvoTask` / `TychoEvoRcpBuilder` | Invocations of `MavenBuildExecutor` executing `./mvnw clean package -P export-product`. |
| `ExportEvoTask` | Discovery and verification of exported Eclipse RCP ZIP/JAR artifacts. |
| `StartEvoTask` / `EvoRcpRuntime` | Deployment and startup of external EVO RCP process. |
| `EvoRcpVerifier` | Polling `/server/status`, `/server/osgi`, and `evo_ready.txt` marker for runtime readiness. |
| `CreateEvoProjectTask` | HTTP POST `/server/project/create` to initialize active `EvoProject`. |
| `CreateInferenceTask` | HTTP POST `/task` for submitting asynchronous inference tasks. |

---

## 3. Full 18-Stage Execution Graph

```
[1. START_EVO] → [2. GitCheckTask] → [3. CopySourceTask] → [4. BuildEvoTask] → [5. ExportEvoTask]
  → [6. StartEvoTask] → [7. EvoRcpVerifier] → [8. CreateEvoProjectTask] → [9. CreateInferenceTask]
  → [10. Poll Status] → [11. Result Verification] → [12. Analytic Validation]
  → [13. Collect Diagnostics] → [14. Git Commit/Tag] → [15. SelfDevSupervisor]
  → [16. Failure Recovery] → [17. Auto Rollback] → [18. Complete Run]
```
