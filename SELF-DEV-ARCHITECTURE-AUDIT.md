# SELF-DEV ARCHITECTURE AUDIT (WP0)

Status: **READ-ONLY AUDIT — no source files modified.**
Date: 2026-11-17
Scope: EVO Self-Development subsystem — controller selfdev package, supervisor bundle, view (DevelopmentPage), Darwin integration, tests.

---

## 1. Executive summary

The Self-Dev subsystem exists in **three parallel, partially overlapping implementations** that do not form one lifecycle:

1. **Controller task pipeline** (`eu.kalafatic.evolution.controller.orchestration.selfdev`) — the operational workhorse: 34 registered tasks in `SelfDevOrchestrator`, run through `SelfDevBootstrapController`, driven by `DevelopmentPage`.
2. **External supervisor process** (`eu.kalafatic.evolution.supervisor` bundle) — a separate JVM that launches EVO children, builds/exports/validates products and runs an unbounded `while(true)` monitoring loop.
3. **Darwin in-process engine chain** (`OrchestratorServiceImpl → KernelFacade → KernelFactory → DarwinEngineFactory → SelfDevelopmentEngine (extends ADarwinEngine) → IterationManager`) — the authoritative Darwin orchestration, correctly reused for SELF_DEV_MODE.

Critical structural findings:

- The controller task registry has **34 tasks**, not 24; the UI hardcodes **28 rows** and never consumes the registry (`DevelopmentPage.java:361-392` vs `SelfDevOrchestrator.java:48-164`; `getTaskRegistry()` has zero callers in the view bundle).
- The historical **START_EVO ~4 ms "Supervisor is already running" SUCCESS** is confirmed and still present: `SupervisorRuntime.java:27-32` fast path + static JVM-lifetime `Process` handles (`SupervisorRuntime.java:12-13`) + unconditional `START_EVO` success recording in `StartSupervisorTask.java:69-71` + result-reuse short-circuit `SelfDevOrchestrator.java:196-200` + dependency fabrication `SelfDevOrchestrator.java:221-229`.
- **No task in the pipeline ever runs tests**: every Maven/Tycho invocation in the self-dev path passes `-DskipTests` (BuildEvoTask.java:11, TychoEvoRcpBuilder.java:23/99-101, MavenSupervisorBuilder.java:29/37, GenomeCheckTask.java:51/53, supervisor ProcessRunner.java:81/99, RcpBuildTool.java:28).
- **No artifact provenance**: `BuildArtifact` has no checksum/size/run/build ID; artifact identity is re-derived by filesystem scan in each task (`zips[0]`, `archives[0]`, `jars[0]`, `findFirst`), so BUILD→EXPORT→START chaining is by filename guess, not by recorded manifest.
- **No PID files, no process-ownership records on disk, no shutdown hooks**: all child process identity is in-memory (`SelfDevContext.supervisorPid`, static handles, `SupervisorMain.activeEvoProcess`). A new parent session has zero knowledge of its children.
- **Rollback is partially destructive**: engine-level rollback runs `git reset --hard HEAD` + `git clean -fd` on the canonical repository (`KernelFactory.java:47-60,88` + `SelfDevContext.java:87` + `ADarwinEngine.java:1187-1194,1593-1595,1786-1800`), while `RollbackTask` targets the isolated copy — inconsistent isolation; no iteration baseline exists anywhere.
- **Readiness verification is soft**: `EvoRcpVerifier.java:64-82` returns SUCCESS on timeout if the process is merely alive; supervisor-side readiness is "alive after 3 seconds" (`SupervisorMain.java:702-712,774-784`).

The subsystem is not one autonomous loop today. It is a set of pipelines that can each individually succeed without proving the final acceptance criteria (real build, real tests, exact artifact, verified child RCP, verified health, verified iteration).

---

## 2. The three parallel execution worlds

### 2.1 World A — Controller task pipeline (operational UI path)

- Entry: `DevelopmentPage` → `SelfDevBootstrapController.runTask(id)` (`SelfDevBootstrapController.java:56-58`) → `SelfDevOrchestrator.executeTaskWithDependencies` (`SelfDevOrchestrator.java:182-253`).
- Registry: 34 `SelfDevTask` instances in `SelfDevOrchestrator.registerTasks()` (`SelfDevOrchestrator.java:48-164`).
- Execution: recursive dependency resolution with cycle detection (`visitingStack`), cached-result reuse, blocked-on-failed-dependency semantics.
- Used by: DevelopmentPage rows, SupervisorGroup buttons, tests.
- State: per-run in-memory `SelfDevContext` (task results, artifacts, PID fields). Run workspace on disk.

