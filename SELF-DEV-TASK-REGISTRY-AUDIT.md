# SELF-DEV TASK REGISTRY AUDIT (WP0)

Authoritative registry: `SelfDevOrchestrator.taskRegistry` — `c/orchestration/selfdev/SelfDevOrchestrator.java:15` (plain `HashMap`), populated exclusively by `registerTasks()` (`SelfDevOrchestrator.java:48-164`). No other production registration site exists. `getTaskRegistry()` returns unmodifiable view (166-168) and is **never consumed by the UI**.

## 1. Inventory: 34 registered tasks

| # | ID | Class | Dependencies | Registered |
|---|---|---|---|---|
| 1 | GIT | GitCheckTask | — | 115 |
| 2 | GIT_EVO | GitCheckTask | — | 116 |
| 3 | GIT_SUPERVISOR | GitCheckTask | — | 117 |
| 4 | COPY | CopySourceTask | — | 119 |
| 5 | COPY_SUPERVISOR | CopySourceTask | — | 120 |
| 6 | BUILD | BuildEvoTask | COPY | 122 |
| 7 | BUILD_EVO | BuildEvoTask | COPY | 123 |
| 8 | MAVEN | BuildEvoTask | COPY | 124 |
| 9 | MAVEN_EVO | BuildEvoTask | COPY | 125 |
| 10 | BUILD_SUPERVISOR_LOCAL | BuildSupervisorTask | COPY_SUPERVISOR | 128 |
| 11 | BUILD_SUPERVISOR | BuildSupervisorTask | BUILD_SUPERVISOR_LOCAL | 127 |
| 12 | MAVEN_SUPERVISOR | BuildSupervisorTask | — | 129 |
| 13 | EXPORT | ExportEvoTask | BUILD_EVO | 131 |
| 14 | EXPORT_EVO | ExportEvoTask | BUILD_EVO | 132 |
| 15 | EXPORT_SUPERVISOR | ExportSupervisorTask | BUILD_SUPERVISOR_LOCAL | 133 |
| 16 | DEPLOY_EVO | DeployEvoTask | EXPORT_EVO | 136 |
| 17 | DEPLOY | DeployEvoTask | EXPORT_EVO | 135 |
| 18 | START_EVO | StartEvoTask | DEPLOY_EVO | 138 |
| 19 | STOP_EVO | StopEvoTask | — | 139 |
| 20 | START_SUPERVISOR | StartSupervisorTask | EXPORT_SUPERVISOR | 140 |
| 21 | START_EVO_SUPERVISOR | StartSupervisorTask | START_SUPERVISOR, DEPLOY_EVO | 141 |
| 22 | STOP_SUPERVISOR | StopSupervisorTask | — | 142 |
| 23 | STOP_EVO_SUPERVISOR | StopSupervisorTask | — | 143 |
| 24 | SUPERVISOR | SupervisorCheckTask | START_SUPERVISOR | 145 |
| 25 | GENOME | GenomeCheckTask | — | 146 |
| 26 | PERMISSIONS | PermissionsCheckTask | — | 147 |
| 27 | CREATE_EVO_PROJECT | CreateEvoProjectTask | START_EVO | 149 |
| 28 | CREATE_INFERENCE_TASK | CreateInferenceTask | CREATE_EVO_PROJECT | 150 |
| 29 | ANALYTIC_CHAT_VALIDATION | AnalyticChatValidationTask | CREATE_INFERENCE_TASK | 151 |
| 30 | PROMOTE | PromoteTask | START_EVO | 158 |
| 31 | ROLLBACK | RollbackTask | — | 159 |
| 32 | LLM | LlmCheckTask | — | 161 |
| 33 | SUPERVISOR_LOOP | SupervisorLoopTask | START_SUPERVISOR | 162 |
| 34 | SELF_DEV_LOOP | SelfDevLoopTask | — | 163 |

