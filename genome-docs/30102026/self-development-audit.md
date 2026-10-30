# Self-Development Loop Architecture & Supervisor Audit

**Date:** October 30, 2026
**Document ID:** `genome-docs/30102026/self-development-audit.md`
**Scope:** Self-Development Workflow, Workspace Isolation, Supervisor Independence, Build & Export, Runtime Verification

---

## 1. Overview & Architectural Purpose

The Self-Development Loop (`SelfDevOrchestrator`, `SelfDevSupervisor`, `EvoRcpRuntime`) represents EVO's autonomous self-evolution pipeline. It allows EVO to acquire its own canonical Git source, isolate a build workspace, execute Maven/Tycho product compilation, export an Eclipse RCP product runtime, start an independent external Supervisor process, launch the exported EVO instance, verify runtime health via REST/OSGi, submit self-development inference tasks, and recover from runtime failures.

---

## 2. 18-Step Self-Development Execution Graph

```
 [1. START_EVO] ------------> [2. GitCheckTask] ------------> [3. CopySourceTask]
                                                                      |
                                                                      v
 [6. StartEvoTask] <------- [5. ExportEvoTask] <------- [4. BuildEvoTask (Tycho)]
        |
        v
 [7. EvoRcpVerifier] -------> [8. CreateEvoProjectTask] ---> [9. CreateInferenceTask]
        |
        v
 [10. Poll Status] ---------> [11. Result Verification] ---> [12. Analytic Validation]
        |
        v
 [13. Collect Diagnostics] -> [14. Git Commit/Tag] --------> [15. SelfDevSupervisor]
                                                                      |
                                                                      v
 [18. Complete Run] <-------- [17. Auto Rollback] <--------- [16. Failure Recovery]
```

---

## 3. Detailed Component Responsibilities

### 3.1 Supervisor Process Independence
- **Independence Requirement**: The Supervisor (`SupervisorMain` / `EVOSupervisorServer` on port 8089/28080) runs as an independent external Java process. It must survive target EVO RCP crashes, hangs, or unexpected exits.
- **Monitoring Loop**: `SelfDevSupervisor.runMonitoringLoop()` polls target process health and task queues. When the target process crashes, the Supervisor detects process termination, records failure diagnostics, and attempts bounded retries without crashing itself.

### 3.2 Workspace & Source Isolation
- Canonical source is acquired via `GitCheckTask`.
- `CopySourceTask` creates an isolated working copy in `<workspace>/self-dev-run/` or `target/workspace_copy/`.
- Temporary `run_*` execution directories and build outputs are strictly excluded from contaminating the canonical Git repository.

### 3.3 Build & Export Pipeline
- `BuildEvoTask` invokes `MavenBuildExecutor` executing `./mvnw clean package -P export-product -DskipTests`.
- Output validation verifies the exported product structure:
  - Native launcher executable (`evo` / `evo.exe`).
  - `evo.ini` JVM configuration.
  - Non-empty `plugins/` directory containing bundle JARs.
  - `configuration/config.ini` Equinox configuration.

### 3.4 Runtime Verification & Project/Inference Tasks
- `EvoRcpVerifier` polls `/server/status` and `/server/osgi` on port 8080 until readiness file `evo_ready.txt` is created and OSGi bundles resolve.
- `CreateEvoProjectTask` posts to `/server/project/create` to create an active `EvoProject` and bind session context.
- `CreateInferenceTask` posts asynchronous inference requests to `/task` and polls `/server/conversation/<sessionId>` for response completion.

---

## 4. Recovery, Rollback & Truthful Reporting

1. **Truthful Task Terminal States**: A task is never marked `COMPLETED` based solely on process creation or HTTP 200 headers. The response JSON payload must be parsed to confirm actual success (`status == "OK"` or non-empty output).
2. **Failure Classification**: Distinguishes transient errors (retryable with backoff) from permanent errors (security blocks, corrupt binaries, missing dependencies).
3. **Automated Rollback**: If verification fails after maximum retry attempts, `GitManager.rollback()` restores the working tree to the last known good commit.
