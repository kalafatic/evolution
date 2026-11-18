# SELF-DEV COMPONENT MAP (WP0)

Every relevant component, its responsibility, location, consumers and defects.
Base: `/home/petr/git/evolution-121126/` — `c` = `eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/`, `s` = `eu.kalafatic.evolution.supervisor/src/main/java/eu/kalafatic/evolution/supervisor/`, `v` = `eu.kalafatic.evolution.view/src/eu/kalafatic/evolution/view/`, `m` = `eu.kalafatic.evolution.model/src/eu/kalafatic/evolution/model/`.

---

## 1. Orchestration & task pipeline

| Component | Location | Role | Consumers | Defects |
|---|---|---|---|---|
| `SelfDevOrchestrator` | c/orchestration/selfdev/SelfDevOrchestrator.java | Task registry (34 tasks), dependency-driven execution, result caching | SelfDevBootstrapController, tests | forged START_EVO (221-229); sticky FAILED; no duplicate-ID detection; preflight snapshot installed even when BLOCKED (42-44) |
| `SelfDevBootstrapController` | c/orchestration/selfdev/SelfDevBootstrapController.java | UI facade: runTask, executePreflight, startBootstrap, stopBootstrap | DevelopmentPage, SupervisorGroup | reflective dead helpers (72-85) |
| `SelfDevTask` (interface) / `AbstractSelfDevTask` | c/orchestration/selfdev/ | Task contract: preValidate/run/postValidate/cancel; exception→FAILED | all 20+ task classes | cancel() never invoked anywhere (dead); no timeout wrapper |
| `TaskResult` / `TaskStatus` (controller) | c/orchestration/selfdev/ | Result/status model | all tasks, UI | READY/RUNNING/SUCCESS/FAILED/BLOCKED/SKIPPED only — no WARNING/CANCELLED/NOT_EXECUTED; SKIPPED carries reason via message |
| `TaskExecutor` (selfdev pkg) | c/orchestration/selfdev/TaskExecutor.java | Executes EMF-model Tasks through EvolutionOrchestrator | ADarwinEngine | parallel "task" concept disconnected from registry |
| `SelfDevPreflight` / `SelfDevPreflightResult` | c/orchestration/selfdev/ | Pre-run validation; builds ResolvedSelfDevResources | DevelopmentPage.runDebug only | fallbacks collapse onto repo root (61-66); "Valid DAG" without analysis (423-440); advisory only |
| `TaskPlanner` | c/orchestration/selfdev/TaskPlanner.java | LLM task planning from BranchVariant | Darwin engine paths | separate EMF-task world |

## 2. Resource & workspace

| Component | Location | Role | Defects |
|---|---|---|---|
| `ResourceManager` | c/resource/ResourceManager.java | Central EMF-config path resolution | everything | default `user.home/git/evolution` (170,186); canonical-root enforcement (105-118) bypassed by several consumers; EMF model auto-guessing (61-85) |
| `SelfDevContext` | c/orchestration/selfdev/SelfDevContext.java | Per-run context: run id, dirs, task results, artifacts, supervisor PID/exe/workdir, effective ports | all tasks | run id = second-resolution timestamp, no uniqueness check (89-90); `/app` + projectRoot fallbacks skip canonical validation (75-87); dual source of truth with snapshot (243-325); ProcessOwnership enum unused (49-54) |
| `ResolvedSelfDevResources` | c/orchestration/selfdev/ResolvedSelfDevResources.java | Immutable path/platform snapshot | tasks via context | created even when preflight BLOCKED (SelfDevPreflight.java:276-291) |
| `GitSourceProvider` / `SourceProvider` | c/orchestration/selfdev/ | Repo validation, pull/fetch, recursive copy | GitCheckTask, CopySourceTask | dirty tree passes; pull-fail→fetch→SUCCESS (80-95) |
| `PortRegistry` | c/resource/PortRegistry.java | Port constants + DEBUG_PORT_OFFSET=10 (41) | context, supervisor | good; but supervisor re-derives offset independently |
| `ProjectManager` (view) | v/provider/ProjectManager.java | 7-step codebase discovery for UI projectRoot | DevelopmentPage/SupervisorGroup | separate discovery chain (133-234) bypassing ResourceManager |

## 3. Build / export / deploy