Historical reference: the audit brief mentioned "24 tasks". The current code registers **34**. The discrepancy with the UI (28 rows) is resolved as: 6 tasks hidden (GIT, BUILD, MAVEN, EXPORT, DEPLOY, STOP_SUPERVISOR), and the UI never reads the registry.

## 2. Dependency graph

DAG with topological recursion (`SelfDevOrchestrator.java:216-248`). Verified acyclic by inspection; **no automated cycle check at registration/preflight** — cycles only detected mid-recursion via `visitingStack` (207-211), and only after result-cache reuse checks.

```
GIT*                     (independent)
COPY -> (none)
COPY_SUPERVISOR -> (none)
BUILD/BUILD_EVO/MAVEN/MAVEN_EVO -> COPY
BUILD_SUPERVISOR_LOCAL -> COPY_SUPERVISOR
BUILD_SUPERVISOR -> BUILD_SUPERVISOR_LOCAL
MAVEN_SUPERVISOR -> (none)
EXPORT/EXPORT_EVO -> BUILD_EVO
EXPORT_SUPERVISOR -> BUILD_SUPERVISOR_LOCAL
DEPLOY/DEPLOY_EVO -> EXPORT_EVO
START_EVO -> DEPLOY_EVO
STOP_EVO -> (none)
START_SUPERVISOR -> EXPORT_SUPERVISOR
START_EVO_SUPERVISOR -> START_SUPERVISOR, DEPLOY_EVO
STOP_SUPERVISOR / STOP_EVO_SUPERVISOR -> (none)
SUPERVISOR -> START_SUPERVISOR
CREATE_EVO_PROJECT -> START_EVO
CREATE_INFERENCE_TASK -> CREATE_EVO_PROJECT
ANALYTIC_CHAT_VALIDATION -> CREATE_INFERENCE_TASK
PROMOTE -> START_EVO
SUPERVISOR_LOOP -> START_SUPERVISOR
GENOME, PERMISSIONS, ROLLBACK, LLM, SELF_DEV_LOOP -> (none)
```

Graph anomalies:
- `PROMOTE -> START_EVO` only: promotion requires nothing about build/export/test provenance — only a (possibly forged) START_EVO result.
- `MAVEN_SUPERVISOR` and `GIT*`/`GENOME`/`ROLLBACK`/`SELF_DEV_LOOP` are orphan root tasks executed ad hoc; nothing chains a full pipeline end-to-end from a single root.
- `START_EVO` is not depended on by `STOP_EVO` (fine), but the semantic STOP-before-START ordering is nowhere enforced.

## 3. ID aliasing (same-semantics duplicates)

| Group | IDs | Same class | Same deps |
|---|---|---|---|
| EVO build | BUILD, BUILD_EVO, MAVEN, MAVEN_EVO | BuildEvoTask | COPY |
| EVO export | EXPORT, EXPORT_EVO | ExportEvoTask | BUILD_EVO |
| EVO deploy | DEPLOY, DEPLOY_EVO | DeployEvoTask | EXPORT_EVO |

No ID is registered twice with *different* semantics — but the registry is a `HashMap` with **no duplicate-ID detection**, so any future double-`put` silently overwrites.

## 4. Semantics audit (per audit requirements)

| Requirement | Actual |
|---|---|
| One authoritative ID per task | YES (34 unique IDs), but 4 alias groups pollute identity |
| One implementation per task | YES per ID; same class reused across IDs |
| Declared dependencies | YES (10 tasks have none) |
| Clear result | TaskResult with message/error/diagnostics — but several tasks return SUCCESS without real verification |
| Progress from actual work | HTTP tasks yes; Maven tasks yes; loop tasks no; check tasks partial |
| Cancellation support | `cancel()` API exists but **never invoked**; mid-execution interruption absent |
| Deterministic failure semantics | NO — LLM-check fallback SUCCESS, START_EVO_SUPERVISOR failure→SUCCESS, ROLLBACK always SUCCESS, verifier alive-timeout SUCCESS |
| Cycle prevention | Partial (runtime-only) |
| Invalid dependencies block execution | YES at runtime (unknown task → failure; failed dep → BLOCKED) — but preflight "Valid DAG" is cosmetic |
| Duplicate registration detected | NO |
| Reproducible order | Dependency recursion yes; UI row order user-editable and independent |

