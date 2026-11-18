# SELF-DEV REFACTORING PLAN (WP0 → WP1+)

Derived from the verified audit (SELF-DEV-ARCHITECTURE-AUDIT.md, SELF-DEV-DEFECT-REGISTER.md). Principle: **smallest targeted changes; preserve architecture; one authority per responsibility; no false success; no destructive git; verify everything**.

## 1. Remediation principles

1. Repair in place — no rewrites, no new parallel managers.
2. Every fix pairs with a focused test (existing test suite: `eu.kalafatic.evolution.controller.tests` + supervisor bundle tests).
3. Truth before automation: fix provenance/state BEFORE wiring loops.
4. Safe-by-default git: never destructive on the canonical repo; baseline capture before any iteration.
5. Honest status: remove every false-SUCCESS path before adding new features.

## 2. Work package order and scope

### WP1 — Resource & workspace truth (D-30..D-39)
1. `ResolvedSelfDevResources`: make it the ONLY path source for tasks; drop context-field dual-sourcing (`SelfDevContext.java:243-325`) — after preflight, all getters read the snapshot; without preflight, task execution is refused (see WP2).
2. Blocked snapshot must not be installed: `SelfDevOrchestrator.executePreflightInternal` (`:42-44`) only installs when `res.isSuccess()`.
3. Run ID: append uniqueness token (e.g., `run_<ts>_<seq|uuid-short>`); refuse (not merge) if run dir already exists (`SelfDevContext.java:89-90,225-237`).
4. Remove preflight fallbacks onto repo root (`SelfDevPreflight.java:61-66`): missing build/export/log config → BLOCKED with explicit field name.
5. Add `expandVariables` to `SelfDevContext.resolvePath(File,String)` (`:158-168`); route supervisor/llm configured paths through canonical validation (`SelfDevPreflight.java:152-155`, `GitCheckTask.java:74,83`).
6. Migrate legacy `self-dev-run` writers/readers onto the run-dir layout or a single stable readiness contract: `ServerManager.java:405,438-442`, `SelfDevProtocol.java:19-23`, `EvoRcpVerifier.java:238-250` (keep CWD-relative probe as fallback only), view log paths.
7. Zip-slip guards in `EvoRcpDeployer`, `EvoProductValidator`, `SupervisorMain` unzip (align with `TychoEvoRcpBuilder.unzipSafely`).
8. Validate view-supplied projectRoot through `ResourceManager.validateCanonicalPath` before constructing context (`SelfDevContext.java:75-87`).
9. Tests: run-ID uniqueness, snapshot-only paths, blocked-snapshot refusal, malformed config → BLOCKED, zip-slip rejection.

### WP2 — Task registry, graph, preflight (D-64..D-68, D-57, D-06)
1. Collapse alias IDs to canonical (`BUILD_EVO`, `EXPORT_EVO`, `DEPLOY_EVO`) after confirming zero production consumers of aliases; keep one-line compat shims only if a consumer is found.
2. Duplicate-ID detection at registration (throw/assert); static DAG validation (cycle + unknown dependency) in `registerTasks` and in `SelfDevPreflight.validateTaskGraph` (replace the fake "Valid DAG").
3. Extend controller `TaskStatus` with `CANCELLED`; keep SKIPPED reason. Wire `cancel()` into execution loop (check flag between stages; interrupt HTTP/Maven waits).
4. Orchestrator-level preflight gate: `executeTaskWithDependencies` verifies a successful preflight result exists for the run; otherwise BLOCKED with reason (covers runSelected/single-row paths).
5. Result-cache honesty: forged START_EVO (`:221-229`) removed; cached SUCCESS reuse only valid for pure checks (`GIT`, `PERMISSIONS`, `SUPERVISOR`); process-affecting tasks always re-verify.
6. Replace `SupervisorLoopTask`/`SelfDevLoopTask` single-ping semantics: either implement bounded loop (WP8) or rename to honest `SUPERVISOR_PING`/`SELF_DEV_LOOP_STATUS`; UI text updated accordingly.
7. Tests: duplicate ID rejection, cycle detection, unknown dependency, preflight gate, cancellation, no-forged-START_EVO.

