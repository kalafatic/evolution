# TRYST Platform Architectural Audit & Improvement Report

**System:** TRYST Autonomous Development Platform (Eclipse RCP / OSGi / Java 21)
**Date:** November 2026
**Author:** Jules (Autonomous Chief Systems Architect)

---

## 1. Executive Summary

This report delivers a comprehensive architectural investigation of the TRYST autonomous development infrastructure integrated into the Eclipse RCP / OSGi runtime and EVO server platform. The primary objective is to audit, harden, and harmonize the platform's core operational capabilities:
1. **Task Pool & Concurrency Management**
2. **Secure Sandbox Execution for User Repositories**
3. **Repository Reuse & Workspace Isolation**
4. **Repository and Task Synchronization**
5. **User Notifications and AI-Assisted Task Scheduling**
6. **Lifecycle Reliability and Server Restart Recovery**

The audit evaluates existing capabilities, uncovers critical concurrency and security vulnerabilities, and details the concrete architectural improvements implemented to achieve enterprise-grade reliability, security, and architectural harmony with EVO's single-authority principles.

---

## 2. Existing Architecture & Execution-Flow Mapping

### 2.1 Complete Execution Path
The end-to-end execution flow of an autonomous development request proceeds through the following pipeline:

```
User Request (HTTP POST /develop/tasks, /develop/plan, or Web UI /develop.html)
   │
   ▼
Task Planning & Decomposition (`DevelopTaskPlanner.planCompoundRequest`)
   │  Proposes sub-tasks, dependencies, and parallelization advice (advisory)
   ▼
Project & Task Registration (`DevelopTaskManager.createTask`)
   │  Assigns taskId, captures title, description, repo URL, branch, permissions, dependencies, priority
   ▼
Session & Workspace Provisioning (`DevelopSessionManager.createSession`)
   │  Creates workspace directory under ~/.evo/develop-workspaces/<sessionId>
   │  Coordinates with RepositoryWorkspaceManager for bare mirror cache or worktree
   │  Registers TaskContext & SessionContainer in EVO core SessionManager
   ▼
Task Scheduling & Concurrency Control (`DevelopTaskManager`)
   │  Enqueues task into priority queue
   │  Evaluates dependencies and concurrency capacity
   │  Acquires project lock via `ProjectLockManager` with thread-agnostic atomic lease state machine
   ▼
AI Agent Execution (`DevelopAgent.executeTask`)
   │  Step 1: PREPARING (Clone repo/worktree, checkout branch)
   │  Step 2: ANALYZING (Detect build system, language, dependencies via RepositoryAnalyzer)
   │  Step 3: PLANNING (Task context construction)
   │  Step 4: CODING (Iterative loop: AI inference via LLM router, parse/apply file modifications)
   │  Step 5: BUILDING (RunBuildTool executes in `SandboxProcessExecutor` with env sanitization)
   │  Step 6: TESTING (RunTestsTool executes in `SandboxProcessExecutor` with env sanitization)
   │  Step 7: FIXING (Iterative AI retry loop on build/test failures)
   ▼
Diff & Review Stage (`PeerReviewPage` / `develop.html` / REST endpoints)
   │  Generates Git unified diff and changed files list
   │  Transitions to READY_FOR_REVIEW
   ▼
Delivery & Completion
   │  Stages accepted files, creates local commit, optional push, synthesizes PR
   │  Releases ProjectLockManager locks, decrements active worker count, triggers queue dispatch
```

---

## 3. Inventory of Existing Components & Responsibilities

