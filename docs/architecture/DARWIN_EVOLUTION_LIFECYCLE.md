# DARWIN EVOLUTION LIFECYCLE

> Architecture reference for the Darwin evolutionary process (controller bundle).
> Companion validation documents: `docs/testing/DARWIN_EVOLUTION_BASELINE.md`,
> `docs/testing/DARWIN_EVOLUTION_TEST_PLAN.md`, `docs/testing/DARWIN_EVOLUTION_VALIDATION_REPORT.md`.

## 1. Components and ownership

| Component | Responsibility | Key source |
|---|---|---|
| `OrchestratorServiceImpl` | Session/turn entry, continuation handling (approval/input), log propagation | `orchestration/OrchestratorServiceImpl.java` |
| `KernelFacade` | Reuse/create the session's `IterationManager`; engine replacement on mode change | `orchestration/KernelFacade.java` |
| `KernelFactory` | Builds the kernel control plane (GitManager, TaskPlanner, TaskExecutor, Evaluator, engine, capabilities) | `orchestration/KernelFactory.java` |
| `DarwinEngineFactory` | Mode → engine class mapping (10 PlatformTypes) | `selfdev/DarwinEngineFactory.java` |
| **`IterationManager`** | **Sole authority for `SystemState` transitions**; decision orchestration, clarification, satisfaction review, checkpoints, lineage records | `orchestration/IterationManager.java` |
| `ADarwinEngine` | Evolutionary loop (`evolve`), one iteration (`runDarwinIteration`), candidate generation, parallel objective evaluation, winner execution and lineage persistence | `selfdev/ADarwinEngine.java` |
| `SiblingGenerationManager` / `DynamicSiblingGenerator` / `TrajectoryTerritoryMapper` / `DarwinVariantSpawner` | Sequential sibling blueprint discovery and materialization (LLM) with dedup | `selfdev/*` |
| `DarwinVariantValidator` (+`autoRepair`) | Structural validation of materialized variants | `selfdev/DarwinVariantValidator.java`, `DarwinVariantSpawner.autoRepair` |
| `DarwinFitnessRanker` | Predicted quality score (correctness/simplicity/extensibility/performance/maintainability) | `selfdev/DarwinFitnessRanker.java` |
| `Evaluator` + `FitnessEngine` | Objective evaluation: LIGHT (static), MEDIUM (compile), HEAVY (full `mvn` + test pass rate); score `0.8 + 0.2*passRate` on success, `passRate*0.5` on failure | `selfdev/Evaluator.java`, `orchestration/engines/FitnessEngine.java` |
| `KernelScheduler` | Budget/backpressure-driven scheduling of evaluated variants into the competition | `execution/KernelScheduler.java` |
| `AuthorityController` / `DecisionResolver` / `ActivationResolver` | Multi-policy winner resolution (CriticalFailure, HighestScore, coherence, complexity, stability policies) | `supervision/*` |
| `IterationMemoryService` | Persistence: records, trajectory analyses, evolution tree, checkpoints, audit trail (`orchestrator/memory/`) | `selfdev/IterationMemoryService.java` |
| `EvolutionaryTrajectoryEngine` + `StabilityAnalyzer` + `EvolutionPhaseMachine` | Phase progression, convergence/stagnation, continuation decisions | `trajectory/*`, `orchestration/EvolutionPhaseMachine.java` |
| `EvolutionProgressPublisher` | UI observability events (EVOLUTION_PROGRESS, branches, winner) | `orchestration/EvolutionProgressPublisher.java` |

## 2. Evolutionary state machine

### 2.1 System states (`SystemState`, owned by `IterationManager.transition`)

```
INIT → ANALYZING → EXECUTING → VERIFYING → DONE
  ↑                    ↓
  └── MUTATING / CLARIFYING / WAITING_FOR_USER_DECISION / AWAITING_BRANCH_SELECTION
FAILED, DONE are terminal: they only accept INIT / RECOVERING afterwards.
```

- `IterationManager.transition(SystemState, TaskContext)` is the single entry point.
- Illegal transitions from terminal states are logged and rejected (state unchanged).
- Every transition publishes `RuntimeEventType.SUPERVISOR_STATUS_CHANGED` with
  `fromState`/`toState` metadata and updates the current EMF iteration status.

### 2.2 Phase machine (`EvolutionPhase`)

```
INTENT_EXPANSION → ARCHITECTURE_VARIANTS → SELECTION_REFINEMENT → IMPLEMENTATION_PLAN
  → FINAL_SYNTHESIS → DESIGN_SATISFIED → TERMINAL_SUCCESS
```

- `EvolutionPhaseMachine.validateTransition` forbids backward/terminal re-entry.
- `EvolutionaryTrajectoryEngine.determineNextPhase` applies intensity-based skipping
  (ARCHITECTURE_VARIANTS requires intensity ≥ 3), capability skipping and stability
  acceleration (converged → FINAL_SYNTHESIS).
- `DESIGN_SATISFIED` triggers `IterationManager.handleSatisfactionReview`:
  - auto-approve mode: terminates autonomously (`TERMINAL_SUCCESS`) — no blocking;
  - manual mode: interactive Show / Continue Refinement / Stop Evolution choice.
- `SelfDevDecision` (CONTINUE / STOP / ROLLBACK) is derived from the phase.