### WP3 — Build pipeline (D-03, D-13, D-11, D-15)
1. Introduce an explicit `SelfDevBuildPolicy` (tests: ON for iteration acceptance, OFF only for explicit smoke builds) — remove hardcoded `skipTests=true` defaults (`TychoEvoRcpBuilder.java:23`, `BuildEvoTask.java:11`); keep a `-Dselfdev.skipTests` escape hatch, never the default.
2. Supervisor reactor build moves from canonical repo root to the isolated copy (`MavenSupervisorBuilder.java:31-39`, `GenomeCheckTask.java:45-53`) — same guard as EVO build (`BuildEvoTask.java:28-31`).
3. Remove build-inside-export (`TychoEvoRcpBuilder.java:214-235`): EXPORT must fail with explicit "artifact missing; run BUILD_EVO" instead.
4. Fix UI command text to match executed commands (`DevelopmentPage.java:364-377,768`).
5. Tests: skipTests default false, reactor-isolation assertion, export-fails-without-build, UI text (if applicable).

### WP4 — Artifact manifest & provenance (D-09, D-10, D-12, D-14, D-43)
1. Extend `BuildArtifact` with: runId, buildId (run-scoped monotonic), revision, size, sha-256, createdAt (artifact timestamp), productId, os/arch, buildResult. Persist per-run manifest `run_<id>/export/artifact-manifest.json` (or `logs/`) — single writer: BUILD/EXPORT record; all consumers read.
2. One artifact resolver: consume only the manifest entry recorded by BUILD/EXPORT; remove `zips[0]`/`archives[0]`/`jars[0]`/`findFirst` fallbacks in `EvoRcpRuntime`, `StartEvoTask`, `MavenSupervisorBuilder`, `EvoProductBuildService`. Missing/mismatched manifest → FAILED/BLOCKED.
3. Checksum verification at DEPLOY/START (compare manifest hash before unzip/launch).
4. `PromoteTask` copies the recorded artifact AND its manifest entry (checksum preserved).
5. `RollbackTask.findPreviousKnownGoodArtifact` selects newest valid promoted entry with matching checksum, not `files[0]`.
6. Fix `ExportSupervisorTask` to write a real artifact record into the export dir.
7. Tests: manifest round-trip, stale-artifact rejection, checksum mismatch rejection, promoted selection determinism.

### WP5 — Supervisor process ownership & lifecycle (D-16..D-25, D-28)
1. Introduce per-run ownership record file `run_<id>/runtime/process-ownership.json` (PID, parent PID, executable, command line, working dir, runtime path, start time, effective ports, run id, supervisor instance id, exit status) — written on launch, updated on stop; consumed by STOP and by fast-path checks.
2. Remove static process handles from `SupervisorRuntime` (`:12-13`); replace with ownership-record-driven discovery scoped to the current run (plus same-run PID verification via `ProcessHandle.info().commandLine()`/`startInstant()`).
3. Fast path correction (`SupervisorRuntime.java:27-32`): "already running" accepted ONLY if the ownership record matches current run + expected artifact + ports + PID alive + ping.
4. Implement `/shutdown` on the supervisor HTTP server (`SupervisorMain.java:210-251`); `SupervisorRuntime.stop` uses graceful → bounded force escalation, records result.
5. Kill-by-PID identity verification before termination (`SupervisorRuntime.java:279-291`): command-line + run record match; else refuse and report.
6. Supervisor-side descendants kill (`SupervisorMain.handleStopEvo`); remove command.json RESTART duplicate-launch path (`:917-920`) or make it idempotent.
7. Add shutdown hooks in parent controller for owned children (or documented child-owned termination protocol).
8. Tests: ownership record lifecycle, stale-PID rejection, fast-path negative cases, graceful shutdown, parent-protection invariants.

