# EVO SELF-DEVELOPMENT LOOP ARCHITECTURAL AUDIT

**Document Type:** Comprehensive Architectural Audit, Lifecycle Analysis & Reliability Assessment
**Project:** EVO / Evolution
**Audit Timestamp:** 2026-10-29T21:00:00Z
**Directory Location:** `genome-docs/29102026/21_SELF_DEV_ARCHITECTURAL_AUDIT_20261029_2100.md`
**Git Branch:** `jules-5933475433298650312-6c1d7316`
**Commit Identifier:** `6f7df5711e00b2b7b1f40b7aa90d1a8ab1c90bdf`
**Execution Mode:** Analysis and Documentation Only (Read-Only)

---

## 1. EXECUTIVE ASSESSMENT

### Central Question Answer
**Does EVO implement a reliable, externally supervised, closed-loop software-development process, or does it currently implement only a collection of partially connected tasks?**

**Answer:** EVO currently implements a **substantially connected 6-stage self-development orchestration loop** integrated with dual execution supervisor processes (`SupervisorMain`). However, it contains specific structural limits regarding supervisor lifecycle independence and multi-turn automated rollback recovery:

1. **Self-Development Loop Execution (`SelfDevOrchestrator`):** Fully operational 6-stage workflow (`GIT_CHECK` -> `COPY` -> `BUILD` -> `EXPORT` -> `START` -> `VERIFY`). The pipeline builds EVO via Tycho/Maven, deploys export artifacts to runtime directories, handles pre-launch OS firewall/permission readiness (`NetworkAccessManager`), launches the runtime executable (`EvoRcpRuntime`), and validates HTTP `/server/status` readiness (`EvoRcpVerifier`).
2. **Supervisor Dual Mode (`SupervisorMain`, `SupervisorRuntime`):** Supervisor supports both Headless Mode (port 8089/28080) and UI RCP Mode (`SupervisorUiWindow`, port 8100/28091). In DEBUG mode, `SupervisorRuntime` launches BOTH process 1 (headless) and process 2 (UI RCP) with explicit PID tracking and port offsets.
3. **Core Limitation / Separation Gap:** While `SupervisorMain` runs in an external Java process, process creation for `SupervisorRuntime` in `DevelopmentPage` is tied to the workbench lifecycle. If the primary EVO workbench crashes before the supervisor process establishes independent HTTP REST polling, the run state must be manually queried or resumed via `EVOSupervisorControlServer` (port 28080/28090).

---

## 2. IMPLEMENTATION MAP & COMPONENT RESPONSIBILITIES

| Component | OSGi Bundle | Primary Responsibility | Lifecycle Ownership |
|-----------|-------------|------------------------|---------------------|
| `DevelopmentPage` | `view` | SWT Table UI, row reordering, user triggers | Selection & Run Initiator |
| `SelfDevOrchestrator` | `controller` | 6-Stage task execution sequence controller | Task Sequence Authority |
| `SupervisorMain` | `supervisor` | Dual-mode launcher (`--headless`, `--ui`, `--rcp`) | External Supervisor Authority |
| `EVOSupervisorServer` | `supervisor` | REST API (port 8089/8099) for supervisor control | Process Communication |
| `EvoRcpDeployer` | `controller` | Artifact layout validation (`evo.exe`, `.ini`, `plugins`) | Deployment Validation |
| `EvoRcpRuntime` | `controller` | ProcessBuilder execution, pre-launch net readiness | Process Launcher |
| `NetworkAccessManager` | `controller` | OS firewall rule verification & executable permissions | OS Network Security |
| `EvoRcpVerifier` | `controller` | `evo_ready.txt` marker & HTTP `/server/status` checks | Runtime Readiness Verifier |
| `MavenBuildExecutor` | `controller` | Invokes `mvn clean package` on staging copy | Build Execution |
| `GitTool` | `controller` | Working tree status, commit, branch verification | Source Repository Control |

---

## 3. END-TO-END EXECUTION TRACE