| Component | Location | Role | Defects |
|---|---|---|---|
| `MavenBuildExecutor` | c/orchestration/selfdev/MavenBuildExecutor.java | Canonical Maven exec: timeout, streams, retry(1), error classification | TychoEvoRcpBuilder, MavenSupervisorBuilder, GenomeCheckTask | exit-code-only success; recovery only for NETWORK/TRANSIENT |
| `TychoEvoRcpBuilder` | c/orchestration/selfdev/TychoEvoRcpBuilder.java | EVO Tycho build + exact product locate + validate | BuildEvoTask, ExportEvoTask | skipTests=true default (23); export re-runs build when product missing (214-235); getArtifact re-scans each call (302-334) |
| `EvoRcpBuilder` / `AbstractProjectBuilder` / `ProjectBuilder` | c/orchestration/selfdev/ | Abstraction layer over build | — | thin wrappers |
| `MavenSupervisorBuilder` | c/orchestration/selfdev/MavenSupervisorBuilder.java | Supervisor jar build | BuildSupervisorTask | builds at canonical repo root (31-39); `-DskipTests`; `jars[0]` pick (94-126) |
| `EvoRcpDeployer` / `AbstractDeployer` / `Deployer` | c/orchestration/selfdev/ | Unzip artifact to runtime/evo | DeployEvoTask | zip-slip unprotected (170-190) |
| `EvoRcpVerifier` | c/orchestration/selfdev/EvoRcpVerifier.java | Readiness: liveness, fatal-exception scan, evo_ready.txt, /server/status, /server/osgi | EvoRcpRuntime | alive-at-timeout ⇒ SUCCESS (64-82); OSGi exceptions ignored (129-131) |
| `EvoRcpRuntime` / `EvoRcpLifecycle` / `DefaultEvoRcpLifecycle` | c/orchestration/selfdev/ | EVO child launch/stop | StartEvoTask, StopEvoTask | `zips[0]` fallback (36-44); no port availability check; fast-path "already running" (20-25); start-command.txt only PID-ish record (115-121) |
| `PromoteTask` | c/orchestration/selfdev/ | Promote artifact to export/promoted | UI row | new BuildArtifact w/o checksum (64-71); dep only START_EVO (153-154) |
| `ProcessRunner` (supervisor) | s/ProcessRunner.java | Supervisor-side mvn build/test/run/patch | SelfDevSupervisor, SupervisorMain | own invocations; `reset --hard`+`clean -fd` before patch (273-309) |
| `EvoProductBuildService` | s/EvoProductBuildService.java | Supervisor-side product build | SelfDevSupervisor | exact-name else `Files.walk(...).findFirst()` (43-91) |
| `EvoProductValidator` | s/EvoProductValidator.java | Supervisor-side product layout validation | SelfDevSupervisor | own unzip without zip-slip guard (277-300) |
| `RcpBuildTool` / `BuildConfiguration` | s/bootstrap/ | Bootstrap build tool | supervisor tests | skipTests default true |

## 4. Supervisor lifecycle & processes

| Component | Location | Role | Defects |
|---|---|---|---|
| `SupervisorRuntime` (controller) | c/orchestration/selfdev/SupervisorRuntime.java | Launch/stop external supervisor | StartSupervisorTask, StopSupervisorTask, RollbackTask | static process handles leak across runs (12-13); "already running" fast path (27-32); kill by PID unverified (279-291); `/shutdown` unimplemented server-side; +1 UI-supervisor scheme (102-103) |
| `SupervisorClient` | c/orchestration/selfdev/SupervisorClient.java | HTTP client /ping /start-evo /stop-evo /build /export | tasks | ping-only liveness; hardcoded 28480 fallback (40) |
| `SupervisorLifecycle` / `AbstractSupervisorLifecycle` / `DefaultSupervisorLifecycle` | c/orchestration/selfdev/ | Supervisor lifecycle abstraction | SelfDevOrchestrator | thin |
| `SupervisorDeployer` | c/orchestration/selfdev/SupervisorDeployer.java | Copy supervisor jar → runtime/supervisor | ExportSupervisorTask | copies to runtime, not export dir |
| `SupervisorMain` | s/SupervisorMain.java | Supervisor HTTP server + start/stop/export evo handlers | parent RCP, SupervisorClient | static activeEvoProcess (161); 3s liveness "readiness" (702-712, 774-784); no descendants kill (946); command.json RESTART → duplicate EVO (917-920); unzip w/o guard (970-997) |
| `SelfDevSupervisor` (supervisor) | s/SelfDevSupervisor.java | while(true) task lifecycle loop | (auto-started) | unbounded loop (55); no objective feed; task state json w/o process fields |
| `SupervisorState` | s/SupervisorState.java | Task queue/history persistence | SelfDevSupervisor | no process data |
| `SupervisorRcpApplication` / `SupervisorUiWindow` | s/ui/ | UI supervisor variant | debug mode | separate port scheme |
| `EVOSupervisorControlServer` | s/EVOSupervisorControlServer.java | Control TCP 28580 | — | hardcoded ports (29-34) |

