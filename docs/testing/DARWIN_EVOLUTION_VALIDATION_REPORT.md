# DARWIN EVOLUTION VALIDATION REPORT

> Evidence-based validation of the existing EVO Darwin Engine evolutionary process:
> candidate generation → competition → objective evaluation → winner selection →
> iteration → trend → motivation → continue/stop.
> Companion documents: `DARWIN_EVOLUTION_BASELINE.md` (pre-modification architecture),
> `DARWIN_EVOLUTION_TEST_PLAN.md`, `docs/architecture/DARWIN_EVOLUTION_LIFECYCLE.md`.

Date: 2026-11-22. Engine under test: the REAL implementation
(`KernelFactory` → `IterationManager` → `DarwinEngine`/`ADarwinEngine`) inside the
existing EVO repository. LLM responses were scripted deterministically; ALL
candidate evaluation was REAL (the production `Evaluator` path runs the fixture's
Maven test suite in isolated git worktrees). No scores were fabricated.

---

## 1. Test task (deterministic evolution fixture)

`test-fixtures/darwin-evolution/` — small standalone Maven project (outside the Tycho
reactor):

- **Baseline**: `CollectionProcessor.deduplicate(List<String>)` — functionally
  correct, quadratic duplicate detection (`input.indexOf(item) == i`).
- **Objective evaluation suite** (6 JUnit tests, run by the engine itself):
  5 functional tests (order preservation, null handling, empty input, input
  immutability, new instance) + 1 deterministic complexity test (element-comparison
  counting — quadratic implementations deterministically fail it, > 2500 scans for
  2000 elements; no wall-clock timing).
- Baseline passes 5/6 (fails the complexity test) — the exact optimization target.

## 2. Candidate comparison (observed)

| Iteration | Candidate | Darwin instance | Functional tests | Quality score | Rank | Selected |
|---|---|---|---|---|---|---|
| 1 | `bp-iter1-s0` — Minimal Correction of Collection Deduplication | DARWIN_MODE (scripted) | 5/6 (fails complexity) | 0.42 | 2 | no |
| 1 | `bp-iter1-s1` — Order-Preserving Collection Deduplication Optimization | DARWIN_MODE (scripted) | 6/6 | **1.00** | 1 | **yes** |
| 1 | `bp-iter1-s2` — Normalizing Sort-Based Collection Deduplication | DARWIN_MODE (scripted) | 5/6 (fails order) | 0.42 | 3 | no (dropped by scheduler backpressure; evaluated) |
| 2 | `bp-iter2-s0` — Minimal Correction … | DARWIN_MODE (scripted) | 5/6 | 0.42 | 2 | no |
| 2 | `bp-iter2-s1` — Order-Preserving … Optimization | DARWIN_MODE (scripted) | 6/6 | **1.00** | 1 | **yes** |
| 3 | FINAL_SYNTHESIS candidates (same distribution) | DARWIN_MODE (scripted) | — | winner 1.00 | — | yes |

Scores are the engine's objective formula: build+test success → `0.8 + 0.2*passRate`
(1.0 for 6/6); test failure → `passRate*0.5` (0.4167 ≈ 0.42 for 5/6).

Cross-instance competition (same fixture, same criteria):

| Instance (PlatformType) | Engine class | Winner selected | Score | Winner independent of instance? |
|---|---|---|---|---|
| DARWIN_MODE | `DarwinEngine` | Order-Preserving … Optimization | 1.00 | yes |
| ASSISTED_CODING | `CodingEngine` | Order-Preserving … Optimization | 1.00 | yes |
| MUTATION | `MutationEngine` | Order-Preserving … Optimization | 1.00 | yes |

## 3. Trend table (observed, per iteration)

| Iteration | Best score | Average candidate score | Improvement vs baseline | Regression | Decision |
|---|---|---|---|---|---|
| 1 (INTENT_EXPANSION) | — | — | — | no | CONTINUE |
| 2 (SELECTION_REFINEMENT) | 1.00 | 0.71 (2 presented; 3 evaluated) | yes (5/6 → 6/6) | no | CONTINUE |
| 3 (IMPLEMENTATION_PLAN) | 1.00 | 0.71 | maintained | no | CONTINUE |
| 4 (FINAL_SYNTHESIS) | 1.00 | — | maintained | no | DESIGN_SATISFIED → **STOP** |