## 5. Status model

- Controller enum (authoritative for the pipeline): `READY, RUNNING, SUCCESS, FAILED, BLOCKED, SKIPPED` (`TaskStatus.java:3-10`).
- Missing from audit requirement: `NOT_EXECUTED` (only as message text, `SelfDevOrchestrator.java:237`), `WARNING` (nonexistent), `CANCELLED` (exists only in the supervisor bundle enum and the unrelated EMF enum).
- SKIPPED carries a reason via `TaskResult.skipped(id, reason)` (TaskResult.java:47-49) — compliant in design; UI renders SKIPPED as red FAIL (DevelopmentPage.java:598, 1008).

## 6. Task honesty register (false-success list)

| Task | Defect | Evidence |
|---|---|---|
| START_EVO_SUPERVISOR | records `START_EVO` SUCCESS before checking `start-evo` result; returns SUCCESS on failure | StartSupervisorTask.java:69-71, 79-86 |
| START_EVO (via orchestrator) | forged from START_EVO_SUPERVISOR success | SelfDevOrchestrator.java:221-229 |
| ROLLBACK | exceptions swallowed; final SUCCESS regardless of restore result | RollbackTask.java:36-41, 84, 89-100 |
| LLM | endpoint failure → SUCCESS "fallback" | LlmCheckTask.java:35-38 |
| SUPERVISOR_LOOP / SELF_DEV_LOOP | no loop, single ping | SupervisorLoopTask.java:10-30; SelfDevLoopTask.java:10-30 |
| START_EVO readiness | timeout + process alive → SUCCESS | EvoRcpVerifier.java:64-82 |
| START_SUPERVISOR | existing-some-supervisor ping → SUCCESS w/o current-run/artifact check | SupervisorRuntime.java:27-32 |
| GIT / GIT_EVO / GIT_SUPERVISOR | pull failure → fetch → SUCCESS; no-remote → SUCCESS | GitSourceProvider.java:74-95 |
| GENOME | hub update exceptions swallowed; build-only success | GenomeCheckTask.java:61-70 |
| BUILD*/EXPORT* (as test gate) | tests never executed (`-DskipTests` everywhere) | BuildEvoTask.java:11; TychoEvoRcpBuilder.java:23,99-101,218-219; MavenSupervisorBuilder.java:29,37; GenomeCheckTask.java:51,53 |

## 7. Executor paths

1. UI row → `SelfDevBootstrapController.runTask` → `SelfDevOrchestrator.executeTaskWithDependencies` (registry path).
2. `SelfDevOrchestrator.startBootstrap()` → `supervisorLifecycle.start(context)` (bypasses registry; used by SupervisorGroup start and (dead) controller SelfDevSupervisor).
3. `TaskExecutor` (selfdev package) → EMF-model tasks via `EvolutionOrchestrator` (Kernel path; unrelated to registry).
4. Supervisor bundle HTTP endpoints → `SupervisorMain` handlers (own subprocess world).

## 8. Recommendations (minimal, deferred to plan)

- Keep 34 tasks or merge aliases: decision for WP2 — **collapse alias IDs into canonical ones** (BUILD_EVO, EXPORT_EVO, DEPLOY_EVO) after verifying no external consumers depend on alias IDs (none found: only UI/tests reference; alias IDs not shown in UI).
- Add duplicate-ID detection at `put`; add static cycle validation in `registerTasks`/preflight (real DAG check).
- Extend TaskStatus with CANCELLED (and map SKIPPED reason); keep WARNING optional.
- Make `SupervisorLoopTask`/`SelfDevLoopTask` either real loops with bounded budgets or rename them to honest ping checks.
- Enforce preflight before any pipeline execution (orchestrator-level, not UI-level).
