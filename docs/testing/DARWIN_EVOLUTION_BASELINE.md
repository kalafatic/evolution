# DARWIN EVOLUTION BASELINE

> Baseline of the existing EVO Darwin Engine evolutionary process, captured **before** any
> validation-driven modification. Companion documents: `DARWIN_EVOLUTION_TEST_PLAN.md`,
> `DARWIN_EVOLUTION_VALIDATION_REPORT.md`, `docs/architecture/DARWIN_EVOLUTION_LIFECYCLE.md`.

Date of capture: 2026-11-21. Repository: `eu.kalafatic.evolution.controller` (main bundle).

---

## 1. Execution path (submission → evolution)

```
OrchestratorServiceImpl.handle/submit  (session + continuation handling)
  └─ KernelFacade.handle               (session kernel reuse, mode-engine resolution)
       └─ KernelFactory.create         (mode → DarwinEngineFactory → IDarwinEngine, builds IterationManager)
            └─ IterationManager.handle
                 └─ ADarwinEngine.orchestrateEvolution(TaskRequest, IterationManager)
                      ├─ intent routing / control-command handling
                      ├─ discovery (git repo → structure/reality snapshot)
                      ├─ GoalModel grounding, locked abstraction level, execution profile
                      └─ evolve(...)   ← the iterative evolutionary loop
                           └─ runDarwinIteration(context, manager)   ← one evolutionary iteration
                                ├─ phase resolution (EvolutionPhaseMachine)
                                ├─ INTENT_EXPANSION handling (iteration 0)
                                ├─ generateProposals
                                │    ├─ evaluateWithSnapshot (baseline build+test snapshot)
                                │    ├─ generateVariants
                                │    │    ├─ TrajectoryTerritoryMapper (LLM: sibling blueprint)
                                │    │    ├─ DarwinVariantSpawner       (LLM: materialize variant JSON)
                                │    │    ├─ DarwinVariantValidator → autoRepair fallback
                                │    │    ├─ semantic-distance gating   (REJECTED_SEMANTIC)
                                │    │    └─ DarwinFitnessRanker        (predicted score)
                                │    ├─ parallel candidate evaluation (evaluateVariantParallel)
                                │    │    ├─ isolated dir (git worktree / copy)
                                │    │    ├─ TaskPlanner.generateTasksFromVariant → WRITE actions
                                │    │    ├─ task execution (pre-generated code written via FileTool)
                                │    │    ├─ LIGHT reality gate (brace balance)
                                │    │    ├─ MEDIUM reality gate (mvn compile)
                                │    │    └─ HEAVY evaluation (mvn clean install → test pass rate → score)
                                │    ├─ KernelScheduler scheduling (ScheduledExecutionPlan)
                                │    └─ TrajectoryAnalysisRecord persistence
                                ├─ awaitApproval (autoApprove → max score; else WAITING_FOR_USER_DECISION)
                                ├─ IterationManager.decide → AuthorityController → DecisionResolver → ActivationResolver
                                │    └─ applyDecision (winner ACTIVE, others REJECTED/KEPT)
                                ├─ transition EXECUTING → executeWinner
                                │    ├─ git snapshot branch + winner branch
                                │    ├─ winner re-evaluation (evaluateVariantParallel)
                                │    ├─ reality check, HEAVY fitness gate
                                │    ├─ IterationRecord ACTIVE/KEPT persistence, EvolutionTree winner mark
                                │    └─ merge winner branch, commit
                                ├─ re-mediation (mediated mode)
                                └─ result handling:
                                     ├─ determineNextPhase (EvolutionaryTrajectoryEngine + StabilityAnalyzer)
                                     ├─ DESIGN_SATISFIED → handleSatisfactionReview (user decision)
                                     ├─ phase decision → SelfDevDecision.CONTINUE | STOP
                                     ├─ completeIteration (progress publisher)
                                     └─ transition DONE / FAILED
                      └─ loop exit: safetyCounter >= maxIterations | pause/cancel | decision != CONTINUE
                                  | mediated success | terminal/SATISFIED phase | WAITING_FOR_USER_DECISION
```