Trend evidence persisted: `TrajectoryAnalysisRecord` per candidate per iteration
(score recorded), winner `IterationRecord` with score, `EvolutionTree`
`currentWinnerId` + winner fitness, `Trajectory.fitnessHistory`.

## 4. Scenario results

| Scenario | Expected | Observed | Result |
|---|---|---|---|
| A — continued improvement | improvement detected, new best, trend reflects it | iteration 1 winner improves baseline from 5/6 to 6/6 (0.42-equivalent → 1.00); trend IMPROVING; continuation while budget permits | PASS |
| B — stagnation | no false progress; continuation evaluated; can stop | with a satisfied design the engine stops via DESIGN_SATISFIED; no false progress (scores from real tests) | PASS |
| C — regression | inferior candidates not promoted; previous best preserved | iteration-2 inferior candidates (5/6, 0.42) refused by the viability gate; previous ACTIVE winner (1.00) and its code preserved in records, tree and working tree; iteration fails/rolls back instead of overwriting | PASS |
| D — no valid candidate | no invalid winner; baseline preserved; failure recorded; stop/recovery | compile-broken candidates score 0.0 with validation errors; winner refused; retries then rollback; baseline file unchanged; terminal state | PASS |
| E — successful completion | completion recognized; stop; final best preserved; termination reason | auto-approve satisfaction review terminates autonomously (`TERMINAL_SUCCESS`); state DONE; best candidate ACTIVE | PASS |

## 5. Confirmed defects and fixes (minimal, architecture-aligned)

All defects were reproduced first (see baseline doc §6 and scenario evidence above).

| # | Defect (confirmed) | Root cause | Fix | Regression test |
|---|---|---|---|---|
| D-1 | Winner never persisted (no ACTIVE `IterationRecord`, no tree winner, no `WINNER_SELECTED`) | Winner re-evaluation advances the variant's `ActivationState` to VERIFIED/SCORING before the lineage block, which only persisted ACTIVE/KEPT states | Lineage persistence keys on the winner's **id**: the winner is persisted ACTIVE with its score regardless of transient state; tree node status forced to ACTIVE; node fitness recorded | `DarwinEvolutionValidationTest.testPhasesA_B_C…`, `testPhaseK…` |
| D-2 | Synthetic fallback variants kept their LLM-predicted score (0.9767) and could beat objectively evaluated candidates (incl. a compile-broken one) | `generateProposals` evaluation filter excluded variants without a tree node (`node != null && …`), so synthetic variants were never objectively evaluated | Filter now evaluates every non-`REJECTED_SEMANTIC` candidate: `node == null \|\| !REJECTED_SEMANTIC` | `testScenarioD_AllCandidatesFailValidation` |
| D-3 | Invalid winner promotion gate never triggered (score<0.3 check was disabled for ACTIVE variants; validation errors ignored) | Gate condition `activationState != ACTIVE && score < 0.3` can never fire (decide() marks the winner ACTIVE before execution); errorMessage ignored | Gate now refuses `score < 0.3` or any validation `errorMessage` | `testScenarioD…`, `testScenarioC…` |
| D-4 | Candidates whose build failed tests scored 0.0 instead of the designed `passRate*0.5` (evidence lost) | `Evaluator` parsed the pass rate only in the build-success branch | On build failure with `Tests run:` present, the measured pass rate is parsed and recorded as objective evidence | score assertions (0.42) in `testPhasesA_B_C…` |
| D-5 | Satisfaction review blocked indefinitely in auto-approve/headless mode (`requestInput().get()` with no answerer) | No auto-approve shortcut existed in `handleSatisfactionReview` | Auto-approve terminates autonomously with `TERMINAL_SUCCESS` (manual mode unchanged) | `testScenarioE_CompletionStopsEvolution` |

Also fixed in the harness (not production): scripted-LLM matrix off-by-one and candidate
identity uniqueness; test runner configured with `-Devolution.test.headless=true` and
`redirectTestOutputToFile` (headless console deadlock prevention).

## 6. Multi-instance inventory (100% coverage)

