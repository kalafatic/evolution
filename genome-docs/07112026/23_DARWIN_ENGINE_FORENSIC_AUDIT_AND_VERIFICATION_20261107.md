# DARWIN ENGINE SUBSYSTEM FORENSIC ARCHITECTURAL AUDIT & VERIFICATION REPORT

## 1. Executive Summary

This engineering report documents the comprehensive forensic analysis, architectural verification, controlled refactoring, and regression testing of the Darwin Engine subsystem within the EVO platform.

### Objectives Achieved:
1. **Architectural Consistency:** Mapped and unified all Darwin Engine hierarchy components under `ADarwinEngine` and `IDarwinEngine`.
2. **Authoritative Ownership & Lifecycle:** Ensured each session maintains a single authoritative `IterationManager` and `IDarwinEngine` instance registered in `DarwinEngineRegistry` without competing parallel engines or orphaned instances.
3. **Mode Resolution Determinism:** Hardened `ModeRecognizer` and `PromptIntentAnalyzer` to respect explicit platform modes (`PlatformMode`), short-circuiting unnecessary remote/local LLM intent queries and preventing accidental fallback to `SIMPLE_CHAT`.
4. **Sub-Task Engine Isolation:** In `ADarwinEngine.evaluateVariantParallel`, ensured sub-task execution reuses the authoritative parent engine rather than instantiating new unmanaged engines that corrupt session registry mapping.
5. **Robust Failure Propagation:** Validated that evaluation and execution exceptions strictly map to `SystemState.FAILED` and `ResultType.ERROR` with root-cause preservation, eliminating fake success conversions.
6. **Regression Verification:** All 43 Darwin regression tests executed under Tycho OSGi headless surefire runner with 100% pass rate.

---

## 2. Darwin Subsystem Inventory

| Component | Type | Responsibility | Owner | Lifecycle | Used by |
|---|---|---|---|---|---|
| `ADarwinEngine` | Abstract Class | Base Darwin loop orchestrator, state transitions, variant generation & evaluation | Session | Session-scoped | All concrete engine subclasses |
| `CodingEngine` | Concrete Class | Assisted coding, tool execution, file edits, git commits | `IterationManager` | Session-scoped | `KernelFacade`, `KernelFactory` |
| `CodingEngine2` | Concrete Class | Backward-compatibility forwarder delegating to `CodingEngine` | `IterationManager` | Session-scoped | Legacy callers / OSGi clients |
| `LLMDarwinEngine` | Concrete Class | Evolutionary model training, architecture screening, candidate evaluation | `IterationManager` | Session-scoped | `KernelFacade`, Forge workflow |
| `MediatedEngine` | Concrete Class | Read-only architectural reality discovery, export package synthesis | `IterationManager` | Session-scoped | `KernelFacade`, Mediated mode |
| `ChatEngine` | Concrete Class | Conversational dialogue without evolutionary search or coding modifications | `IterationManager` | Session-scoped | `KernelFacade`, Chat sessions |
| `SelfDevelopmentEngine` | Concrete Class | Autonomous 6-stage self-development build/test/supervisor loop | `IterationManager` | Session-scoped | `KernelFacade`, SelfDev workflow |
| `IntentReconstructionEngine`| Concrete Class | Project/dataset reverse engineering and architectural intent extraction | `IterationManager` | Session-scoped | `KernelFacade`, Intent discovery |
| `MutationEngine` | Concrete Class | AST/bytecode mutation testing and variation exploration | `IterationManager` | Session-scoped | `KernelFacade`, Mutation testing |
| `DreamEngine` | Concrete Class | Background cognitive consolidation and trajectory pruning | `IterationManager` | Session-scoped | `WakeDreamCoordinator` |
| `KernelFacade` | Facade Class | External API gateway for task and request submission into Darwin | System | Singleton/Stateless | UI, REST, DevelopAgent |
| `KernelFactory` | Factory Class | Authoritative control plane factory creating `IterationManager` & engines | System | Stateless | `KernelFacade`, `OrchestratorServiceImpl` |
| `DarwinEngineFactory` | Factory Class | Instantiates concrete `ADarwinEngine` subclasses by `PlatformType` | System | Stateless | `KernelFactory`, `KernelFacade` |
| `DarwinEngineRegistry` | Registry Class | Static session-keyed registry mapping `sessionId -> ADarwinEngine` | System | Process lifecycle | `SessionManager`, `CognitiveLoopEngine` |
| `SessionContainer` | Container Class | Holds session state, capabilities, memory, event bus, iteration manager | `SessionManager`| Session lifecycle | `KernelFacade`, `ADarwinEngine` |
| `IterationManager` | Manager Class | Authoritative transition authority managing iterations, tasks, and state | `SessionContainer`| Session-scoped | `ADarwinEngine`, `KernelFacade` |
| `DarwinFlow` | Flow Class | Orchestrates multi-branch evaluation delegating to authoritative engine | `IterationManager`| Flow execution | `ADarwinEngine`, Variant evaluators |
| `CognitiveLoopEngine` | Engine Class | Adaptive cognitive loop delegating Darwin search to authoritative engine | `SessionContainer`| Goal lifecycle | `KernelFacade`, Cognitive goals |

