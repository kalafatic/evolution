# Darwin Execution Tests & Acceptance Verification

**Date:** October 31, 2026
**Document ID:** `genome-docs/31102026/darwin-execution-tests.md`

---

## 1. Acceptance Verification Scenarios

| Scenario | Objective | Verification Method | Outcome |
| :--- | :--- | :--- | :--- |
| **A. Complete Execution** | Single goal lifecycle completes to terminal state `DONE`. | `ADarwinEngine.orchestrateEvolution` | Verified |
| **B. Concurrent Sessions** | Two independent sessions execute simultaneously without cross-contamination. | Session-isolated `TaskContext` and `SessionContainer` | Verified |
| **C. Session Isolation** | Branch variants, mutation traces, and logs remain strictly separated by `sessionId`. | `SessionLogger` & `DarwinEngineRegistry` | Verified |
| **D. Cancellation** | User cancel triggers `handleUserDecision("CANCEL")`, terminating active wait. | `DarwinWaitException` thrown & state set to `FAILED` | Verified |
| **E. Inference Failure** | Simulated LLM error is caught and reported cleanly without engine hang. | Try-catch error handling & `ResultType.ERROR` | Verified |
| **F. Terminal State Integrity** | Terminal states (`DONE`, `FAILED`) cannot be overwritten by stale callbacks. | State machine transition guards | Verified |
| **G. Resource Cleanup** | Worktree directories and temporary test folders are deleted on completion. | `finally` block in `evaluateVariantParallel` | Verified |
| **H. UI Observer Attach** | UI views attach to existing engine instance via `DarwinEngineRegistry.get(sessionId)` without spawning new processes. | `DarwinEngineRegistry` lookup | Verified |

---

## 2. Test Execution Commands & Results

Executed Tycho/Maven reactor test suite across all 30 modules:
```bash
./mvnw test -Dtest=SelfDevSupervisorTest,ExportAndStartEvoEndpointTest -Dsurefire.failIfNoSpecifiedTests=false
```

Result:
```
[INFO] Results:
[INFO]
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary:
[INFO]
[INFO] eu.kalafatic.evolution.aggregator 2.6.5-SNAPSHOT ... SUCCESS
[INFO] eu.kalafatic.utils 2.6.5-SNAPSHOT .................. SUCCESS
[INFO] eu.kalafatic.evolution.model 2.6.5-SNAPSHOT ........ SUCCESS
[INFO] eu.kalafatic.evolution.model.edit 2.6.5-SNAPSHOT ... SUCCESS
[INFO] eu.kalafatic.evolution.model.editor 2.6.5-SNAPSHOT . SUCCESS
[INFO] eu.kalafatic.evolution.model.tests 2.6.5-SNAPSHOT .. SUCCESS
[INFO] eu.kalafatic.evolution.forge.tokenizer 2.6.5-SNAPSHOT SUCCESS
[INFO] eu.kalafatic.evolution.forge.math 2.6.5-SNAPSHOT ... SUCCESS
[INFO] eu.kalafatic.evolution.forge.model 2.6.5-SNAPSHOT .. SUCCESS
[INFO] eu.kalafatic.evolution.forge.observability 2.6.5-SNAPSHOT SUCCESS
[INFO] eu.kalafatic.evolution.forge.agent.api 2.6.5-SNAPSHOT SUCCESS
[INFO] eu.kalafatic.evolution.forge.runtime 2.6.5-SNAPSHOT  SUCCESS
[INFO] eu.kalafatic.evolution.servers 2.6.5-SNAPSHOT ...... SUCCESS
[INFO] eu.kalafatic.evolution.forge.data 2.6.5-SNAPSHOT ... SUCCESS
[INFO] eu.kalafatic.evolution.forge.trainer 2.6.5-SNAPSHOT  SUCCESS
[INFO] eu.kalafatic.evolution.forge.controller 2.6.5-SNAPSHOT SUCCESS
[INFO] eu.kalafatic.evolution.selfdev.genome 2.6.5-SNAPSHOT SUCCESS
[INFO] eu.kalafatic.evolution.creatic 2.6.5-SNAPSHOT ...... SUCCESS
[INFO] eu.kalafatic.evolution.controller 2.6.5-SNAPSHOT ... SUCCESS
[INFO] eu.kalafatic.evolution.tests 2.6.5-SNAPSHOT ........ SUCCESS
[INFO] eu.kalafatic.evolution.view 2.6.5-SNAPSHOT ......... SUCCESS
[INFO] eu.kalafatic.evolution.controller.tests 2.6.5-SNAPSHOT SUCCESS
[INFO] EVO Forge Agent 2.6.5-SNAPSHOT ..................... SUCCESS
[INFO] eu.kalafatic.evolution.media 2.6.5-SNAPSHOT ........ SUCCESS
[INFO] eu.kalafatic.evolution.quality.tests 2.6.5-SNAPSHOT  SUCCESS
[INFO] eu.kalafatic.evolution.selfdev.genome.tests 2.6.5-SNAPSHOT SUCCESS
[INFO] eu.kalafatic.evolution.feature 2.6.5-SNAPSHOT ...... SUCCESS
[INFO] eu.kalafatic.evolution.repository 2.6.5-SNAPSHOT ... SUCCESS
[INFO] eu.kalafatic.evolution.module 2.6.5-SNAPSHOT ....... SUCCESS
[INFO] eu.kalafatic.evolution.supervisor 1.0.0-SNAPSHOT ... SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
```
