# SELF-DEV DEFECT REGISTER (WP0)

Convention: `D-<nn>` stable IDs; severity C=Critical, H=High, M=Medium, L=Low; status: OPEN / IN_PROGRESS / FIXED / PARTIALLY_FIXED / BLOCKED. Evidence paths abbreviated: `c/`=controller bundle, `s/`=supervisor bundle, `v/`=view bundle, `m/`=model bundle.

---

## Group 1 — False success / dishonest state (C)

| ID | Severity | Title | Evidence | Impact | Status |
|---|---|---|---|---|
| D-01 | C | START_EVO ~4 ms SUCCESS "Supervisor is already running" without current-run verification | `c/.../selfdev/SupervisorRuntime.java:27-32` (fast path), `:12-13` (static Process handles), `SupervisorClient.java:47-59` (ping-only), `StartSupervisorTask.java:69-71` (START_EVO recorded before result check), `:79-86` (failure swallowed), `SelfDevOrchestrator.java:196-200` (result reuse), `:221-229` (dependency fabrication) | Pipeline proceeds with no child RCP of the current run; downstream PROMOTE/CREATE_* consume forged success | PARTIALLY_FIXED |
| D-02 | C | Readiness timeout downgraded to SUCCESS when process merely alive | `c/.../selfdev/EvoRcpVerifier.java:64-82` | START_EVO "succeeds" with zero HTTP/OSGi/application evidence | FIXED |
| D-03 | C | Tests are skipped in every self-dev build path | `BuildEvoTask.java:11`; `TychoEvoRcpBuilder.java:23,99-101,218-219`; `MavenSupervisorBuilder.java:29,37`; `GenomeCheckTask.java:51,53`; `s/ProcessRunner.java:81,99`; `s/bootstrap/RcpBuildTool.java:28` + `BuildConfiguration.java:11` | No build in the pipeline ever proves tests pass; "verify" goal is hollow | FIXED |
| D-04 | C | ROLLBACK always returns SUCCESS, exceptions swallowed | `RollbackTask.java:36-41,84,89-100` | Rollback failures invisible; known-good restore failure reported as success | FIXED |
| D-05 | C | LLM check returns SUCCESS on endpoint failure | `LlmCheckTask.java:35-38` | Gating check is meaningless | FIXED |
| D-06 | C | Loop tasks are single pings, not loops | `SupervisorLoopTask.java:10-30`; `SelfDevLoopTask.java:10-30` | The named "autonomous loop" does not exist as a task | FIXED |
| D-07 | H | UI batch always reports 100% complete regardless of failures | `v/.../DevelopmentPage.java:1021,1023`; debug reset erases failures `:832` | Single green indicator hides intermediate failures | FIXED |
| D-08 | H | GIT check passes dirty trees and downgrades pull failures to fetch+SUCCESS | `GitCheckTask.java:226`; `GitSourceProvider.java:74-95` | Iteration proceeds on unknown divergence; baseline semantics broken | FIXED |

## Group 2 — Artifact provenance (C)

| ID | Severity | Title | Evidence | Impact | Status |
|---|---|---|---|---|
| D-09 | C | No artifact manifest: no checksum/size/run/build ID | `BuildArtifact.java:9-23`; `SelfDevContext.java:57` (in-memory only) | Cannot prove BUILD→EXPORT→START chain uses the same artifact | FIXED |
| D-10 | C | Artifact identity re-derived per task with first-match picks | `EvoRcpRuntime.java:36-44` (`zips[0]`); `StartEvoTask.java:52-85` (`archives[0]`); `MavenSupervisorBuilder.java:94-126` (`jars[0]`); `s/EvoProductBuildService.java:43-91` (`findFirst`); `TychoEvoRcpBuilder.getArtifact` re-scan `:302-334` | Stale/wrong artifact can be silently selected; nondeterministic | FIXED |
| D-11 | H | EXPORT re-runs the full Maven build when product missing | `TychoEvoRcpBuilder.java:214-235` | Double builds, hidden build failures inside export, unbounded cost | FIXED |
| D-12 | H | BUILD records directory artifact; EXPORT re-locates and re-records a different zip artifact | `TychoEvoRcpBuilder.java:147-170` vs `:244-290` | Provenance break between build and export records | FIXED |
| D-13 | H | Supervisor reactor build runs in canonical repo root | `MavenSupervisorBuilder.java:31-39`; `GenomeCheckTask.java:45-53` | Build mutates the git repository (target/), violating isolation | FIXED |
| D-14 | H | ExportSupervisorTask never writes to export dir; only runtime copy | `SupervisorDeployer.java:28-35`; `ExportSupervisorTask.java:46-49` | EXPORT semantics broken for supervisor | FIXED |
| D-15 | M | UI shows commands that are not executed | `DevelopmentPage.java:364-377` (`-Pexport-product`, `-Dproject.build.directory`) vs real `validate clean verify -P<platform>` | Operator misinformation | FIXED |