### 2.2 World B — External supervisor process (autonomous-ish loop)

- Launched by World A via `SupervisorRuntime.start` (`SupervisorRuntime.java:57-136`, `java -jar supervisor.jar <projectRoot> --port --control-port --headless`).
- Bundle: `eu.kalafatic.evolution.supervisor` (`SupervisorMain`, `SelfDevSupervisor`, `ProcessRunner`, `EvoProductBuildService`, `EvoProductValidator`).
- Provides HTTP endpoints (`/ping`, `/build`, `/export`, `/start-evo`, `/stop-evo`, `/git-check`…) in `SupervisorMain.EVOSupervisorServer` (switch at `SupervisorMain.java:210-251`).
- Runs `SelfDevSupervisor.run()`: `while(true)` polling loop (`SelfDevSupervisor.java:55-90`) executing `DevelopmentTask` lifecycle (EVO_RUNNING → BUILDING → TESTING → RUNNING → VERIFYING → COMPLETED) via `ProcessRunner` Maven/java subprocesses.
- **Bypasses Darwin**: build/test/run/verify are raw subprocess invocations; Darwin is only re-entered by launching the exported EVO product as a child (`--mode=SELF_DEV` → `SelfDevMain` → `KernelFacade`).
- Loop is unbounded (no iteration cap, no wall-clock limit); task queue is never fed from user objectives (only `createRepairTask` appends, `SelfDevSupervisor.java:333`).

### 2.3 World C — Darwin in-process engine chain (authoritative orchestration)

- Chain: `OrchestratorServiceImpl.submit` → `KernelFacade.handle` → `KernelFactory.create` (`PromptIntentAnalyzer`/`ModeRecognizer`) → `DarwinEngineFactory.createEngine(SELF_DEV_MODE)` → `SelfDevelopmentEngine extends ADarwinEngine` (`SelfDevelopmentEngine.java:10-14`) → `IterationManager` (sole iteration authority).
- Loop: `ADarwinEngine.evolve` (`ADarwinEngine.java:544-740`) with real enforcement: `safetyCounter` bound by `getMaxIterationLimit()` (`ADarwinEngine.java:3107-3153`), pause/cancel checks, terminal-phase gates, `IterationManager` state transitions.
- Defects: `Evaluator` sets CONTINUE even on build/test failure (`Evaluator.java:259-268`); no wall-clock limit; no stall/no-change detection; decision enum `SelfDevDecision` has only CONTINUE/ROLLBACK/STOP — no granular acceptance states.

Dead/unreachable code in the subsystem (confirmed):
- `controller .../selfdev/SelfDevSupervisor.startSession()` — zero callers.
- `controller .../selfdev/SelfDevDarwinEngine.java` — 579 lines, 100% commented out.
- `supervisor .../IterationManager.java` — directory copier, zero callers.
- `EclipseGitRepositoryManager` — zero callers.
- `AbstractBootstrapTask` — no subclasses.
- `TaskExecutor` (selfdev package) — executes EMF-model `Task`s through the Kernel, disconnected from the SelfDevTask registry; a second "task" concept in the same package.

---

## 3. Authority assessment (one authority per responsibility?)

