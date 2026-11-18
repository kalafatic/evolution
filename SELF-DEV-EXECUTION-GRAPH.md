# SELF-DEV EXECUTION GRAPH (WP0)

Actual execution paths as they exist in code today. All file:line references verified by direct reads.
Base: `/home/petr/git/evolution-121126/`.
Abbrev: `c/...` = `eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/`, `s/...` = `eu.kalafatic.evolution.supervisor/src/main/java/eu/kalafatic/evolution/supervisor/`, `v/...` = `eu.kalafatic.evolution.view/src/eu/kalafatic/evolution/view/`.

---

## 1. Graph A — UI-driven task pipeline (operational)

```
[DevelopmentPage] v/editors/pages/DevelopmentPage.java
  runDebug (829-969) / runSelected (971-1027) / handleActionInternal->executeBackgroundTask (573-615)
  |  -> bootstrapController.runTask(row.taskId)      (595, 878, 1007)
  |      SelfDevBootstrapController c/.../selfdev/SelfDevBootstrapController.java:56-58
  |        -> orchestrator.executeTaskWithDependencies(taskId)
  |            SelfDevOrchestrator c/.../selfdev/SelfDevOrchestrator.java:182-253
  |            | cache: SUCCESS reuse (196-200); FAILED/BLOCKED sticky (202-205)
  |            | cycle: visitingStack (207-211)
  |            | deps: recursive (217-234)
  |            | FORGE: START_EVO satisfied by START_EVO_SUPERVISOR success (221-229)
  |            | blocked on dep failure (236-243)
  |            -> task.execute(context)  (248)
  |                AbstractSelfDevTask.execute c/.../selfdev/AbstractSelfDevTask.java:72-225
  |                cancel check (94-102) -> validateDependencies -> resolveResources
  |                -> preValidate -> run() -> postValidate -> recordTaskResult -> DiagnosticsSummaryWriter
```

### Node table (registry order; all registered in SelfDevOrchestrator.java:48-164)

| ID | Class | Deps | Real work | Result honesty |
|---|---|---|---|---|
| GIT / GIT_EVO / GIT_SUPERVISOR | GitCheckTask | — | exists+.git+`pull --ff-only`→fallback `fetch` | Dirty tree passes; pull failure → fetch → SUCCESS |
| COPY / COPY_SUPERVISOR | CopySourceTask | — | recursive copy repo → run source/ | ok; post-checks pom.xml |
| BUILD / BUILD_EVO / MAVEN / MAVEN_EVO | BuildEvoTask | COPY | Tycho `validate clean verify -P<platform> -DskipTests` on isolated copy | exit code; tests skipped |
| BUILD_SUPERVISOR_LOCAL | BuildSupervisorTask | COPY_SUPERVISOR | Maven `clean package -DskipTests` (supervisor dir) | ok-ish |
| BUILD_SUPERVISOR | BuildSupervisorTask | BUILD_SUPERVISOR_LOCAL | Maven `clean install -pl supervisor -am -DskipTests` **at canonical repo root** | builds on git repo (defect) |
| MAVEN_SUPERVISOR | BuildSupervisorTask | — | same build | same |
| EXPORT / EXPORT_EVO | ExportEvoTask | BUILD_EVO | locate single zip in `target/products`; if none → **re-run full build**; copy to export/ | ambiguity rejected; no checksum |
| EXPORT_SUPERVISOR | ExportSupervisorTask | BUILD_SUPERVISOR_LOCAL | deploy supervisor jar to runtime/supervisor | export dir not written (defect) |
| DEPLOY / DEPLOY_EVO | DeployEvoTask | EXPORT_EVO | delete runtime/evo; unzip artifact (zip-slip unprotected) | layout check |
| START_EVO | StartEvoTask | DEPLOY_EVO | launch ./evo; wait 30 s (EvoRcpVerifier) | **alive-at-timeout = SUCCESS** |
| STOP_EVO | StopEvoTask | — | destroy instance handle, graceful→force | ok for same instance |
| START_SUPERVISOR | StartSupervisorTask | EXPORT_SUPERVISOR | launch supervisor.jar headless; 15 s /ping | fast-path "already running" |
| START_EVO_SUPERVISOR | StartSupervisorTask | START_SUPERVISOR, DEPLOY_EVO | send `start-evo` HTTP cmd | **records START_EVO SUCCESS before result check (69-71); failure swallowed (79-86)** |
| STOP_SUPERVISOR / STOP_EVO_SUPERVISOR | StopSupervisorTask | — | shutdown cmd (unimplemented) → force kill by PID | PID identity unverified |
| SUPERVISOR | SupervisorCheckTask | START_SUPERVISOR | HTTP ping | ping-only (UI claims more) |
| GENOME | GenomeCheckTask | — | `mvn clean install -pl genome -am -DskipTests` at repo root | hub update exceptions swallowed |
| PERMISSIONS | PermissionsCheckTask | — | write+delete test files in log/runtime/build | ok |
| CREATE_EVO_PROJECT | CreateEvoProjectTask | START_EVO | HTTP POST /server/project/create | ok, 2 retries |
| CREATE_INFERENCE_TASK | CreateInferenceTask | CREATE_EVO_PROJECT | HTTP POST /task | ok, 2 retries |
| ANALYTIC_CHAT_VALIDATION | AnalyticChatValidationTask | CREATE_INFERENCE_TASK | HTTP poll | ok |
| PROMOTE | PromoteTask | START_EVO | copy artifact → export/promoted | new BuildArtifact, no checksum |
| ROLLBACK | RollbackTask | — | stop evo+supervisor; restore runtime/evo from promoted[0]; git reset/clean on isolated copy | **always SUCCESS; exceptions swallowed (36-41, 84, 89-100)** |
| LLM | LlmCheckTask | — | HTTP GET :11434/api/tags | **connection error → SUCCESS (35-38)** |
| SUPERVISOR_LOOP | SupervisorLoopTask | START_SUPERVISOR | single ping | no loop |
| SELF_DEV_LOOP | SelfDevLoopTask | — | single ping | no loop |