## 2. Responsibility map (who owns what)

| Responsibility | Owner | Location |
|---|---|---|
| Session/turn lifecycle, continuation | `OrchestratorServiceImpl` | `orchestration/OrchestratorServiceImpl.java` |
| Kernel reuse per session, engine creation on mode change | `KernelFacade` / `KernelFactory` | `orchestration/KernelFacade.java`, `orchestration/KernelFactory.java` |
| Engine per mode | `DarwinEngineFactory` | `selfdev/DarwinEngineFactory.java` |
| **State transitions (sole authority)** | `IterationManager.transition` | `orchestration/IterationManager.java:434` |
| Phase transitions | `EvolutionPhaseMachine` (+ `EvolutionaryTrajectoryEngine.determineNextPhase`) | `orchestration/EvolutionPhaseMachine.java` |
| Iteration loop | `ADarwinEngine.evolve` | `selfdev/ADarwinEngine.java:544` |
| One iteration | `ADarwinEngine.runDarwinIteration` | `selfdev/ADarwinEngine.java:748` |
| Candidate generation | `generateVariants` → `SiblingGenerationManager`/`DynamicSiblingGenerator` → `TrajectoryTerritoryMapper` + `DarwinVariantSpawner` | `selfdev/*` |
| Candidate validation | `DarwinVariantValidator` (+ `autoRepair`) | `selfdev/DarwinVariantValidator.java`, `selfdev/DarwinVariantSpawner.java:55` |
| Predicted scoring | `DarwinFitnessRanker` | `selfdev/DarwinFitnessRanker.java` |
| Objective evaluation | `evaluateVariantParallel` → `Evaluator` (real `mvn clean install` + test parse) | `selfdev/ADarwinEngine.java:1840`, `selfdev/Evaluator.java` |
| Final score | `FitnessEngine.calculateScore`: success ? `0.8 + passRate*0.2` : `passRate*0.5` | `orchestration/engines/FitnessEngine.java:12`, `selfdev/Evaluator.java:313` |
| Scheduling | `KernelScheduler` (ISchedulingContract) | `execution/KernelScheduler.java` |
| Winner selection | `IterationManager.decide` → `AuthorityController.decide` → `DecisionResolver.resolveWinner` → `ActivationResolver.resolve` | `orchestration/IterationManager.java:1714`, `supervision/*` |
| Winner execution + persistence | `executeWinner` | `selfdev/ADarwinEngine.java:1451` |
| Iteration records / checkpoint / tree persistence | `IterationMemoryService` (JSON under `orchestrator/memory/`) | `selfdev/IterationMemoryService.java` |
| Trend/pressure | `EvolutionaryPressureEngine`, `Trajectory.fitnessHistory`, `EvolutionaryTrajectoryEngine`, `DefaultStabilityPolicy` | `kernel/`, `trajectory/` |
| Progress observability | `EvolutionProgressPublisher` (EVOLUTION_PROGRESS RuntimeEvent + UI message) | `orchestration/EvolutionProgressPublisher.java` |
| Engine instance registry (UI access) | `DarwinEngineRegistry` (sessionId → ADarwinEngine) | `selfdev/DarwinEngineRegistry.java` |

## 3. Candidate identity, scoring, competition

- **Population size**: `branchingLimit = getMaxBranchingLimit(...)` – min 2, max 4 (hard cap 8); per-iteration, driven by expansion level.
- **Candidate identity**: id generated per sibling (`bp-iterN-sK-<ts>` blueprints, variant id from materialized JSON), recorded in `EvolutionTree` nodes and `TrajectoryAnalysisRecord`.
- **Predicted score** (`DarwinFitnessRanker`): correctness 40% / simplicity 20% / extensibility 20% / performance 10% / maintainability 10%, semantic alignment blend, pressure sigmoid, scope gate (score 0 if scope ratio > 4).
- **Objective score**: after real build+test in an isolated directory the predicted score is **overwritten** by `FitnessEngine.calculateScore` (ADarwinEngine.java:2083). In `testMode`/mediated/chat paths the score is **hardcoded to 0.95** and no build runs (ADarwinEngine.java:1873-1875, 1903-1905).
- **Selection**: `ActivationResolver` merges policy scores with signal boost; KEPT boost; winner = top ranked id. `applyDecision` marks winner ACTIVE, others REJECTED (or KEPT).
- **Failed candidates**: score `0.1` on LIGHT/MEDIUM failure with no physical results; execution exceptions → score `0.0` + REJECTED. A winner with score < 0.3 is refused in `executeWinner` (line 1535-1538).