| Responsibility | Authoritative today? | Actual situation |
|---|---|---|
| Resource resolution | **NO** | 8+ mechanisms (see Resource audit). Snapshot `ResolvedSelfDevResources` exists but is dual-sourced with context fields; installed even when preflight is BLOCKED. |
| Workspace creation | Partially | `SelfDevContext` constructor creates `workspace/self-dev/run_<ts>/{source,build,export,runtime,logs}`; legacy writers still target `self-dev-run/` (ServerManager.java:405/438-442, SelfDevProtocol.java:19-23). |
| Task registration | YES (registry) | `SelfDevOrchestrator.taskRegistry` — but UI ignores it and hardcodes rows. |
| Task dependencies | Partial | Recursive topological execution, but preflight's "Valid DAG" check is fake (`SelfDevPreflight.java:423-440`); duplicate-ID detection absent. |
| Task execution | Partial | Two concepts: SelfDevTask registry vs EMF-model TaskExecutor; result reuse makes FAILED sticky and START_EVO forgeable. |
| Supervisor lifecycle | **NO** | No state machine; static Process handles + booleans; graceful shutdown endpoint `/shutdown` unimplemented (`SupervisorRuntime.java:272,285` vs switch `SupervisorMain.java:210-251`). |
| Build artifact identity | **NO** | No manifest persistence; per-task filesystem re-discovery; `zips[0]`/`jars[0]`/`findFirst` nondeterminism. |
| Git operations | **NO** | 5+ parallel integrations (GitTool/ShellTool CLI, GitManager, GitVersionControlProvider, EclipseGitEvoTool/JGit, GitServiceImpl, dead EclipseGitRepositoryManager); destructive rollbacks possible on canonical repo. |
| RCP process lifecycle | Partial | `EvoRcpRuntime` per-task-instance (loses cross-run tracking); supervisor-side `activeEvoProcess` single static handle; no PID files; no descendants kill on supervisor side. |
| Rollback | **NO** | `RollbackTask` (isolated copy, swallows errors, always SUCCESS) vs engine-level `reset --hard`/`clean -fd` on canonical repo vs supervisor `applyPatch` destructive reset/clean. |
| Iteration state | Partial | EMF `SelfDevSession` + `IterationMemoryService` JSON records; run/task state in-memory only; no single persisted iteration ledger spanning all worlds. |

---

## 4. Lifecycle reconstruction (actual, today)

**UI-driven pipeline (the only fully functional path):**

```
DevelopmentPage.runDebug/runSelected
  -> SelfDevBootstrapController.runTask(id)
  -> SelfDevOrchestrator.executeTaskWithDependencies(id)
       [reuse cached SUCCESS | sticky FAILED/BLOCKED | cycle check]
       [recurse dependencies; START_EVO may be forged from START_EVO_SUPERVISOR]
       -> task.execute(context)
            cancel check -> validateDependencies -> resolveResources
            -> preValidate -> run() -> postValidate -> recordTaskResult
GIT_EVO:  existence + .git + pull --ff-only / fetch (dirty tree passes; pull failure downgraded to fetch+SUCCESS)
COPY:     recursive copy of canonical repo -> run_<id>/source (excludes self-dev-run)
BUILD_EVO: mvnw validate clean verify -P<platform> -DskipTests in run_<id>/source (45 min timeout, 1 retry)
EXPORT_EVO: locate single zip in <source>/<module>/target/products (ambiguity rejected);
            if missing: RE-RUNS the whole Maven build, then re-searches; copy to run_<id>/export
DEPLOY_EVO: delete run_<id>/runtime/evo, unzip artifact there (unprotected zip-slip)
START_EVO:  launch ./evo --port=28080(+10 debug) --mode=SELF_DEV -data <runtime>/workspace
            wait 30 s: process alive? fatal-exception scan? evo_ready.txt? /server/status? log markers?
            if timeout && process alive -> SUCCESS anyway (EvoRcpVerifier.java:64-82)
PROMOTE:    copy export artifact to export/promoted (new BuildArtifact, no checksum)
ROLLBACK:   StopEvo+StopSupervisor -> restore runtime/evo from promoted[0] -> git reset/clean on isolated copy
            (exceptions swallowed; final result SUCCESS)
```

**External supervisor path:** parent launches `supervisor.jar`; supervisor's `SelfDevSupervisor` loop drives `ProcessRunner` Maven builds (`-DskipTests`), `EvoProductValidator`, and re-launches EVO children; all process identity is a single static handle; loop unbounded.

**Darwin path:** chat request → SELF_DEV_MODE detection → `SelfDevelopmentEngine`/`ADarwinEngine.evolve` iterations with `Evaluator` (Maven `clean install`, test pass-rate computation) and `IterationManager` checkpoints; commits possible via engine GitManager (canonical repo!); termination via safety counter/phase machine only.

**There is no single flow that chains: Darwin modification → isolated build → exact artifact → verified child deployment → verified health → authorized commit.** Each world owns fragments.

---

## 5. Confirmed headline defects (full register in SELF-DEV-DEFECT-REGISTER.md)

