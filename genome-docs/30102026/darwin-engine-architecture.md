# Darwin Engine Architecture & Subsystem Responsibilities

**Date:** October 30, 2026
**Document ID:** `genome-docs/30102026/darwin-engine-architecture.md`

---

## 1. Overview & Core Purpose

`ADarwinEngine` is the cognitive execution and mutation authority in EVO. It coordinates multi-variant trajectory exploration, goal extraction, prompt composition, intent expansion, parallel variant materialization, fitness ranking, decision authority, and lineage persistence.

---

## 2. Process, Instance, and Session Ownership Model

- **Session (`ChatSession` / `SessionContainer`)**: Persistent memory, logging, and cognitive state container uniquely identified by `sessionId`.
- **Darwin Process**: Logical execution workflow managing goal resolution through `orchestrateEvolution`.
- **Darwin Instance (`ADarwinEngine` subclass)**: Stateful engine instance bound to a `sessionId` and registered in `DarwinEngineRegistry`.
- **Inference Invocation**: Dispatched via `AiService` / `LlmResponse`.

---

## 3. Dependency & Execution Flow Map

```
USER INPUT
  ↓
MODE / INTENT ROUTER (ModeRouter, ModeRecognizer, PromptIntentAnalyzer)
  ↓
DARWIN INSTANCE RESOLUTION (DarwinEngineRegistry, SessionManager)
  ↓
DISCOVERY & REALITY MODEL (TargetScanner, TargetRealityModel, RealityDiscoveryAgent)
  ↓
RECURSIVE EVOLUTIONARY LOOP (ADarwinEngine.evolve / runDarwinIteration)
  ├── 1. GOAL MODEL & ENVELOPE (GoalModel, SemanticEnvelope)
  ├── 2. VARIANT GENERATION (SiblingGenerationManager, DarwinVariantSpawner)
  ├── 3. PARALLEL EVALUATION (evaluateVariantParallel, Git Worktree Isolation)
  ├── 4. FITNESS SCORING (FitnessEngine, DarwinFitnessRanker)
  └── 5. DECISION GATE (awaitApproval, AuthorityController)
  ↓
WINNER MATERIALIZATION & MERGE (executeWinner, ExecutionEngine, GitManager)
  ↓
FINAL RESPONSE SYNTHESIS (FinalResponseAssembler)
```
