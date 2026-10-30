# Darwin Engine Architecture & Cognitive Process Audit

**Date:** October 30, 2026
**Document ID:** `genome-docs/30102026/darwin-engine-audit.md`
**Scope:** Darwin Engine, Process vs. Instance Model, Execution Modes, State Transitions & Concurrency

---

## 1. Overview & Architectural Boundaries

`ADarwinEngine` (extending `BaseAiAgent` and implementing `IDarwinEngine` and `IMutationContract`) serves as the core cognitive and execution authority in EVO. It coordinates LLM inference requests, intent routing, goal modeling, trajectory variant generation, fitness scoring, decision authority, and lineage persistence.

---

## 2. Process vs. Instance vs. Session Model

To prevent accidental state conflation across concurrent executions, Darwin strictly separates:
- **Session (`ChatSession` / `SessionContainer`)**: Represented by a stable `sessionId` UUID. Acts as the persistent memory, logging, and cognitive state container.
- **Darwin Process**: An execution flow (`orchestrateEvolution`) managing a single goal lifecycle across multiple iterations.
- **Darwin Instance (`ADarwinEngine` subclass)**: A concrete, stateful engine object bound to a specific `sessionId` and registered in `DarwinEngineRegistry`.
- **Inference Invocation**: An individual LLM request dispatched via `AiService`.

```
Session (sessionId UUID)
 └── Darwin Instance (ADarwinEngine) [Registered in DarwinEngineRegistry]
      └── Darwin Process (orchestrateEvolution loop)
           └── Execution Iterations (runDarwinIteration)
                ├── Variant Generation (generateProposals)
                └── Parallel Variant Evaluation (evaluateVariantParallel)
                     └── LLM Invocations (AiService)
```

---

## 3. Execution Modes Matrix

| Mode / PlatformType | Key Characteristics | Source Path / Action | Output / Behavior |
| :--- | :--- | :--- | :--- |
| **`SIMPLE_CHAT`** | Conversational short-circuit | `handleIntentRouting` | Direct conversational text response, zero code generation, fast completion. |
| **`ASSISTED_CODING`** | Code generation & modification | `generateProposals` -> `executeWinner` | Multi-variant branching, Git worktree isolation, compilation & test evaluation. |
| **`MEDIATED_MODE`** | Non-destructive cognitive analysis | `getMediationEngine().mediate()` | Repository structure analysis, hotspots, architectural facts, ZIP export link. |
| **`INTENT_RECONSTRUCTION`** | Architectural reverse engineering | `IntentReconstructionEngine` | Intent profiles, subsystem discovery, multi-level scanning, skip redundant loops. |
| **`SELF_DEV_MODE`** | Autonomous self-modification | `SelfDevOrchestrator` | Source copy, Tycho build, product export, Supervisor launch, HTTP verification. |

---

## 4. State Machine & Decision Authority

`ADarwinEngine` transitions through explicitly defined system states managed by `IterationManager`:
1. `INIT`: Initialization & kernel reset for new goals.
2. `ANALYZING`: Intent expansion, repository scanning, and reality discovery.
3. `EXECUTING`: Generating proposals, parallel variant evaluation, and winner materialization.
4. `VERIFYING`: Static analysis, syntax check, build/test reality gates.
5. `WAITING_FOR_USER_DECISION`: Pauses execution thread (`awaitApproval`) until user or auto-approve resumes execution (`resumeWithWinner`).
6. `DONE` / `FAILED`: Terminal state indicating completion or unrecoverable error.

---

## 5. Concurrency, Isolation & Threading Rules

1. **Session Boundary Guard**: Each session executes with its own `TaskContext` and `SessionContainer`.
2. **Parallel Variant Evaluation**: Proposals evaluate in parallel using isolated Git worktrees (`evo-variant-<id>`) or temporary directory copies, preventing cross-variant contamination.
3. **Thread Approval Locking**: Manual approval blocks the engine thread using `approvalLock.wait()`, and resumes via `resumeWithWinner` / `resumeAutoApprove` notifying `approvalLock.notifyAll()`.