D-01 START_EVO false SUCCESS ("Supervisor is already running") — verified code path present.
D-02 Tests skipped in every self-dev build path.
D-03 No artifact manifest (no checksum/size/run/build ID); per-task `[0]`/`findFirst` artifact guessing.
D-04 Readiness timeout downgraded to SUCCESS (process-alive only).
D-05 No PID files / process-ownership records / shutdown hooks; static process handles leak across runs.
D-06 Rollback: engine-level destructive git on canonical repo; no iteration baseline anywhere; RollbackTask always SUCCESS.
D-07 UI hardcodes 28 rows; registry (34 tasks) not consumed; 6 tasks invisible; SKIPPED renders red; batch always reports 100% complete.
D-08 Preflight is advisory only (not enforced by runSelected/single-row paths); "Valid DAG" claim without graph analysis; snapshot installed even when BLOCKED.
D-09 Supervisor loop unbounded; `SelfDevLoopTask`/`SupervisorLoopTask` are single pings, not loops; no objective→task-queue wiring.
D-10 Three distinct `TaskStatus` enums; controller enum lacks WARNING/CANCELLED/NOT_EXECUTED.
D-11 Port +10 offset applied centrally (PortRegistry/`SelfDevContext.java:371-391`) but re-derived independently supervisor-side (`this.port - 28480`, `SupervisorMain.java:677-678`); +1 UI-supervisor scheme; no port-availability check for direct EVO launch.
D-12 Legacy `self-dev-run` writers/readers still active (ServerManager, SelfDevProtocol, EvoRcpVerifier readiness-file guessing, view log paths).
D-13 Supervisor reactor build runs Maven in the canonical repo root (`MavenSupervisorBuilder.java:31-39`; `GenomeCheckTask`).
D-14 STOP kills by PID with no identity verification (`SupervisorRuntime.java:279-291`); `/shutdown` endpoint unimplemented.
D-15 No commit-eligibility gate; un-gated pushes in several paths; master protection configurable/bypassable.

---

## 6. Verified evidence inventory

Key files fully audited (read + traced, not assumed):

- `eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/orchestration/selfdev/` — 156 Java files (~28k lines): orchestrator, context, resources snapshot, preflight, all task classes, build/export/deploy/start/stop/rollback/promote, verifier, runtime classes, GitManager/GitSourceProvider, Darwin engines, loop tasks, net access.
- `eu.kalafatic.evolution.supervisor/src/main/java/...` — SupervisorMain, SelfDevSupervisor, ProcessRunner, EvoProductBuildService, EvoProductValidator, SupervisorState, bootstrap tools.
- `eu.kalafatic.evolution.view/.../editors/pages/DevelopmentPage.java` (1761 lines) + `development/SupervisorGroup.java`.
- `eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/resource/ResourceManager.java`, `PortRegistry.java`.
- Darwin chain: `OrchestratorServiceImpl`, `KernelFacade`, `KernelFactory`, `DarwinEngineFactory`, `ADarwinEngine`, `SelfDevelopmentEngine`, `IterationManager`, `Evaluator`.
- Tests: `controller.tests` (`SelfDevTaskArchitectureTest`, `SelfDevPathArchitectureInvariantsTest`, `SelfDevPipelineHardeningTest`, `RealSelfDevLifecycleE2ETest`, `SelfDevStandalonePipelineTest`, `SelfDevResourceManagerIntegrationTest`, `SelfDevFlowTest`, `SelfDevHttpTasksTest`, `SelfDevPreflightCognitiveTest`), supervisor bundle tests.

Build/test commands were NOT executed during this read-only phase (see AGENTS.md verification rules; builds will run in WP1+ after implementation).

---

## 7. Bottom line

- The pieces for a real autonomous loop **exist** (isolated workspace layout, dependency-driven task graph, Tycho build, product validation, HTTP readiness, Darwin engine chain) — but they are not chained with authoritative provenance and truthful state.
- The single most damaging class of defect is **false success**: supervisor-already-running START_EVO, alive-but-unready verifier success, skipped tests, unverified artifact picks, always-success rollback, forged START_EVO, fabricated "Valid DAG".
- The single most damaging risk is **destructive git on the canonical repository** and **no baseline capture**.
- Priority order for remediation is set in `SELF-DEV-REFACTORING-PLAN.md`: (1) resource/workspace truth, (2) honest task graph + preflight, (3) artifact manifest chaining, (4) process ownership + readiness, (5) safe STOP, (6) Git isolation + rollback, (7) loop control, (8) UI observability, (9) end-to-end verification.