## Group 3 — Process ownership & lifecycle (C)

| ID | Severity | Title | Evidence | Impact | Status |
|---|---|---|---|---|
| D-16 | C | No PID files / on-disk ownership records; no shutdown hooks | `SelfDevContext.java:43` (in-memory); `SupervisorRuntime.java:12-13` (static); `s/SupervisorMain.java:161` (static); grep: no `.pid` anywhere | New parent session cannot identify children; orphans after parent exit | FIXED |
| D-17 | C | Static supervisor handles leak across runs | `SupervisorRuntime.java:12-13` + fast path `:27-32` | New run adopts old run's supervisor and forges START_EVO | FIXED |
| D-18 | C | STOP kills by PID with no identity verification | `SupervisorRuntime.java:257,279-291` | PID reuse could kill unrelated process | FIXED |
| D-19 | C | Graceful shutdown endpoint unimplemented | `SupervisorRuntime.java:272,285` (`shutdown` cmd) vs switch `s/SupervisorMain.java:210-251` (no case) | Graceful STOP is dead code; force-kill always | FIXED |
| D-20 | H | Supervisor-side readiness = 3 s liveness | `s/SupervisorMain.java:702-712,774-784` | No HTTP/OSGi/application verification on supervisor launch path | FIXED |
| D-21 | H | No descendants kill on supervisor side | `s/SupervisorMain.java:667,858,946` (destroyForcibly only) | Launcher-forked grandchildren leak | FIXED |
| D-22 | H | command.json RESTART causes duplicate EVO launch | `s/SupervisorMain.java:917-920` + `s/SelfDevSupervisor.java:82-87,442-487` + `ProcessRunner.java:369` | Two EVO children, double ports, untracked process | FIXED |
| D-23 | H | EVO fast path "already running" on fresh instance is ineffective (per-task instance) | `EvoRcpRuntime.java:10,20-25`; `StartEvoTask.java:8` | Second run launches duplicate EVO on same port with no availability check | FIXED |
| D-24 | M | ProcessOwnership enum never used for decisions | `SelfDevContext.java:49-54`; sole use `RollbackTask.java:99` | Ownership model exists but is cosmetic | FIXED |
| D-25 | M | `findAvailablePort` returns busy startPort silently; racy | `s/SupervisorMain.java:964-972`; `s/ProcessRunner.java:169-177` | Collision risk | FIXED |

## Group 4 — Port isolation (H)

| ID | Severity | Title | Evidence | Impact | Status |
|---|---|---|---|---|
| D-26 | H | +10 offset re-derived independently supervisor-side | `s/SupervisorMain.java:677-678,746-747,889-890` (`this.port - 28480`) vs central `PortRegistry.java:41` + `SelfDevContext.java:371-391` | Two formulas can diverge; child binds wrong port | FIXED |
| D-27 | M | Third port scheme (+1) for debug UI supervisor | `SupervisorRuntime.java:102-103` | Undocumented offset variety | FIXED |
| D-28 | M | Hardcoded port fallbacks ignoring offset | `SupervisorClient.java:40`; `s/ProcessRunner.java:201,369` | Wrong-port requests | FIXED |
| D-29 | M | UI shows default ports, not allocated ports | `DevelopmentPage.java:379-380,1128-1129`; `SupervisorGroup.java:98,267-268` | Misleading commands if PortAllocationManager reassigns | FIXED |

## Group 5 — Resource & workspace (H)

