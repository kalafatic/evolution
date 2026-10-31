# Darwin Execution Lifecycle Architecture & State Machine

**Date:** October 31, 2026
**Document ID:** `genome-docs/31102026/darwin-execution-lifecycle.md`

---

## 1. Execution Boundary & Ownership

`ADarwinEngine` (extending `BaseAiAgent` and implementing `IDarwinEngine` and `IMutationContract`) serves as the authoritative owner of cognitive evolution lifecycles in EVO.

### Responsibility Map
- **Session Container (`SessionManager`, `SessionContainer`)**: Holds memory graph, failure memory, cognitive state, event bus, and session-isolated logs.
- **Darwin Engine Registry (`DarwinEngineRegistry`)**: Thread-safe registry mapping `sessionId` to `ADarwinEngine` instances, ensuring multi-view attach capability without duplicate process creation.
- **Iteration Manager (`IterationManager`)**: Manages the iterative step progression and state transitions across reality levels.
- **Authority Controller (`AuthorityController`)**: Evaluates decision types (`HUMAN_APPROVED`, `EXECUTE`, `STOP`) and controls user approval gates.

---

## 2. Execution Lifecycle State Transitions

The execution lifecycle progresses through deterministic, verifiable states:

```
CREATE
  ↓
INITIALIZE (INIT)
  ↓
ANALYZE (ANALYZING - Reality Discovery, Target Snapshot, Intent Expansion)
  ↓
EXECUTE (EXECUTING - Branch Variant Generation & Parallel Evaluation)
  ↓
VERIFY (VERIFYING - Reality Gates: Static Analysis, Build & Test Evaluation)
  ↓
DECISION GATE (WAITING_FOR_USER_DECISION - Pauses on approvalLock in Manual Mode)
  ↓
TERMINAL STATE (DONE / FAILED)
```

1. **`INIT`**: Clears stale tasks, resets trajectory lineage and iteration counters.
2. **`ANALYZING`**: Discovers target repository reality (`TargetRealityModel`), constructs semantic snapshot, and derives `GoalModel`.
3. **`EXECUTING`**: Spawns competing trajectory variants, evaluates them in parallel inside isolated Git worktrees, and materializes winning blueprints.
4. **`VERIFYING`**: Tests syntax, compilation, and reality gates.
5. **`WAITING_FOR_USER_DECISION`**: In manual mode, pauses engine thread (`approvalLock.wait()`) until user decision or auto-approve resumes (`resumeWithWinner`).
6. **`DONE` / `FAILED`**: Irreversible terminal states with complete `FinalResponse` assembly.