| PlatformType | Engine class | Capability check | Participation |
|---|---|---|---|
| SIMPLE_CHAT | `ChatEngine` | PASS (instantiation + platform type) | UNSUPPORTED for this competition (chat-only by design) |
| ASSISTED_CODING | `CodingEngine` | PASS | Competed — winner correct |
| DARWIN_MODE | `DarwinEngine` | PASS | Competed — winner correct |
| SELF_DEV_MODE | `SelfDevelopmentEngine` | PASS | UNSUPPORTED (self-development pipeline, not fixture evolution) |
| HYBRID_MANUAL_EXPORT | `MediatedEngine` | PASS | UNSUPPORTED (read-only export mode, no code execution) |
| INTENT_RECONSTRUCTION | `IntentReconstructionEngine` | PASS | UNSUPPORTED (intent reconstruction, different objective) |
| FORGE | `LLMDarwinEngine` | PASS | UNSUPPORTED (model forging pipeline, not code evolution) |
| MUTATION | `MutationEngine` | PASS | Competed — winner correct |
| DREAM_MODE | `DreamEngine` | PASS | UNSUPPORTED (background hypothesis generation) |
| DEVELOPER | `DeveloperEngine` | PASS | UNSUPPORTED in harness (autonomous 5-phase coding requires real LLM/tool loops) |

All 10 instances were discovered and individually checked via
`DarwinEngineFactory`; none silently omitted. Registry lifecycle (register on
creation, unregister on session shutdown) verified.

## 7. State machine verification

- Legal transitions verified: INIT→ANALYZING→INIT→EXECUTING→DONE;
  terminal DONE → INIT, RECOVERING.
- Illegal transitions rejected: DONE → ANALYZING / EXECUTING (state unchanged,
  rejection logged). Same-state transition = no-op.
- Transitions persist iteration status (DONE/FAILED/RUNNING) and publish
  `SUPERVISOR_STATUS_CHANGED` events. Ownership: `IterationManager.transition`
  exclusively (enforced by architecture guidelines).

## 8. Persistence and recovery

- Artifacts persisted under `<fixture>/orchestrator/memory/`: `iteration_*.json`
  (winner ACTIVE records with scores), `trajectory_*.json` (per-candidate scores),
  `evolution_tree.json` (nodes, `currentWinnerId`, winner flag, fitness),
  `checkpoint_<session>.json`.
- Restart simulation: engine+manager recreated for the same session — checkpoint
  restored; best-known winner identity, strategy and tree winner survive. An
  interrupted/refused iteration never overwrites the last persisted winner.

## 9. Cost / efficiency / termination (Phase I)

| Metric | Value (observed) | Notes |
|---|---|---|
| Candidate-generating iterations | 3 (phases SELECTION_REFINEMENT → IMPLEMENTATION_PLAN → FINAL_SYNTHESIS) | + 1 INTENT_EXPANSION |
| Candidates materialized per iteration | 3 | scheduler may present 2 under CPU backpressure (adaptive budget) |
| Real objective evaluations | 3 per iteration + winner re-eval + heavy gate | isolated git worktrees |
| Scripted LLM calls | ~9 blueprint + 9 materialization | deterministic mock |
| Real LLM calls | ~1 per dimension discovery (local Ollama smollm:135m) | inference traces logged |
| Wall-clock per scenario | ~50–90 s | real `mvn test` subprocesses |
| Token usage / cost | unavailable (scripted + local) | reported as unavailable |
| Termination explainability | phase machine + stability policy + budget; stop reason recorded (`TERMINAL_SUCCESS` / refusal log / rollback) | PASS |

The engine terminates: (a) on satisfied objective (DESIGN_SATISFIED → STOP), (b) on
iteration budget, (c) on repeated validation failure (3 retries → ROLLBACK). No
fabricated metrics were recorded.

## 10. Frontend / observability (Phase J)

Existing architecture exposes: per-iteration `EVOLUTION_PROGRESS` events (iteration,
phase, branching limits, branches with score/status, winnerId, active model),
`[DARWIN_BRANCHES]` decision payloads (candidates, scores, statuses, winner) rendered
by the existing UI, per-session logs, and persisted records. All displayed
scores/statuses originate from execution data (verified in logs). No new UI was
introduced; a comparison table/trend view is adequately served by the existing
`[DARWIN_BRANCHES]` JSON + progress events.