| ID | Severity | Title | Evidence | Impact | Status |
|---|---|---|---|---|
| D-30 | H | Preflight fallbacks collapse build/export/log onto repo root | `SelfDevPreflight.java:61-66` + `ResourceManager.java:136-141,196-202` | Preflight could write-test inside the git repo | FIXED |
| D-31 | H | Snapshot installed even when preflight BLOCKED | `SelfDevPreflight.java:276-291`; `SelfDevOrchestrator.java:42-44` | Only the view gate (DevelopmentPage.java:838-844) prevents use; other paths don't check | FIXED |
| D-32 | H | Run ID = second-resolution timestamp, no uniqueness check; silent mkdirs merge | `SelfDevContext.java:89-90,233-237` | Two runs within a second share directories | FIXED |
| D-33 | H | Legacy `self-dev-run` writers still active | `c/.../ServerManager.java:405,438-442`; `SelfDevProtocol.java:19-23`; readiness readers `EvoRcpVerifier.java:238-250`; view log paths `SupervisorGroup.java:460-462`; fallbacks `DevelopmentPage.java:409,416` | Readiness contract depends on child CWD; mixed layouts | PARTIALLY_FIXED |
| D-34 | M | Configured supervisor/llm repo paths bypass canonical validation + variable expansion | `SelfDevPreflight.java:152-155`; `GitCheckTask.java:74,83`; `SelfDevContext.resolvePath(File,String)` no expandVariables `:158-168` | Config typos produce wrong paths silently | FIXED |
| D-35 | M | `/app` and view-discovered projectRoot accepted without canonical-root validation | `SelfDevContext.java:75-87`; `v/.../ProjectManager.java:133-234` | Canonical-repo guarantee weakened | FIXED |
| D-36 | M | Zip-slip unprotected in 3 of 4 unzip implementations | `EvoRcpDeployer.java:170-190`; `s/EvoProductValidator.java:277-300`; `s/SupervisorMain.java:970-997` (safe: `TychoEvoRcpBuilder.java:883-891`) | Path traversal from crafted archive | FIXED |
| D-37 | L | `ProjectModelManager.migratePath` rewrites any path containing "supervisor" | `c/.../ProjectModelManager.java:1236-1266` | Legitimate paths rewritten | OPEN |
| D-38 | L | Supervisor jar may come from repo target/ | `SupervisorRuntime.java:383-384` | Repo build output consumed | FIXED |
| D-39 | H | Preflight not enforced by all execution paths | `DevelopmentPage.java:595,1007` (no preflight) vs `:834-847` (debug only) | Malformed environment proceeds to builds | FIXED |

## Group 6 — Git & rollback (C)

| ID | Severity | Title | Evidence | Impact | Status |
|---|---|---|---|---|
| D-40 | C | Engine-level `reset --hard HEAD` + `clean -fd` can hit canonical repo | `KernelFactory.java:47-60,88` + `SelfDevContext.java:87` + `ADarwinEngine.java:1187-1194,1593-1595,1786-1800`; `AtomicFlow.java:20-22`; `GitManager.java:198-206` | Destroys pre-existing user work | FIXED |
| D-41 | C | No iteration baseline captured anywhere | `GitCheckTask.java:198-250` (revision stored only as artifact metadata); no stash/tag/tree-save | Rollback cannot restore pre-iteration state | FIXED |
| D-42 | H | Supervisor patch application discards local modifications unconditionally | `s/ProcessRunner.java:281-298` | Destroys uncommitted work in supervisor baseDir | FIXED |
| D-43 | H | RollbackTask restores arbitrary promoted artifact (`files[0]`, no ordering/validation) | `RollbackTask.java:124-136` | Wrong known-good restored | FIXED |
| D-44 | H | No commit-eligibility gate; `--allow-empty` commits | `GitManager.java:98-99,174-185`; `ADarwinEngine.java:1774-1778` | Commits without build/test/runtime validation | FIXED |
| D-45 | H | Un-gated pushes in multiple paths; master protection configurable/bypassable | `GitTool.java:196-199`; `GitVersionControlProvider.java:173-176`; `PeerReviewService` commit+push `:108-109`; `OrchestrationCommandHandler.java:243-251`; `GitCommitCommandHandler.java:53-60`; `EclipseGitEvoTool.java:986-1001` | Unauthorized remote mutation possible | FIXED |
| D-46 | M | 5+ parallel git integrations with divergent policies | see SELF-DEV-COMPONENT-MAP.md §9 | Inconsistent protection | PARTIALLY_FIXED |
| D-47 | M | GitTool silently retargets working dir | `GitTool.java:138-151` | Operations hit unexpected repo | FIXED |
| D-48 | L | String-built git commands (injection via branch names) | `GitManager.java:142,167,212-215` | Argument injection/breakage | FIXED |
| D-49 | L | `ensureInitialCommit` mutates any repo (config + commit) | `GitManager.java:68-105` | Unintended commits | PARTIALLY_FIXED |

## Group 7 — Darwin loop (H)