### WP6 — RCP readiness (D-02, D-20, D-23)
1. `EvoRcpVerifier`: timeout ⇒ FAILED (never SUCCESS-on-alive). Readiness levels as explicit enum (PROCESS_STARTED, PROCESS_ALIVE, HTTP_RESPONSIVE, SUPERVISOR_RESPONSIVE, OSGI_RESOLVED, REQUIRED_BUNDLES_ACTIVE, APPLICATION_INITIALIZED, APPLICATION_HEALTHY); result carries highest achieved level.
2. Require OSGi resolution + required-bundle-active + application-initialized before START_EVO SUCCESS (configurable minimum level: default APPLICATION_INITIALIZED).
3. Supervisor-side launch readiness uses the same HTTP/OSGi checks (not 3 s liveness) — reuse verifier contract via shared messages; implement minimal `/server/status`+`/server/osgi` probe in supervisor bundle if not present.
4. Port availability check before direct EVO launch (`EvoRcpRuntime.java:79,99`): refuse on occupied port.
5. Tests: readiness-level matrix, alive-not-ready ⇒ FAILED, occupied-port refusal.

### WP7 — Port isolation (D-26, D-27, D-29)
1. Single offset authority: controller passes explicit child ports (`-Devo.http.port`, `-Devo.supervisor.port`, `-Devo.control.port`) to the supervisor command; supervisor consumes them instead of re-deriving `this.port - 28480` (`SupervisorMain.java:677-678` etc.).
2. Fix `findAvailablePort` fallback-to-busy-port: report conflict, let caller fail or allocate via `PortAllocationManager`.
3. Replace hardcoded fallbacks (`SupervisorClient.java:40`, `ProcessRunner.java:201,369`) with configured/effective ports.
4. Document the +1 debug-UI scheme or fold it into the same offset table.
5. Tests: end-to-end port propagation (config → env → bound socket), no-collision, parent ports untouched.

### WP8 — Darwin iteration loop (D-50..D-56)
1. Wire objective → run: a single entry (`SelfDevBootstrapController.startIteration(objective)` backed by existing Darwin chain via `OrchestratorServiceImpl`) creates/validates the run and feeds the external supervisor queue (new `POST /tasks` endpoint or file-protocol task file) — no second iteration manager.
2. Bounded loop: explicit max iterations, max wall-clock, repeated-identical-failure and no-change detection, resource checks; every termination carries a reason (state persisted).
3. Make `Evaluator` failure terminal-or-configurable: build/test failure stops iteration (CONTINUE only when policy allows).
4. Extend decision recording (iteration ledger JSON under run dir) with stage completions: ANALYSIS, CODE_GENERATED, TESTS_PASSED, BUILD_PASSED, EXPORT_PASSED, DEPLOYED, VERIFIED, ACCEPTED.
5. Delete/neutralize dead code (`SelfDevDarwinEngine.java`, dead controller `SelfDevSupervisor`, supervisor `IterationManager`) only after confirming zero references; else leave with `@Deprecated`.
6. Tests: termination conditions, stage-ledger updates, no dual-iteration-manager regression.

### WP9 — Git lifecycle & rollback (D-40..D-49)
1. Baseline capture: on run start record (branch, HEAD, status --porcelain, stash-free) into run dir; iteration diff computed against baseline; pre-existing changes NEVER included in self-dev diffs.
2. Engine GitManager root fix (`KernelFactory.java:47-60,88`): in self-dev, root the session GitManager at the isolated source copy, never the canonical repo; assert canonical-repo protection in a guard (deny reset/clean on canonical path).
3. Replace destructive `reset --hard`/`clean -fd` semantics in `GitManager.rollback` with baseline-restore (checkout paths / apply reverse diff) limited to iteration-owned changes.
4. Supervisor `ProcessRunner.applyPatch`: check cleanliness first; refuse when dirty (or stash with explicit record).
5. Commit eligibility: require SUCCESS from build+test+export (and runtime verification when policy requires) recorded in ledger; commit summary from ledger; never `--allow-empty` for self-dev commits.
6. Push as separate gated operation: remote/branch/upstream/auth verification, divergence check, never force; default policy = prepare commit + pending-push state until explicit authorization.
7. Consolidate: keep `GitTool` as sole CLI executor; `GitManager` as sole self-dev policy facade; deprecate parallel paths (document; no removal without callers audit).
8. Tests: baseline/restore round-trip, user-change protection, canonical-repo protection, commit gate, push gate.