## 4. Iterations, continuation, motivation

- Loop: `evolve()` while(true): `safetyCounter` vs `maxIterationsLimit = 5 + (expansion-1)*12` (intensity caps 20/40), min iterations 1 (2 for non-chat).
- Iteration counter is written in **three** places: `evolve()` line 594 (`setIterationCount(safetyCounter)`), `runDarwinIteration` line 1014 (+1), `handleIntentExpansionPhase` line 1114 (+1).
- Phase machine: INTENT_EXPANSION → ARCHITECTURE_VARIANTS → SELECTION_REFINEMENT → IMPLEMENTATION_PLAN → FINAL_SYNTHESIS → DESIGN_SATISFIED → TERMINAL_SUCCESS. Intensity-based skipping (`ARCHITECTURE_VARIANTS` needs intensity ≥ 3).
- Continuation decision: `result.getDecision()` (CONTINUE vs STOP from phase machine); terminal phases stop; `StabilityAnalyzer`/`DefaultStabilityPolicy` controls progression (convergence at `generation >= 3 && stability >= 0.95` or diminishing returns `< 0.02` gain; max generation 8).
- Satisfaction review (`IterationManager.handleSatisfactionReview`, line 1039): blocks on `context.requestInput(...).get()` **unconditionally** (no auto-approve shortcut) — in automated/auto-approve mode this stalls unless an input listener answers.
- **No explicit cross-iteration "motivation" module exists**; continuation = iteration limits + phase machine + stability policy + user gates.

## 5. Trend, persistence, multi-instance

- Trend evidence: `Trajectory.fitnessHistory` (appended in `ActivationResolver.updateTrajectoryMetrics` and during git re-evaluation), `TrajectoryAnalysisRecord` (per candidate per iteration), `EvolutionMemoryGraph.entropyHistory` (never populated – no callers of `recordEntropy`), checkpoint JSON with entropy/convergence fields.
- Persistence: `orchestrator/memory/iteration_*.json`, `trajectory_*.json`, `evolution_tree.json`, `checkpoint_<session>.json`, `audit_trail.jsonl`.
- Multi-instance: one `ADarwinEngine` instance per session (`DarwinEngineRegistry`), one `IterationManager.engineMap` keyed by `PlatformType`. Engine classes: DarwinEngine (DARWIN_MODE), ChatEngine (SIMPLE_CHAT), CodingEngine/CodingEngine2 (ASSISTED_CODING), DeveloperEngine (DEVELOPER), MediatedEngine (HYBRID_MANUAL_EXPORT), SelfDevelopmentEngine (SELF_DEV_MODE), IntentReconstructionEngine (INTENT_RECONSTRUCTION), MutationEngine (MUTATION), LLMDarwinEngine (FORGE), DreamEngine (DREAM_MODE). Cross-instance competition is per-session; no engine pool competes within one session.

## 6. Known baseline defects (pre-validation observations)

1. Winner fitness never stored on `EvolutionNode` → lineage branch-revival logic inert.
2. Iteration counter multi-writer clobbering (see §4).
3. Predicted ranking is discarded (only objective score survives) – no record of predicted vs actual.
4. Degenerate competition when implementation validation is disabled (all candidates get 0.95).
5. `entropyHistory` / `convergenceReasoning` never populated.
6. `handleSatisfactionReview` blocks unconditionally in auto mode.
7. No cross-iteration best-so-far gate: an inferior winner in iteration N+1 can overwrite a better winner from iteration N.
8. Unused engine fields (`selectionEngine`, `penaltyModel`, `diversityController` in ADarwinEngine).