| ID | Severity | Title | Evidence | Impact | Status |
|---|---|---|---|---|
| D-50 | H | External supervisor loop unbounded | `s/SelfDevSupervisor.java:55,89` (`while(true)`) | Uncontrolled iteration | FIXED |
| D-51 | H | Build/test failure is not a termination condition in Darwin | `Evaluator.java:259-268` (CONTINUE on failure) | Loop continues past broken builds | FIXED |
| D-52 | H | No wall-clock / no-change / stall detection in loops | `ADarwinEngine.java:577-673`; `CognitiveBudget` wired only to dataset flow `KernelFacade.java:84-88` | Resource exhaustion possible | FIXED |
| D-53 | H | No objective→task-queue wiring for supervisor | only `createRepairTask` appends `s/SelfDevSupervisor.java:333` | Supervisor never receives user objectives | FIXED |
| D-54 | M | SelfDevDecision lacks granular states (analysis/code/tests/build/deploy/verify/accepted) | `m/orchestration/SelfDevDecision.java:29-49` (CONTINUE/ROLLBACK/STOP) | Cannot distinguish stage completion | PARTIALLY_FIXED |
| D-55 | M | Dead code: controller SelfDevSupervisor (zero callers), SelfDevDarwinEngine (100% commented), supervisor IterationManager, EclipseGitRepositoryManager, AbstractBootstrapTask | respective files | Confusion + drift risk | PARTIALLY_FIXED |
| D-56 | L | SelfDevSession default maxIterations = 0 | `m/orchestration/impl/SelfDevSessionImpl.java:94` | Dead-loop variant would run 0 iterations | FIXED |

## Group 8 — UI observability (M)

| ID | Severity | Title | Evidence | Impact | Status |
|---|---|---|---|---|
| D-57 | M | UI hardcodes 28 rows; registry (34 tasks) not consumed; 6 tasks hidden | `DevelopmentPage.java:361-392` vs `SelfDevOrchestrator.java:48-164` | Divergence guaranteed | FIXED |
| D-58 | M | SKIPPED renders as red FAIL; lowercase "ready" breaks styling; STOPPED uncolored | `DevelopmentPage.java:598,1008,822,1042,1048,1051,555` | Wrong signals | FIXED |
| D-59 | M | Required observability fields absent (run/iteration/build/artifact IDs, git revision, PID, ports, readiness, rollback state) | `DevelopmentPage.java:107-116,488` | Operator blind | PARTIALLY_FIXED |
| D-60 | M | NPE risk when bootstrapController null | `DevelopmentPage.java:595,1007,878` (creation gated `:203-206`) | Row stuck "running" | FIXED |
| D-61 | M | Background-thread SWT access (getShell) + stopSelected UI-thread session shutdown | `DevelopmentPage.java:841,1040` | Race/dispose + UI hang | FIXED |
| D-62 | M | SupervisorGroup creates new controller per action; pid labels never updated | `SupervisorGroup.java:376,393,431,95` | Split context | FIXED |
| D-63 | L | Deprecated global OrchestrationStatusManager used | `DevelopmentPage.java:590,602,1004,1021` | Legacy singleton | PARTIALLY_FIXED |

## Group 9 — Status model (M)

| ID | Severity | Title | Evidence | Impact | Status |
|---|---|---|---|---|
| D-64 | M | Three distinct TaskStatus enums | `c/.../selfdev/TaskStatus.java:3-10`; `m/orchestration/TaskStatus.java:20-109`; `s/TaskStatus.java:3-20` | Mapping errors | PARTIALLY_FIXED |
| D-65 | L | Controller enum lacks WARNING/CANCELLED/NOT_EXECUTED | `TaskStatus.java:3-10` | Audit-required statuses missing | PARTIALLY_FIXED |

## Group 10 — Preflight (H)

| ID | Severity | Title | Evidence | Impact | Status |
|---|---|---|---|---|
| D-66 | H | "Valid DAG" claim without graph analysis | `SelfDevPreflight.java:423-440` (esp. `:438`) | False assurance | FIXED |
| D-67 | M | Duplicated preflight logic (preflight vs GitCheckTask vs PermissionsCheckTask vs ensureDirectories) | `SelfDevPreflight.java:94-183,223-227,297-318`; `GitCheckTask.java:94-125,171-195`; `PermissionsCheckTask.java:40-68`; `SelfDevContext.java:225-231` | Drift | OPEN |
| D-68 | M | TARGET_PLATFORM check can never fail (fallback chain) | `SelfDevPreflight.java:229-239` + `ResourceManager.java:351-375` | Misleading check | OPEN |

---

## Summary counts

- Critical: 12 (D-01..D-06, D-09, D-10, D-16..D-19, D-40, D-41)
- High: 20
- Medium: 16
- Low: 6
- Total: 68 open defects.

No fixes applied during WP0. Remediation sequencing: see `SELF-DEV-REFACTORING-PLAN.md`.