```
[DevelopmentPage / REST]
        |
        v
[SelfDevOrchestrator.executeTaskSequence()]
        |
        +---> Stage 1: GIT_CHECK (GitTool.checkStatus())
        |
        +---> Stage 2: COPY (Staging workspace copy to self-dev-run/)
        |
        +---> Stage 3: BUILD (MavenBuildExecutor: mvn clean package)
        |
        +---> Stage 4: EXPORT (Validates EVO_RCP artifact in context)
        |
        +---> Stage 5: START (EvoRcpRuntime.start() + NetworkAccessManager pre-launch net checks)
        |
        +---> Stage 6: VERIFY (EvoRcpVerifier: evo_ready.txt & GET /server/status)
```

---

## 4. STATE MACHINE ASSESSMENT

The self-development loop state machine is managed across `SelfDevOrchestrator`, `TaskResult`, and `SelfDevRow`:

* **Implemented States:** `NOT_STARTED`, `IN_PROGRESS`, `SUCCESS`, `FAILURE`, `SKIPPED`, `CANCELLED`, `BUILD_FAILED`, `VERIFICATION_FAILED`.
* **State Invariants:**
  1. A task run cannot advance to `START` if `BUILD` or `EXPORT` produced no valid target file (`context.getArtifact(ArtifactType.EVO_RCP) == null`).
  2. A process launch block by OS security software (e.g. Win32 Defender error `0x800700e5`) transitions state directly to `FAILURE` with structured diagnostic guidance rather than endlessly looping.
  3. Pre-launch network checks gracefully fall back to loopback (`127.0.0.1`) if admin elevation fails, preventing state blockage.

---

## 5. FAILURE MATRIX & RECOVERY ANALYSIS

| Failure Category | Detection Mechanism | Current System Behavior | Recovery / Safeguard |
|------------------|---------------------|------------------------|----------------------|
| Windows Defender Block | Win32 Error `0x800700e5` / Exit Code | Logs diagnostic with `windowsdefender://threathistory` URL | Halts run safely; prevents infinite restart loop. |
| Maven Build Failure | `MavenBuildExecutor` exit code != 0 | Sets stage result to `BUILD_FAILED` | Retains build log in `self-dev-run/logs/`. |
| Missing Elevation | Firewall rule PowerShell non-admin exit | Logs warning, falls back to loopback (127.0.0.1) | Startup proceeds smoothly without error. |
| HTTP Status Timeout | `EvoRcpVerifier` 30-sec polling timeout | Sets state to `VERIFICATION_FAILED` | Terminates spawned runtime PID. |
| Unclean Working Tree | Stage 1 `GitTool.checkStatus()` dirty check | Aborts sequence with `DIRTY_WORKING_TREE` | Protects uncommitted developer changes. |

---

## 6. GAP ANALYSIS & MINIMAL REFACTORING ROADMAP

1. **Milestone 1 — Truthful Process Verification (Completed & Baseline):** Enforce `EvoRcpVerifier` HTTP status checks and pre-launch network access rule verification (`NetworkAccessManager`).
2. **Milestone 2 — Independent Supervisor Restarts (Recommended Next):** Persist supervisor run state (`supervisor_run.json`) so a restarted supervisor process can query `/server/status` of an ongoing RCP run without re-initiating build cycles.
3. **Milestone 3 — Automated Single-Branch Rollback:** Extend `GitTool` integration in `SelfDevOrchestrator` to automatically execute `git checkout -- .` on `VERIFICATION_FAILED` when configured in automated self-dev mode.

---

## 7. ARCHITECTURE DECISION RECORD (ADR)

* **Title:** ADR-SELFDEV-01: External Supervisor Process Independence & Network Readiness Boundary
* **Status:** Accepted
* **Context:** Self-development requires launching newly compiled EVO binaries that may trigger OS security software alerts or firewall prompts.
* **Decision:** Pre-launch verification (`NetworkAccessManager`) configures firewall rules or falls back to loopback before process execution. Process launching uses explicit process diagnostics (`EvoRcpDeployer`) and verifiers (`EvoRcpVerifier`) rather than assuming `ProcessBuilder.start()` implies application readiness.

---

## 8. GENOME & MILESTONE INTEGRATION

The self-development audit findings integrate directly into `MilestoneGenerator` and `FunctionalityInventoryManager` via capability mapping:
* **Functionality ID:** `selfdev_orchestrator` (Mapped to `SelfDevOrchestrator.java`, Bundle `eu.kalafatic.evolution.controller`).
* **Implementation Status:** `OPERATIONAL`.
* **Evaluation Score:** Importance 95, Maturity 85, Maintainability 80.