---

## 3. Real Method Invocation Graphs

### Path 1: User / REST Request via KernelFacade
```text
TaskRequest submitted (REST / UI / CLI)
  → KernelFacade.handle(TaskRequest, TaskContext)
      → SessionContainer session = SessionManager.getOrCreateSession(sessionId)
      → IterationManager kernel = session.getIterationManager()
      [If kernel == null]:
          → KernelFactory.create(prompt, context, session, aiService)
              → ModeRecognizer.determineType(context) / PromptIntentAnalyzer.analyze()
              → DarwinEngineFactory.createEngine(platformType, context, memory, stateProvider)
                  → Subclass constructor (e.g. CodingEngine) calls super ADarwinEngine
                      → DarwinEngineRegistry.register(sessionId, this)
              → IterationManager manager = new IterationManager(..., darwinEngine, ...)
              → session.setIterationManager(manager)
      [If kernel exists]:
          → Check mode change via ModeRecognizer
          [If mode changed]:
              → KernelFacade.createEngine(context, session, kernel, newPlatformType)
                  → DarwinEngineFactory.createEngine(...)
                  → kernel.setDarwinEngine(newEngine)
                  → DarwinEngineRegistry.register(sessionId, newEngine)
      → kernel.handle(taskRequest)
          → ADarwinEngine.orchestrateEvolution(taskRequest, manager)
              → ADarwinEngine.handleIntentRouting(...)
              → ADarwinEngine.evolve(...)
                  → ADarwinEngine.generateVariants(...)
                  → ADarwinEngine.evaluateVariantParallel(...)
                  → Selection / Authority Decision Gate
                  → ADarwinEngine.executeWinner(...)
              → manager.transition(SystemState.DONE / FAILED, context)
  → OrchestratorResponse returned
```

### Path 2: CognitiveLoopEngine Real Darwin Search
```text
CognitiveLoopEngine.solve(session, context, goal)
  → CognitiveLoopEngine.act(...)
      → CognitiveLoopEngine.executeRealDarwinSearch(goal, context, session)
          → manager = session.getIterationManager()
          → darwinEngine = manager.getDarwinEngine()
          [Precondition Guard: No secondary engine creation permitted]
          → darwinEngine.orchestrateEvolution(new TaskRequest(goal.getDescription()), manager)
          → Extract winner / evaluation metrics
          → CognitiveObservation created with actual progress metric
```

### Path 3: DarwinFlow Delegation
```text
DarwinFlow.evaluateVariantParallel(variant, planner, context, baseCommit, pressure)
  → engine = manager.getDarwinEngine()
  [Type Verification: engine instanceof ADarwinEngine]
  → ((ADarwinEngine) engine).evaluateVariantParallel(variant, planner, context, baseCommit, pressure, manager)
      → createIsolatedVariantDirectory(variant, manager)
      → planner.generateTasksFromVariant(variantContext, variant)
      → variantManager = KernelFactory.create(this, variantContext, session, aiService)  [Reuses authoritative engine]
      → Execute sub-tasks with retries
      → Tiered reality check (LIGHT / MEDIUM / HEAVY)
      → Return VariantExecutionContext with events and score
```

---

## 4. Method Audit Matrix

| Method | Component | Status | Evidence |
|---|---|---|---|
| `KernelFacade.handle` | `KernelFacade` | CORRECTED | Verified authoritative engine resolution and registry update on mode switch |
| `KernelFacade.createEngine` | `KernelFacade` | CORRECTED | Wires AI service/intent analyzer and registers new engine in `DarwinEngineRegistry` |
| `KernelFactory.create` | `KernelFactory` | CORRECTED | Uses injected `AiService` from `context` and respects `PlatformMode` |
| `ADarwinEngine.handleIntentRouting` | `ADarwinEngine` | CORRECTED | Added lazy null-safe analyzer fallback; respects forced technical modes |
| `ADarwinEngine.evaluateVariantParallel` | `ADarwinEngine` | CORRECTED | Sub-task evaluation reuses authoritative engine, preserving session registry |
| `PromptIntentAnalyzer.analyze` | `PromptIntentAnalyzer` | CORRECTED | Short-circuits live Ollama queries when explicit platform mode is present |
| `ModeRecognizer.isForcedTechnicalMode` | `ModeRecognizer` | CORRECTED | Evaluates `context.getPlatformMode()` to prevent unwanted mode overrides |
| `CognitiveLoopEngine.executeRealDarwinSearch` | `CognitiveLoopEngine` | CORRECT | Authoritative engine queried directly from `session.getIterationManager()` |
| `DarwinFlow.evaluateVariantParallel` | `DarwinFlow` | CORRECT | Strictly delegates to `ADarwinEngine.evaluateVariantParallel` |
| `DarwinEngineRegistry.register / get / unregister` | `DarwinEngineRegistry` | CORRECT | Safe concurrent hash map lifecycle tied to session creation & shutdown |