### WP10 — Rollback & recovery (D-04, D-43, D-12)
1. `RollbackTask` honest results: per-step statuses aggregated; FAILED when any critical step fails; restore success verified by checksum + readiness probe of previous known-good.
2. Scoped rollback operations with explicit targets: PROCESS_ONLY / DEPLOYMENT / SOURCE_COPY / GIT_LOCAL / (never automatic remote rewrite; remote revert only as explicit reviewable revert commit).
3. Failure preservation: failing logs/artifact retained under run dir; candidate marked failed; last known good untouched until restore verified.
4. Tests: failed-build rollback, failed-export rollback, failed-startup rollback (stop exact child, restore, verify), git-validation rollback.

### WP11 — UI observability (D-57..D-63, D-07, D-15)
1. Consume registry: DevelopmentPage builds rows from `getTaskRegistry()` (informational-only rows explicitly tagged); preserve user row order as display-only.
2. Status mapping fix: SKIPPED → distinct color; STOPPED colored; READY casing consistent.
3. Add columns: run ID, task deps (tooltip), duration, exit code, build ID, artifact ID, git revision, supervisor state, child PID, effective ports, readiness level, rollback state — fed from TaskResult/context (no invention).
4. Batch result truthful: aggregate status, failures visible; never unconditional 100%.
5. Null-guard bootstrapController; SWT-safe dialog calls; move stopSelected shutdown off UI thread; reuse single controller instance in SupervisorGroup; show allocated ports not defaults.
6. Tests: registry-driven rows, status mapping table, field population.

### WP12 — Full end-to-end verification
Scenarios A–F from the audit brief executed as integration tests with an isolated test repository (never the canonical repo): successful iteration, build failure, invalid export, child startup failure, git failure, repeated iteration (leak detection: exactly one child per run, parent alive, ports unchanged, iteration ledger consistent). Plus fault-injection matrix (each injected failure ⇒ deterministic result).

### WP13 — Performance & reliability report
Measure and record: workspace prep, copy, build, test, export, validation, startup→readiness, shutdown, rollback, total iteration. Identify repeated operations; document safe artifact reuse policy (same revision+config+checksum).

### WP14 — Final acceptance
Execute the full acceptance sequence (ANALYZE→…→PUSH) on a controlled repository and produce `SELF-DEV-FINAL-ACCEPTANCE.md` with actual measured results.

## 3. Sequencing dependencies

```
WP1 (resources) ──> WP2 (graph/preflight) ──> WP3 (build) ──> WP4 (manifest)
                                                      │
WP5 (ownership) <── WP4 (manifest feeds ownership/readiness)
WP6 (readiness) <── WP5
WP7 (ports)    <── WP5/WP6
WP9 (git)      ──> WP10 (rollback)
WP8 (loop)     <── WP4+WP5+WP9 (loop needs provenance+ownership+git safety)
WP11 (UI)      <── WP2+WP4+WP6 (UI needs real data)
WP12 (E2E)     <── WP1..WP11
WP13/WP14      <── WP12
```

## 4. Explicit non-goals

- No rewrite of DevelopmentPage design; no new graph libraries; no Spring/Node/Python; no second supervisor/iteration/resource manager; no modification of ENR/TRYST/DStudio/DCode; no commits/pushes to the repository during this task; no weakening of tests to make builds green; no removal of features to simplify testing.

## 5. Verification policy per package

For each WP: implement → compile affected bundles (Maven/Tycho as per project build) → run focused unit tests → run integration tests → inspect diff → update SELF-DEV-DEFECT-REGISTER.md (state transitions OPEN→FIXED with evidence) → report remaining risks. No success claims without executed verification.