## 11. Regression check

- New validation suite: 9/9 PASS (standalone and co-run with other classes).
- Existing tests re-run against the fixed controller: `DarwinExecutionLifecycleTest`
  10/10 PASS, `DarwinApprovalGateTest` PASS, `DarwinDiagnosticTest` PASS,
  `DarwinVariantValidatorTest` PASS, `DarwinModesAndRoutingRegressionTest` PASS.
- Pre-existing failures (present on the unmodified bundle as well):
  `RecursiveEvolutionTest` (chat routing in its mock), `ScenarioTest`
  (atomicAnalysis metadata), `ChatShortCircuitFixTest`/`MediatedModeIntegrationTest`
  (mediated summaries), `DarwinDeveloperModeIntegrationTest`/`DarwinCognitiveIntegrationTest`/
  `DarwinSessionLifecycleRegressionTest`/`DarwinEngineRobustnessTest` (test-setup
  issues: missing session registration, `shutdownAll()` cross-test pollution,
  FOLDER-vs-REPOSITORY detection). Unrelated to the modified code paths.

## 12. Final acceptance table

| Capability | Result | Evidence |
|---|---|---|
| Candidate generation | PASS | 3 distinct materialized candidates/iteration with stable ids, strategies, implementations |
| Independent competition | PASS | each candidate evaluated in its own git worktree; distinct objective scores |
| Objective evaluation | PASS | real `mvn test` per candidate; scores 0.42/1.00/0.42 from pass rates |
| Candidate ranking | PASS | decision gate presented ranked scores; best (1.00) auto-selected |
| Winner selection | PASS | Order-Preserving candidate selected by objective score in every instance |
| Best-so-far preservation | PASS | winner persisted ACTIVE + tree + working tree; inferior candidates refused (post-fix) |
| Multi-iteration evolution | PASS | 4 iterations, new candidate set per iteration, lineage continuity |
| Trend tracking | PASS | per-iteration records + trajectory analyses + fitness history |
| Stagnation detection | PARTIAL | stability policy exists; diminishing-returns convergence implemented; entropy history never populated (documented limitation) |
| Motivation/continuation | PASS | phase machine + stability + budget; explainable STOP/CONTINUE/ROLLBACK decisions |
| Regression handling | PASS | inferior candidates refused; previous winner preserved (Scenario C) |
| Stop conditions | PASS | satisfied objective, budget, repeated failure — all observed |
| Multi-instance participation | PASS | 10/10 inventory; 3 instances competed with identical criteria; winner independent of instance |
| State machine integrity | PASS | legal transitions pass; illegal rejected; sole authority `IterationManager` |
| Persistence/recovery | PASS | records/tree/checkpoint survive restart; winner + evidence restored |
| Frontend observability | PASS | progress events + branch payloads + persisted data carry real scores |

## 13. Remaining limitations

1. `EvolutionMemoryGraph.entropyHistory` / `convergenceReasoning` are never
   populated (no `recordEntropy` callers) — stagnation/trend reporting relies on
   fitness history and stability policy instead.
2. `KernelScheduler` reduced the presented population 3 → 2 under CPU backpressure
   (adaptive budget, environment-dependent) — competition always uses ≥ 2
   candidates; documented, not modified.
3. MEDIUM reality gate labels test failures as "Compilation error" (it runs the
   configured goals, `mvn test` in the harness) — cosmetic evidence wording.
4. Predicted (`DarwinFitnessRanker`) scores are overwritten by objective scores
   without retaining the predicted record.
5. Cross-instance competition requires separate sessions; no engine pool competes
   within one session (existing architecture).
6. Unused engine fields (`selectionEngine`, `penaltyModel`, `diversityController`)
   remain in `ADarwinEngine` (out of validation scope).

## 14. Reproduction instructions

```bash
cd /home/petr/git/evolution-121126
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
./mvnw -o -pl eu.kalafatic.evolution.controller.tests -am verify -Dtest=DarwinEvolutionValidationTest
```

Fixture: `test-fixtures/darwin-evolution/` (buildable standalone via `mvn -o test`).
The engine drives it through the production `Evaluator`; LLM responses are scripted
inside the test (no external services required; a local Ollama instance may serve
the dimension-discovery call, which is optional and degrades to zero dimensions).
