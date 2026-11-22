# DARWIN EVOLUTION TEST PLAN

> Validation of the existing EVO Darwin Engine evolutionary process:
> candidate generation → competition → objective evaluation → winner selection →
> iteration → trend → motivation → continue/stop. Evidence-based; deterministic
> executable tests are the primary evidence. Companion documents:
> `DARWIN_EVOLUTION_BASELINE.md` (pre-modification architecture), `DARWIN_EVOLUTION_VALIDATION_REPORT.md`
> (results), `docs/architecture/DARWIN_EVOLUTION_LIFECYCLE.md` (lifecycle).

## 1. Objective

Verify, against the **real** Darwin Engine implementation (`KernelFactory` →
`IterationManager` → `DarwinEngine`/`ADarwinEngine`), that Darwin can:
accept an evolutionary task, generate ≥ 3 genuinely competing candidates, evaluate
each independently against the **same deterministic objective criteria**, rank and
select a winner based on evidence, preserve the best known solution, iterate,
track quality trends, and decide when to continue or stop.

## 2. Test task (deterministic evolution fixture)

`test-fixtures/darwin-evolution/` — an isolated small Maven project (not part of the
Tycho reactor):

- **Baseline implementation**: `CollectionProcessor.deduplicate(List<String>)` —
  functionally correct but quadratic duplicate detection
  (`input.indexOf(item) == i`).
- **Functional contract**: deduplicate preserving first-occurrence order; ignore
  nulls; empty → empty; never modify input; always return a new list.
- **Objective evaluation suite** (`CollectionProcessorTest`, 6 JUnit tests):
  5 functional tests + 1 deterministic complexity test. The complexity test
  instruments `List` lookups (element-comparison counting via `CountingList`) —
  quadratic implementations deterministically fail it (> 2500 element scans for
  2000 elements), linear implementations pass. **No wall-clock timing.**
- **Explicit restrictions**: complexity must be linear; result order must be
  preserved; input must not be mutated.
- **Objective scoring** (existing engine formula): build success →
  `score = 0.8 + passRate*0.2`; build/test failure → `score = passRate*0.5`.

Candidates scripted through the test LLM adapter:

| Candidate | Approach | Expected objective result | Expected score |
|---|---|---|---|
| A | Minimal correction (keeps quadratic scan) | 5/6 tests pass | ~0.42 (fail) |
| B | Optimized order-preserving (`LinkedHashSet`) | 6/6 tests pass | 1.0 |
| C | Fast but order-normalizing (`HashSet` + sort) | 5/6 tests pass (order test fails) | ~0.42 (fail) |

The engine is driven through `KernelFactory.create(DarwinEngine, …)` with a
**scripted deterministic LLM adapter** (no real network calls): territory
blueprints and materialized variants are returned deterministically per sibling
index. All **evaluation** is real: the engine runs the fixture's actual
`mvn test` in isolated git worktrees via the existing `Evaluator`/`MavenTool`.

## 3. Phases and test cases

| Phase | Test case | What is verified |
|---|---|---|
| A | `testPhaseA_CandidateGeneration` | ≥3 candidates, stable ids, independent strategies/implementations, per-candidate metadata, no cross-candidate overwrite |
| B | `testPhaseB_ObjectiveEvaluationAndRanking` | same deterministic suite per candidate; compilation results; objective scores; ranking; evidence (EvaluationResult/test pass rate); failed candidates not winners |
| B/C | `testPhaseC_WinnerSelection` | winner = objective best (B); selection rationale; rejected siblings; winner persisted (IterationRecord ACTIVE, EvolutionTree currentWinnerId); evaluation evidence retained |
| D | `testPhaseD_MultiIteration` | multiple iterations; new candidate set per iteration; lineage continuity (previous winner as parent); best-so-far tracking |
| D/C | `testScenarioC_RegressionIteration` | later iteration's inferior winner must NOT overwrite the better previous winner (confirmed defect; fixed) |
| F | `testScenarioD_AllCandidatesFail` | no invalid winner promoted; baseline preserved; failure recorded; stop/rollback decision |
| F | `testScenarioB_Stagnation` | stagnation with no improvement; continuation/stop evaluated |
| E | `testScenarioE_Completion` | objective satisfied → DESIGN_SATISFIED → stop; termination reason recorded |
| G | `testPhaseG_StateMachine` | legal transitions; illegal transitions rejected; transition ownership (IterationManager); persistence of transition events |
| K | `testPhaseK_PersistenceRecovery` | candidate/evaluation/winner/iteration records and checkpoint survive manager recreation; best-known candidate and evidence restored |
| H | `testPhaseH_InstanceInventory` | all 10 PlatformType → engine mappings via DarwinEngineFactory; per-instance capability check |
| H | `testPhaseH_CrossInstanceCompetition` | same fixture evolved by different engine instances (DARWIN_MODE, ASSISTED_CODING, MUTATION); cross-instance comparison with identical criteria |
| A–K | `testEndToEnd_Acceptance` | full pipeline in one run; state/events/persistence consistent |

## 4. Execution environment

- Tycho surefire (OSGi, headless), module `eu.kalafatic.evolution.controller.tests`:
  `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw -o -pl eu.kalafatic.evolution.controller.tests -am verify -Dtest=DarwinEvolutionValidationTest`
- Fixture builds run as real subprocesses (`mvn -o test`, system Maven 3.8.7,
  cached JUnit 4.13.2) — exactly the production `Evaluator` path.
- No production data modified: sessions use temp directories; `testMode` is NOT
  used for evaluation scenarios (it would fake build success).

## 5. Pass criteria (summary)

- ≥ 3 independently evaluated candidates, objective ranking matches the fixture's
  deterministic outcome (B > A ≈ C).
- Winner selected by the configured selection mechanism with recorded evidence.
- Best known solution preserved across iterations and after recovery.
- Illegal state transitions rejected; legal transitions ordered and observable.
- Stop decisions explained and recorded; no fabricated metrics.
- 100% of discovered Darwin instances accounted for in the inventory report.

## 6. Defect handling

Every confirmed defect: reproduce → record actual behavior → root cause →
smallest targeted fix (preserving `IterationManager` as sole transition authority
and the existing engine architecture) → regression test → rerun scenario →
re-verify previously passing scenarios.