### Registration count
- **34 tasks registered** (`registerTasks()`). UI displays 28 rows. Registered-but-hidden: GIT, BUILD, MAVEN, EXPORT, DEPLOY, STOP_SUPERVISOR.
- Aliases with identical semantics: BUILD≡BUILD_EVO, MAVEN≡MAVEN_EVO, EXPORT≡EXPORT_EVO, DEPLOY≡DEPLOY_EVO.

---

## 2. Graph B — External supervisor process (World B)

```
[Parent RCP] SupervisorRuntime.start (c/.../selfdev/SupervisorRuntime.java:17-140)
  fast-path: isAlive() && ping() -> SUCCESS "already running" (27-32)   <-- historical defect
  -> java -jar supervisor.jar <projectRoot> --port=28480(+10) --control-port=28580(+10) --headless

[Supervisor JVM] SupervisorMain.main
  EVOSupervisorControlServer (28480) — endpoints switch (210-251):
    /ping /build /export /start-evo /stop-evo /git-check /maven-check ...
  handleStartEvo (606-929): native evo | equinox launcher | runnable jar
    portOffset = this.port - 28480; portToUse = findAvailablePort(28080+offset)  (677-678)
    waitFor(3s) liveness only (702-712, 774-784)   <-- no HTTP/OSGi/bundle readiness
    writes command.json {"action":"RESTART"} (917-920) -> legacy monitor relaunches EVO
  SelfDevSupervisor.run (s/.../SelfDevSupervisor.java:55-90): while(true) sleep(2000)
    executeTaskLifecycle (124-269): EVO_RUNNING -> BUILDING -> TESTING -> RUNNING -> VERIFYING -> COMPLETED
      EVO child: ProcessRunner.runRCP (133-156) -> SelfDevMain --mode=SELF_DEV
      BUILD: ProcessRunner.runBuild (62-128) `mvnw clean verify -DskipTests -Plinux`  exit code only
      TEST:  ProcessRunner.runTests (134-167) `mvn test` (only non-skipping path)
      VERIFY: EvoProductValidator.validateProduct (226-250)
    repair loop: maxTaskAttempts=3, maxRepairAttempts=3, maxConsecutiveFailures=3 (SupervisorState.java:17-19)
    NO overall iteration cap; NO wall-clock limit
```

---

## 3. Graph C — Darwin in-process chain (authoritative orchestration)