| Component | Class | Responsibility |
|---|---|---|
| **Task Management** | `DevelopTaskManager` | Priority-aware queueing, dependency-resolution dispatch loop, worker pool concurrency, and restart recovery. |
| **Session & Workspace** | `DevelopSessionManager` | Manages per-session directories, state serialization (`state.json`), and EVO `SessionContainer` synchronization. |
| **Session Model** | `DevelopSession` | Binds `DevelopTask`, repository directory, `GitService`, log aggregation, and Git operations (diff, commit, push). |
| **Autonomous Agent** | `DevelopAgent` | Implements iterative autonomous coding, build, test, and self-fixing loop. |
| **VCS Service** | `GitServiceImpl` | Executes Git commands (`clone`, `branch`, `diff`, `commit`, `push`, `createPullRequest`) via `ProcessExecutor`. |
| **Project Analyzer** | `RepositoryAnalyzerImpl` | Detects build systems (Maven, Gradle, Ant, Node.js, Python, CMake, Make) and project languages. |
| **Process Execution** | `ProcessExecutorImpl` | Runs OS commands via `ProcessBuilder` with stdout/stderr capture and timeout waiting. |
| **Sandbox Execution** | `SandboxProcessExecutor` | Hardened process executor enforcing environment sanitization, directory containment, timeouts, and tree termination. |
| **Build & Test Tools** | `RunBuildTool`, `RunTestsTool` | Invokes resolved build and test commands inside `SandboxProcessExecutor`. |
| **Workspace Manager** | `RepositoryWorkspaceManager` | Manages shared bare Git repository mirrors and isolated task worktrees. |
| **Lock Manager** | `ProjectLockManager` | Fine-grained project and workspace locking with atomic lease state machine and expiration preemption. |
| **AI Planner** | `DevelopTaskPlanner` | Advisory decomposition of compound user requests into dependency-wired task plans without infringing on scheduler authority. |
| **HTTP Controller** | `EvolutionServer` | Exposes REST endpoints for tasks, uploads, planning, logs, status, reviews, and HTML views. |
| **Event Bus** | `RuntimeEventBus` | Centralized pub/sub event bus across the platform. |

---

## 4. Identified Problems & Severity Assessment

### 4.1 Concurrency & Task Pool Gaps (Severity: HIGH)
- **Unbounded Execution & Lack of Queueing:** `DevelopTaskManager` executed tasks immediately upon submission on an unbounded cached thread pool.
- **Missing Priority and Dependencies:** Tasks lacked priority levels and inter-task dependency handling (`dependsOnTaskIds`).
- **Absence of Waiting States:** Tasks lacked `WAITING_FOR_RESOURCES` and `WAITING_FOR_DEPENDENCIES` states.
- **Race Conditions:** Tasks could be dispatched multiple times concurrently if submitted in rapid succession.

### 4.2 Repository Management & Workspace Duplication (Severity: HIGH)
- **Repeated Full Clones:** Every task cloned the target repository from scratch into its own isolated directory, creating significant network and disk I/O overhead.
- **No Bare Mirror Sharing:** The platform lacked a shared local mirror cache for remote repositories.
- **Missing Worktree Support:** Instead of using lightweight Git worktrees pointing to a shared local cache, full clones were repeatedly created.
- **User Privacy Exposure:** No validation of user identity vs. cached repositories.

### 4.3 Process Isolation & Sandbox Security Gaps (Severity: CRITICAL)
- **Execution in Host Context:** Untrusted project build and test commands ran directly in the host OS process environment without sandboxing.
- **Credential Leakage:** Environment variables from the host (tokens, API keys, credentials) were inherited by child processes.
- **Directory Traversal / Escape:** Commands could traverse outside the designated workspace repository directory. Sibling prefix escapes (`/workspace-sibling`) were possible with string prefix checks.
- **Orphaned Processes:** On timeout, child processes spawned by Maven/Gradle/Node were not recursively terminated.

### 4.4 Task Synchronization on Shared Projects (Severity: HIGH)
- **No Multi-Task Coordination:** Two tasks executing on the same repository simultaneously had no lock mechanism to prevent conflicting Git updates or concurrent build/test runs.
- **Global Serialization vs. Fine-Grained Locks:** There was no locking coordinator to allow parallel execution of independent repositories while safely serializing operations on the same project.

---

## 5. Architectural Compatibility Matrix & Classifications

The audit categorizes all investigate components according to their role and fit with EVO's single-authority architecture:

| Component | Target Location | Classification | Evidence & Architectural Reasoning |
|---|---|---|---|
| `SandboxProcessExecutor` | `develop.executor` | **KEEP** | Generic, hardened process runner enforcing environment sanitization, working directory containment via `Path.startsWith()`, and tree termination via `ProcessHandle.descendants()`. Broadly usable by EVO tools without conflicting with `ProcessRunner`. |
| `RunBuildTool` | `develop.tools` | **ADAPT** | Adapted to execute through `SandboxProcessExecutor`. Integrates detected build commands (Maven, Gradle, NPM, Make, Python) into structured `ExecutionMetrics`. |
| `RunTestsTool` | `develop.tools` | **ADAPT** | Adapted to execute through `SandboxProcessExecutor`. Captures test execution outcomes and enforces sandbox timeouts. |
| `RepositoryWorkspaceManager`| `develop.workspace` | **KEEP** | Manages shared bare Git repository mirrors (`git clone --mirror`, `git remote update`) and isolated Git worktrees (`git worktree add`). Prevents redundant remote cloning, sanitizes branch names, and enforces user tenant isolation (`computeRepoKey`). Wired into `DevelopSessionManager`. |
| `DevelopTask` | `develop` | **KEEP** | Models autonomous development tasks with priority, dependencies (`dependsOnTaskIds`), delivery stages, and user context. |
| `DevelopTaskStatus` | `develop` | **KEEP** | Explicit lifecycle states extended with `WAITING_FOR_RESOURCES` and `WAITING_FOR_DEPENDENCIES` to model queueing. |
| `DevelopTaskManager` | `develop` | **ADAPT** | Authoritative scheduler for autonomous development workers; coordinates with EVO `SessionManager` (`SessionContainer`) and publishes lifecycle events to EVO `RuntimeEventBus` without bypassing core session lifecycle or flooding events. |
| `ProjectLockManager` | `develop.sync` | **KEEP** | Fine-grained per-project lock coordinator with thread-agnostic atomic lease preemption and timeout release, avoiding global locks while allowing parallel execution across independent repositories. |
| `DevelopTaskPlanner` | `develop.agent` | **KEEP** | Advisory AI planning engine for decomposing compound user requests into dependency-wired task plans; materializes active sessions upon plan creation. |
| `RuntimeEventBus` (Events) | `workflow` | **MERGE** | Integrated task events (`TASK_QUEUED`, `TASK_WAITING_FOR_RESOURCES`, `TASK_WAITING_FOR_DEPENDENCIES`, `TASK_CANCELLED`, `BUILD_COMPLETED`, `TESTS_COMPLETED`, `SANDBOX_VIOLATION`) into EVO's canonical `RuntimeEventType` enum rather than introducing a parallel event bus. |
| `/develop/plan` Endpoint | `EvolutionServer` | **MERGE** | Integrated directly into existing `EvolutionServer` REST routing, preserving single HTTP server port and dispatch lifecycle. |

---

## 6. Concrete Changes Implemented

1. **`RepositoryWorkspaceManager.java`:**
   - Implemented bare mirror cloning and fetch synchronization (`git clone --mirror`, `git remote update`) in `~/.evo/repo-cache/<repoKey>.git`.
   - Provisioned isolated worktree working directories with fallback support.
   - User-scoped cache keys (`computeRepoKey(repoUrl, userContext)`) preventing cross-tenant repository leakage.
   - Branch argument sanitization preventing shell injection.
   - Automatic workspace and worktree cleanup (`git worktree remove`, `git worktree prune`).
   - Wired directly into `DevelopSessionManager.createSession(...)` for automated worktree provisioning.

2. **`SandboxProcessExecutor.java`:**
   - Environment sanitization: strips sensitive environment variables containing `EVO_`, `TRYST_`, `AWS_`, `GITHUB_`, `TOKEN`, `SECRET`, `PASSWORD`.
   - Containment validation: verifies working directory using `java.nio.file.Path.startsWith(Path)`, blocking path traversal and sibling directory escapes.
   - Resource and timeout enforcement: terminates entire process tree recursively via `ProcessHandle.descendants()`.
   - Stream buffering: capped at 2MB per stream to prevent OOM errors.

3. **`RunBuildTool.java` & `RunTestsTool.java`:**
   - Updated to execute through `SandboxProcessExecutor.executeSandboxed(...)` with workspace containment.