## 5. Darwin integration

| Component | Location | Role | Defects |
|---|---|---|---|
| `SelfDevDetector` | c/orchestration/cognitive/SelfDevDetector.java | Keyword detection → SELF_DEV capability | CognitiveAnalysisPipeline | cosmetic only |
| `SelfDevRequestAnalyzer` | c/orchestration/export/SelfDevRequestAnalyzer.java | Manual-export self-dev request analysis | export flow | unrelated to SELF_DEV_MODE |
| `SelfDevMain` | c/orchestration/SelfDevMain.java | CLI entry used by supervisor child | supervisor handleStartEvo | one run then exit |
| `SelfDevelopmentEngine` | c/orchestration/selfdev/SelfDevelopmentEngine.java | SELF_DEV_MODE Darwin engine | DarwinEngineFactory | extends ADarwinEngine — correct reuse |
| `ADarwinEngine` | c/orchestration/selfdev/ADarwinEngine.java | Main Darwin evolve loop | IterationManager | safety counter ok; no wall clock; commits/rollbacks on projectRoot git |
| `SelfDevDarwinEngine` | c/orchestration/selfdev/SelfDevDarwinEngine.java | — | none | 100% commented out (579 lines) |
| `SelfDevSupervisor` (controller) | c/orchestration/selfdev/SelfDevSupervisor.java | EMF-session supervisor loop | none (dead) | MAX_FAILURES=3; maxIter default 0 breaks loop |
| `IterationManager` (controller) | c/orchestration/IterationManager.java | Sole iteration authority | KernelFactory | correct; transitions 470-498 |
| `SelfDevSession` / `SelfDevSessionImpl` / `SelfDevStatus` / `SelfDevDecision` | m/orchestration/ | EMF session model | SelfDevMain, dead supervisor | maxIterations default 0 (SelfDevSessionImpl.java:94); decision enum only CONTINUE/ROLLBACK/STOP |
| `IterationRecord` / `IterationMemoryService` | c/orchestration/selfdev/ | Iteration JSON records | Darwin engines | partial fields; not the single ledger |
| `Evaluator` | c/orchestration/selfdev/Evaluator.java | Maven clean install + test pass-rate snapshot | ADarwinEngine | CONTINUE on build/test failure (259-268) |
| `SupervisorLoopTask` / `SelfDevLoopTask` | c/orchestration/selfdev/ | "loop" tasks | UI rows | single pings, no loop |

## 6. Git

| Component | Location | Role | Defects |
|---|---|---|---|
| `GitManager` | c/orchestration/selfdev/GitManager.java | Facade over GitTool: init/commit/checkout/merge/worktree/rollback | KernelFactory, RollbackTask, engines | `reset --hard`+`clean -fd` (198-206); `--allow-empty` commits (98-99, 176-178); string-built branch commands (142,167,212-215) |
| `GitTool` | c/tools/GitTool.java | CLI git wrapper (ShellTool) | GitManager etc. | silent working-dir retarget (138-151); rollback reset/clean (275-278); un-gated push (196-199) |
| `GitVersionControlProvider` | c/vcs/GitVersionControlProvider.java | Direct ShellTool git ops | PeerReviewService, GitServiceImpl | commit+push adjacent unconditional; no gate |
| `EclipseGitEvoTool` | c/tools/EclipseGitEvoTool.java | JGit full op set | ProjectManager | master push block config-only (986-1019); own rollback |
| `EclipseGitRepositoryManager` | c/orchestration/selfdev/EclipseGitRepositoryManager.java | JGit repo in workspace | none | dead |
| `GitServiceImpl` | c/orchestration/develop/vcs/GitServiceImpl.java | develop-mode git | DevelopSession | separate integration |
| `GitCheckTask` | c/orchestration/selfdev/GitCheckTask.java | pre-iteration repo validation | UI rows | see defects; no diff/commit/push |
| `RollbackTask` | c/orchestration/selfdev/RollbackTask.java | self-dev rollback task | UI row | always SUCCESS; promoted[0] arbitrary; isolated-copy-only reset |