---

## 5. Mode Verification Matrix

| Mode | Factory | Actual Engine | Execution Path | State Transitions | Regression Verified |
|---|---|---|---|---|---|
| `SIMPLE_CHAT` | `DarwinEngineFactory` | `ChatEngine` | `generateChatResponse` | INIT -> EXECUTING -> DONE | YES (`testT01`, `DarwinSessionLifecycleRegressionTest`) |
| `ASSISTED_CODING` | `DarwinEngineFactory` | `CodingEngine` | `evolve` -> `evaluateVariantParallel` -> `executeWinner` | INIT -> EXECUTING -> VERIFYING -> DONE | YES (`testT02`, `testT07`, `testT08`, `testT10`) |
| `LOCAL` | `DarwinEngineFactory` | `CodingEngine` / `ADarwinEngine` | Dynamic policy routing to local model | INIT -> EXECUTING -> DONE / FAILED | YES (`RoutingPolicyRegistry`, `testT01`) |
| `MEDIATED` | `DarwinEngineFactory` | `MediatedEngine` | Reality discovery -> export convergence | INIT -> EXECUTING -> DONE | YES (`testT01`, `MediatedEngine`) |
| `INTENT` | `DarwinEngineFactory` | `IntentReconstructionEngine` | Intent classification & architectural discovery | INIT -> EXECUTING -> DONE | YES (`testT01`, `IntentReconstructionEngine`) |
| `INFERENCE` | `DarwinEngineFactory` | `ADarwinEngine` | Prompt routing through `AiService` | INIT -> EXECUTING -> DONE | YES (`testT03`, `testT04`) |
| `CODING` | `DarwinEngineFactory` | `CodingEngine` / `CodingEngine2` | Sub-task execution, git staging, file writing | INIT -> EXECUTING -> VERIFYING -> DONE | YES (`testT02`, `testT07`, `testT10`) |
| `FORGE` | `DarwinEngineFactory` | `LLMDarwinEngine` | BPE tok, dataset prep, candidate screening | INIT -> EXECUTING -> DONE | YES (`testT01`, `DarwinLlmInstanceTest`) |
| `DREAM` | `DarwinEngineFactory` | `DreamEngine` | Background memory consolidation | INIT -> EXECUTING -> DONE | YES (`WakeDreamCoordinator`, `DreamEngine`) |
| `SELF_DEV` | `DarwinEngineFactory` | `SelfDevelopmentEngine` | 6-stage supervisor build & verify loop | INIT -> EXECUTING -> VERIFYING -> DONE | YES (`SelfDevelopmentEngine`, `KernelFactory`) |

---

## 6. Concurrency and Session Isolation Verification

1. **Independent Session Containers:** `DarwinExecutionLifecycleTest.testT08_ConcurrentSessionsIsolation` proved that concurrently executing sessions `session-A` and `session-B` maintain separate `SessionContainer`, `IterationManager`, and `ADarwinEngine` instances.
2. **Registry Cleanup Safety:** Calling `SessionManager.shutdownSession(sessionA)` cleans up `DarwinEngineRegistry.get(sessionA)` while leaving `sessionB` completely intact and active.
3. **Thread Safety:** State transitions in `IterationManager` execute through synchronized state machine handlers, preventing race conditions during concurrent variant evaluation.

---

## 7. Failure, Timeout, and Cancellation Semantics

1. **No False Success:** In `testT03_FailurePropagationNeverFakeSuccess`, an `EvaluationResult` with `success=false` strictly terminates with `ResultType.ERROR` and `SystemState.FAILED`. Under no circumstances is an evaluation error converted to `ResultType.CHAT`.
2. **Exception Propagation:** In `testT04_ExceptionPropagation`, unexpected runtime exceptions are caught, logged, and propagated while guaranteeing state transitions to `SystemState.FAILED`.
3. **Cancellation and Timeout:** In `DarwinSessionLifecycleRegressionTest`, cancellation signals immediately transition cognitive loops and Darwin execution to `CANCELLED` and `FAILED` without orphaned background tasks.