```
[Chat UI] AiCommanderPage.startSelfDevAction (v/.../AiCommanderPage.java:1007-1063)
  -> OrchestratorServiceImpl.submit (107-108)
     -> KernelFacade.handle
        -> KernelFactory.create (PromptIntentAnalyzer -> ModeRecognizer) (35-44)
        -> DarwinEngineFactory.createEngine(SELF_DEV_MODE) -> SelfDevelopmentEngine extends ADarwinEngine
        -> new IterationManager(...) (KernelFactory.java:128-129)
     [ModeRouter.resolveFlow: SELF_DEV_MODE -> DarwinFlow]
     -> ADarwinEngine.evolve (544-740)
        while(true):
          safetyCounter >= maxIterationsLimit || paused || cancelled -> break (577, 583-590)
          runDarwinIteration (748+): goal -> EvolutionPhaseMachine -> GoalModel -> dimension/variant generation
            -> TaskExecutor (EMF-model tasks, NOT the SelfDevTask registry)
            -> Evaluator.evaluateWithSnapshot: Maven clean install + test pass rate (Evaluator.java:214-287)
            -> decision CONTINUE/ROLLBACK/STOP; saveFullCheckpoint (632)
          terminal phase -> break (659-673)
[CLI child entry] SelfDevMain --mode=SELF_DEV (c/.../orchestration/SelfDevMain.java:21-126)
  -> KernelFacade.handle -> result.json -> exit
```

Termination inventory (verified): safety counter (`getMaxIterationLimit` 5..40, ADarwinEngine.java:3107-3153), pause/cancel, terminal/SATISFIED phase, exception → rethrow. **Missing**: wall-clock limit, no-change/stall detection; build/test failure is NOT a termination condition (Evaluator.java:259-268 CONTINUE).

---

## 4. START_EVO defect chain (the ~4 ms false success) — exact evidence

```
1. SupervisorRuntime.start fast path (c/.../selfdev/SupervisorRuntime.java:27-32)
   if (isAlive() && activeClient.ping())  -> SUCCESS "Supervisor is already running..."
   isAlive() = static volatile supervisorProcess || uiRcpSupervisorProcess (12-13, 211-214)
   ping() = GET /ping 2s timeout, code==200 (SupervisorClient.java:47-59)  -> ms-level
2. StartSupervisorTask.run (StartSupervisorTask.java:66-86)
   records TaskResult.success("START_EVO", "EVO RCP product started via Supervisor command...")
   BEFORE checking evoRes.isSuccess(); even on failure returns SUCCESS (79-86)
3. SelfDevOrchestrator result reuse (SelfDevOrchestrator.java:196-200)
   any later START_EVO request returns the cached forged SUCCESS without executing
4. Dependency fabrication (SelfDevOrchestrator.java:221-229)
   missing/failed START_EVO dep satisfied by copying START_EVO_SUPERVISOR success
```

Consequence chain: stale supervisor endpoint ⇒ START_EVO_SUPERVISOR SUCCESS in ms ⇒ START_EVO forged SUCCESS ⇒ PROMOTE / CREATE_EVO_PROJECT / CREATE_INFERENCE_TASK run "after START_EVO" with no EVO child of the current run ever launched. Current-run artifact never verified: no jar-path check on fast path, no run-id comparison, no PID comparison, no port expectation vs actual.

---

## 5. Ready-state semantics actually implemented

| Level | Implemented? | Evidence |
|---|---|---|
| PROCESS_STARTED | yes | ProcessBuilder.start + 1.5 s alive (`EvoRcpRuntime.java:179-187`) |
| PROCESS_ALIVE | yes | `isAlive()` poll (`EvoRcpVerifier.java:24-27`) |
| HTTP_RESPONSIVE | partial | GET /server/status 200 + body RUNNING (`EvoRcpVerifier.java:252-268`) |
| SUPERVISOR_RESPONSIVE | yes (supervisor only) | /ping 200 (`SupervisorClient.java:47-59`) |
| OSGI_RESOLVED | partial | GET /server/osgi, `eu.kalafatic.*` unresolved → FAIL (87-157); exceptions ignored (129-131) |
| REQUIRED_BUNDLES_ACTIVE | NO | only resolved=false check |
| APPLICATION_INITIALIZED | partial | log markers / readiness file (`EvoRcpVerifier.java:226-250`) |
| APPLICATION_HEALTHY | NO | no health check beyond status body |
| Highest-level reporting | NO | result message only; no readiness-level field |
| Fallback | — | **timeout + alive ⇒ SUCCESS** (`EvoRcpVerifier.java:64-82`) |

---

## 6. Port flow (as executed)