4. **`ProjectLockManager.java`:**
   - Implemented an `AtomicReference<Lease>` state machine with lease expiration preemption.
   - Thread-agnostic release: allows safe cancellation and lock cleanup from any thread without `IllegalMonitorStateException`.

5. **`DevelopTaskManager.java`:**
   - Configurable worker pool concurrency (`maxConcurrentTasks`).
   - Priority queueing (ordered by priority desc, createdAt asc).
   - Dependency checking loop (`areDependenciesSatisfied`).
   - State-transition guards preventing repetitive event notification flooding.
   - Server restart recovery (`recoverTasksOnRestart`).

6. **`DevelopTask.java` & `DevelopTaskStatus.java`:**
   - Added priority (1-10), dependency list (`dependsOnTaskIds`), and user context.
   - Added `WAITING_FOR_RESOURCES` and `WAITING_FOR_DEPENDENCIES` states.

7. **`DevelopTaskPlanner.java` & `EvolutionServer.java`:**
   - Advisory compound instruction analysis decomposing tasks, dependencies, and parallelization advice.
   - Automatically initializes `DevelopSession` for materialized tasks.
   - Added `POST /develop/plan` REST endpoint.

8. **`RuntimeEventType.java` & `MANIFEST.MF`:**
   - Added task, build, test, and sandbox event types to `RuntimeEventType`.
   - Exported `eu.kalafatic.evolution.controller.orchestration.develop.sync` and `develop.workspace`.

---

## 7. Security & Concurrency Verification Results

The test suite (`DevelopConcurrencyAndSandboxTest.java` and `DevelopIntegrationTest.java`) validates all 14 mandatory operational scenarios:

| # | Scenario | Status | Verification Detail |
|---|---|---|---|
| 1 | Concurrent tasks on same repository | **PASS** | `ProjectLockManager` coordinates mutual exclusion with lease timeouts. |
| 2 | Concurrent tasks on different repositories | **PASS** | Independent repository tasks acquire locks and run concurrently in parallel. |
| 3 | Repository mirror clone reuse | **PASS** | Deterministic SHA-256 repo caching and remote update reuse. |
| 4 | Independent workspace isolation | **PASS** | Worktree directories are fully isolated; modifications do not leak across sessions. |
| 5 | Workspace and repository locking timeout & preemption | **PASS** | Expired lock leases allow preemption by waiting tasks without deadlocks. |
| 6 | Task dependency execution order | **PASS** | Child task waits in `WAITING_FOR_DEPENDENCIES` until prerequisite completes. |
| 7 | Duplicate task dispatch prevention | **PASS** | Repeated submission of active/queued tasks is ignored safely. |
| 8 | Sandbox environment sanitization | **PASS** | Tokens, secrets, and API keys are stripped before process launch. |
| 9 | Sandbox process tree termination | **PASS** | Child and descendant processes are forcibly destroyed on timeout. |
| 10| Sandbox directory containment | **PASS** | Path containment verification blocks escapes, including sibling folder attacks. |
| 11| Server restart state recovery | **PASS** | In-flight active tasks transition to `FAILED` with diagnostics; queued tasks re-enqueue. |
| 12| User isolation & credential segregation | **PASS** | Multi-tenant users accessing identical URLs receive distinct cache keys. |
| 13| AI Task Planner decomposition | **PASS** | Compound instructions decompose into structured sub-tasks with active session bindings. |
| 14| Git branch isolation | **PASS** | Worktree branches check out cleanly without working-tree collision. |

---

## 8. Remaining Limitations & Next Steps

1. **Linux Production Containers / cgroups:**
   - For ultra-high-risk untrusted code execution on Linux production hosts, `SandboxProcessExecutor` can be extended with optional rootless Podman / Docker containers or Linux `unshare` / cgroup v2 limits without breaking the current JVM process model.
2. **Persistent Lock Store across Distributed Nodes:**
   - `ProjectLockManager` currently uses memory-based atomic leases backed by SQLite session state; distributed clustering across multiple physical nodes would require distributed lock leases.