### 2.3 Iteration lifecycle (one `runDarwinIteration`)

1. State recovery (terminal → INIT); profile recalculation; goal/envelope grounding.
2. Terminal short-circuit (TERMINAL_*/DESIGN_SATISFIED → STOP).
3. INTENT_EXPANSION handling (first iteration): LLM intent expansion, clarification
   strategy, phase advance.
4. `generateProposals`: baseline snapshot → sibling discovery/materialization →
   predicted ranking → **parallel objective evaluation of every non-rejected
   candidate** (isolated git worktree, task execution, LIGHT/MEDIUM/HEAVY gates) →
   scheduling → trajectory-analysis persistence.
5. `awaitApproval`: auto-approve picks max objective score; manual mode pauses in
   `WAITING_FOR_USER_DECISION` (candidates preserved in `pending_candidates`).
6. `IterationManager.decide`: multi-policy resolution → winner ACTIVE, others
   REJECTED/KEPT.
7. `transition(EXECUTING)` → `executeWinner`:
   - viability gate: `score >= 0.3` and no validation `errorMessage` (a candidate
     that fails mandatory validation cannot be promoted);
   - git snapshot branch → winner re-evaluation → reality check → HEAVY gate;
   - **lineage persistence**: the winner is identified by ID (its activation state
     has already advanced to VERIFIED/SCORING during re-evaluation) and persisted as
     an ACTIVE `IterationRecord` with its score, plus tree winner mark, winner
     fitness, `WINNER_SELECTED` event, genome dimension lock;
   - merge winner branch, commit.
8. Result handling: next phase decision, iteration counter, satisfaction review,
   phase confirmation, `completeIteration`, `transition(DONE)`.
9. Failure: `handleIterationFailure` — up to 3 retries (phase reset to
   SELECTION_REFINEMENT), then ROLLBACK.

### 2.4 Candidate lifecycle (`BranchVariant.ActivationState`)

```
CREATED → ANALYZING → PLANNED → SCORING → RECOMMENDED → [EXECUTING → VERIFIED → SCORING]
  → ACTIVE (winner) | KEPT (diversity survivor) | REJECTED | ARCHIVED
```

- Selection sets ACTIVE/KEPT/REJECTED (`IterationManager.applyDecision`).
- Objective re-evaluation advances the winner through EXECUTING → VERIFIED →
  SCORING; lineage persistence therefore keys on the winner's **id**, not its state.

### 2.5 Trend, motivation and stop conditions

- Trend evidence: `Trajectory.fitnessHistory` (per decision and per git
  re-evaluation), `TrajectoryAnalysisRecord` per candidate per iteration, winner
  score on `IterationRecord` and `EvolutionNode`.
- Convergence: `generation >= minEvolutionaryDepth` AND (`stability >=
  convergenceThreshold` OR diminishing returns, gain < 0.02).
- Progression: converged → advance; ARCHITECTURE_VARIANTS advances at generation ≥ 2;
  otherwise after `maxEvolutionaryDepth` (default 8).
- Loop exit: iteration budget (`maxIterationsLimit`), user pause/cancel, decision ≠
  CONTINUE, mediated success, terminal phase, user-decision wait.
- No separate "motivation" module: continuation is the composition of iteration
  budget + phase machine + stability policy + user gates. Stagnation raises
  evolutionary pressure (`DimensionEngine.handleStagnation`) to force exploration.

## 3. Persistence and recovery

`orchestrator/memory/` under the session's project root:
`iteration_<n>_<ts>.json` (lineage records incl. winner score), `trajectory_*.json`
(per-candidate scores), `evolution_tree.json` (nodes, current winner, winner flags,
fitness), `checkpoint_<sessionId>.json` (phase, iteration count, records, tree,
metadata), `audit_trail.jsonl`.

`IterationManager` restores from `SelfDevSession` state or the session checkpoint at
construction; the best-known winner (ACTIVE record + tree `currentWinnerId`) and its
evidence survive application restart. An interrupted iteration does not overwrite
the last successfully persisted winner (winner persistence happens only after the
heavy reality gate passes).

## 4. Multi-instance model

- Engine instances: `PlatformType` (10) → engine class via `DarwinEngineFactory`.
  All extend `ADarwinEngine` and share the evolutionary loop.
- One authoritative `ADarwinEngine` per session (`DarwinEngineRegistry`,
  sessionId-keyed); `IterationManager.engineMap` retains per-platform engines across
  mode switches.
- Cross-instance competition = independent sessions/engines evaluated against
  identical criteria; winner selection depends only on evaluation evidence, never on
  instance identity.

## 5. Confirmed invariants (validation-enforced)

1. Only `IterationManager.transition` changes `SystemState` (enforced; illegal
   transitions rejected).
2. The kernel path (`OrchestratorServiceImpl → KernelFacade → KernelFactory →
   IterationManager → ADarwinEngine`) is the only orchestration path.
3. Every non-rejected candidate is objectively evaluated with the same deterministic
   suite; predicted scores cannot win over objective evidence (unevaluated synthetic
   variants are now evaluated; sub-threshold/error-marked winners are refused).
4. Best-known solution (ACTIVE record + tree winner + working tree) is preserved;
   inferior or invalid candidates are never promoted.
5. All persisted scores/statuses originate from actual execution data.