```
Production ports: HTTP 28080, DEVELOP 28380, Supervisor 28480, Control 28580 (PortRegistry.java:16-31)
Debug offset: +10 (PortRegistry.java:41)
Central application: SelfDevContext.getPortOffset/getEffective*Port (371-391) — debug only
  -> SupervisorRuntime cmd: -Dport=<28480+o> -Dcontrol.port=<28580+o> --port --control-port (63-69)
  -> EvoRcpRuntime direct: context.getEffectiveServerPort() (79,99) — NO availability check
Supervisor-side re-derivation: portOffset = this.port - 28480; child = findAvailablePort(28080+offset)
  (SupervisorMain.java:677-678, 746-747, 889-890)
UI supervisor debug scheme: +1 ports (SupervisorRuntime.java:102-103)
findAvailablePort: startPort..+49 scan; returns startPort if none free (silent fallback; racy)
  (SupervisorMain.java:964-972, duplicated ProcessRunner.java:169-177)
Hardcoded fallbacks: SupervisorClient.java:40 (28480); SupervisorMain.java:16-18; ProcessRunner.java:201,369 (28080, ignores offset)
```

---

## 7. Git execution graph (as executed)

```
GitCheckTask (per repo): exists -> .git -> revision/branch/remote -> updateRepository
  updateRepository: remote exists? pull --ff-only : "No remote" -> SUCCESS skip (GitSourceProvider.java:74-79)
                   pull fail -> fetch origin -> SUCCESS (80-95)
  status --porcelain: FAILED -> fail; CHANGED -> PASS (GitCheckTask.java:226)
CopySourceTask -> GitSourceProvider copy (excludes self-dev-run)
RollbackTask -> new GitManager(preparedReactor).rollback(null)
  = git reset --hard HEAD + git clean -fd on ISOLATED copy (GitManager.java:198-206)
  exceptions swallowed (RollbackTask.java:84) -> SUCCESS
ADarwinEngine commits/rollbacks -> IterationManager GitManager rooted at context.getProjectRoot()
  = canonical repo in self-dev (KernelFactory.java:47-60,88; SelfDevContext.java:87)
  rollback = reset --hard HEAD + clean -fd on CANONICAL REPO (ADarwinEngine.java:1187-1194, 1593-1595, 1786-1800)
  AtomicFlow rollback on any exception (AtomicFlow.java:20-22)
Supervisor applyPatch: reset --hard + clean -fd + git apply (ProcessRunner.java:273-309) in supervisor baseDir
Other parallel git integrations: GitTool/ShellTool, GitVersionControlProvider, EclipseGitEvoTool(JGit),
  GitServiceImpl, GitCommitCommandHandler, OrchestrationCommandHandler heuristic push, PeerReviewService commit+push
No baseline capture anywhere. No commit-eligibility gate. Push gated only in DevelopSession (canWrite/canPush).
```

---

## 8. Failure-propagation matrix (who blocks whom)

| Upstream failure | Downstream effect (actual) |
|---|---|
| GIT_EVO pull failure | fetch fallback → SUCCESS → COPY proceeds |
| COPY failure | BUILD_* BLOCKED (dep) |
| BUILD_EVO failure | EXPORT_* BLOCKED |
| EXPORT_EVO artifact missing | re-runs BUILD internally; if still missing → FAILED → DEPLOY blocked |
| DEPLOY_EVO failure | START_EVO BLOCKED |
| START_EVO readiness timeout | **SUCCESS if process alive** → PROMOTE / CREATE_* proceed |
| START_EVO_SUPERVISOR start-evo HTTP failure | **START_EVO recorded SUCCESS; task returns SUCCESS** |
| Supervisor /ping timeout | waitUntilReady FAILED (SupervisorRuntime.java:250) |
| ROLLBACK any step failure | swallowed → SUCCESS |
| LLM endpoint down | SUCCESS (fallback) |
| Tests (never run) | — |

---

## 9. Where the loop is NOT closed today

1. Darwin analysis/modification (World C) never feeds the isolated task pipeline (World A) programmatically.
2. External supervisor (World B) never receives objectives from Darwin; its queue is only self-repair.
3. No iteration ledger chains: objective → run id → revision → build id → artifact id → PID → readiness → commit → result.
4. No second-iteration safety: stale artifacts (`zips[0]`), stale processes (static handles, no PID files), stale ports (no availability checks) can silently satisfy a new iteration.