## 7. UI

| Component | Location | Role | Defects |
|---|---|---|---|
| `DevelopmentPage` | v/editors/pages/DevelopmentPage.java | 28-row pipeline page | — | hardcoded rows (361-392); registry unused; SKIPPED→red; lowercase ready; batch always 100% (1021-1023); NPE risk bootstrapController null (595,1007); background-thread getShell (841); default ports not allocated (379-380, 1128-1129) |
| `SupervisorGroup` | v/editors/pages/development/SupervisorGroup.java | supervisor controls | — | new controller per action (376,393,431); pidLabel never updated (95); hardcoded 28480 (98); hardcoded log paths (460-462) |
| `RowEditDialog` / `SelfDevEditDialog` | v/.../pages/development/ | row editing | DevelopmentPage | free-text status injection |
| `InteractiveWorkflowGroup` / `VizGroup` | v/.../pages/development/ | workflow/viz | — | not audited in depth |

## 8. Tests (existing coverage)

| Test | Location | Covers |
|---|---|---|
| SelfDevTaskArchitectureTest | eu.kalafatic.evolution.controller.tests | registry structure (injects fakes reflectively) |
| SelfDevPathArchitectureInvariantsTest | controller.tests | path invariants |
| SelfDevPipelineHardeningTest | controller.tests | pipeline incl. port offset (252-254) |
| SelfDevPreflightCognitiveTest | controller.tests | preflight |
| RealSelfDevLifecycleE2ETest | controller.tests | E2E attempt |
| SelfDevStandalonePipelineTest | controller.tests | standalone pipeline |
| SelfDevResourceManagerIntegrationTest | controller.tests | resource manager |
| SelfDevFlowTest / SelfDevHttpTasksTest | controller.tests | flow/http tasks |
| CanonicalEvoRepositoryPreflightTest | controller.tests | canonical repo preflight |
| Supervisor bundle tests | eu.kalafatic.evolution.supervisor/src/test | SupervisorModeTest, SelfDevSupervisorTest, ExportAndStartEvoEndpointTest, tool tests |

---

## 9. Duplication index (single-responsibility violations)

| Capability | Number of implementations | Locations |
|---|---|---|
| Maven invocation | 10+ | MavenBuildExecutor, TychoEvoRcpBuilder, MavenSupervisorBuilder, GenomeCheckTask, ProcessRunner, RcpBuildTool, MavenTool, EclipseTool, OrchestrationCommandHandler, Evaluator, develop tools |
| Product ZIP location | 8+ | TychoEvoRcpBuilder.findExactExportedProduct, getArtifact, StartEvoTask fallback, EvoRcpRuntime zips[0], EvoProductBuildService, SupervisorMain.copyProductArtifacts/handleExport/handleStartEvo, SelfDevSupervisor, ProcessRunner |
| Unzip | 4 | TychoEvoRcpBuilder (safe), EvoRcpDeployer, EvoProductValidator, SupervisorMain (unsafe) |
| Product layout validation | 3 | TychoEvoRcpBuilder.validateDirectoryLayout, EvoProductValidator.validateLayout, EvoRcpDeployer.validateDeployment |
| Supervisor ping | 6 | SupervisorCheckTask, SupervisorLoopTask, SelfDevLoopTask, SelfDevOrchestrator.isRunning, SupervisorManager.ping, SupervisorGroup.handlePingHealth |
| Git integration | 5+ | GitTool, GitManager, GitVersionControlProvider, EclipseGitEvoTool, GitServiceImpl (+dead EclipseGitRepositoryManager) |
| Stop logic | 3 | RollbackTask (direct StopXTask.execute), SelfDevOrchestrator.stopBootstrap, SupervisorGroup.handleStopSupervisor |
| TaskStatus enum | 3 | controller selfdev, model EMF, supervisor bundle |
| Write-permission check | 2 | SelfDevPreflight, PermissionsCheckTask |
| Directory creation | 3 | SelfDevContext.ensureDirectories, PermissionsCheckTask, SelfDevPreflight write-test |
| Port availability | 2+ | SupervisorMain.findAvailablePort, ProcessRunner.findAvailablePort (silent fallback) |
| Supervisor process launch | 2 | SupervisorRuntime (parent), SupervisorLauncherTool (supervisor bootstrap) |
