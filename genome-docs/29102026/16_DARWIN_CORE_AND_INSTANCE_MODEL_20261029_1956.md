# 16. DARWIN CORE AND INSTANCE MODEL

**Document type:** Architectural Specification & Instance Comparison Model
**System under analysis:** Darwin Evolutionary Core and Specialized Execution Instances
**Analysis Identifier:** EVO-DARWIN-INSTANCE-MODEL-20261029
**Timestamp:** 2026-10-29 19:56 UTC
**Repository Commit:** `529bc25c2b0d703e3d7db5520fd4438b5cbfe3ab`

---

## 1. TECHNICAL DEFINITION OF THE DARWIN ABSTRACTION

In the EVO platform, **Darwin** is defined as an abstract process execution contract and lifecycle engine. It defines a unified interface (`IDarwinEngine`) and abstract base class (`ADarwinEngine`) for goal-driven, iterative cognition and execution.

### Key Contracts Implemented by `ADarwinEngine`:
1. **`IDarwinEngine`**: Core evolutionary methods (`orchestrateEvolution`, `runDarwinIteration`, `generateVariants`, `executeWinner`, `evaluateFitness`).
2. **`ICapability`**: OSGi/Modular capability lifecycle contract (`initialize`, `start`, `stop`, `getHealth`, `getStatus`).
3. **`IMutationContract`**: Uniform variant generation signature accepting goal model, snapshot, failure memory, trajectory, and pressure vector.
4. **`BaseAiAgent`**: Integration with session context, AI services, prompt assembly, and interaction logging.

---

## 2. DARWIN INSTANCE LIFECYCLE & STATE MACHINE

Every Darwin execution instance transitions through a formal state machine managed by `IterationManager` and `TaskContext.StateHolder`:

```
 [INIT] ──► [ANALYZING] ──► [MUTATING/BRANCHING] ──► [EXECUTING] ──► [VERIFYING] ──► [DONE]
   ▲                                 │                                    │
   │                                 ▼                                    ▼
   └─────────────────── [WAITING_FOR_USER_DECISION] ◄─────────────── [FAILED] / [RECOVERING]
```

### State Transitions:
- **`INIT`**: Resetting kernel, clearing stale task queues, initializing `TaskContext` and `OrchestrationState`.
- **`ANALYZING`**: Repository structure scanning, target classification (`TargetClassificationDetector`), building `TargetSnapshot` and `TargetRealityModel`.
- **`MUTATING/BRANCHING`**: Generating candidate proposals (`generateProposals`), calculating fitness scores, and evaluating candidate diversity.
- **`WAITING_FOR_USER_DECISION`**: Pausing loop execution via `approvalLock.wait()` when manual approval is required.
- **`EXECUTING`**: Materializing selected candidate actions (`TaskPlanner`, `TaskExecutor`) or training models (`EvoLlmTrainer`).
- **`VERIFYING`**: Reality checks (LIGHT, MEDIUM, HEAVY) and delta analysis against Git commits or filesystem changes.
- **`DONE` / `FAILED`**: Final response assembly (`FinalResponseAssembler`), result logging, and session checkpointing.

---

## 3. STATE OWNERSHIP AND DECISION AUTHORITY

State ownership is strictly partitioned across kernel components:

| Component | Owned State | Persistence Lifecycle |
|---|---|---|
| **`TaskContext`** | Execution ID, start time, workspace paths, flags (`autoApprove`, `isChatRequest`). | Session runtime. |
| **`OrchestrationState`** | Current phase, iteration count, locked abstraction level, execution profile, metadata map. | Session runtime + Checkpoint. |
| **`GoalModel`** | Intent, target artifact, complexity, constraints. | Task duration. |
| **`TargetRealityModel`** | Subsystems, hotspots, architectural facts, knowledge gaps. | Session runtime + `.evo` export. |
| **`EvolutionTree`** | Tree nodes (`EvolutionNode`), root ID, current winner ID, created/modified files. | Persistent (`user_memory.json` / workspace). |
| **`AuthorityController`** | Lifecycle state transitions (`ACTIVE`, `KEPT`, `REJECTED`), decision snapshots. | Task duration. |

---

## 4. INSTANCE COMPARISON MATRIX ACROSS EXECUTION MODES

The following comparison matrix documents the behavior of specialized Darwin instances instantiated via `DarwinEngineFactory.createEngine(PlatformType, ...)`:

| Dimension | `DARWIN_MODE` (`DarwinEngine`) | `FORGE` (`LLMDarwinEngine`) | `ASSISTED_CODING` (`CodingEngine`) | `HYBRID_MANUAL_EXPORT` (`MediatedEngine`) | `SELF_DEV_MODE` (`SelfDevelopmentEngine`) | `INTENT_RECONSTRUCTION` (`IntentReconstructionEngine`) | `DREAM_MODE` (`DreamEngine`) |
|---|---|---|---|---|---|---|---|
| **Primary Purpose** | Full evolutionary software mutation & iteration | Native LLM model forging & candidate search | Interactive AI-assisted coding & refactoring | Read-only repository cognition & ZIP export | Autonomous self-development & platform improvement | General-purpose target discovery & classification | Background analytical cognitive dreaming & hypothesis generation |
| **Target Output** | Code changes / Git commits | `.evo` & `.gguf` model artifacts | Code patches / file edits | Unified `.zip` export package & markdown report | Self-dev code changes & build verification | Target purpose summary & classified snapshot | Consolidated hypotheses & memory entries |
| **Source Modification** | Yes (Git branch / files) | No (Output in `forge-output/`) | Yes (Local workspace) | **NO** (Strictly Read-Only) | Yes (Self-development branch) | **NO** (Strictly Read-Only) | **NO** (Strictly Read-Only) |
| **Iteration Loop** | Recursive multi-pass loop | Multi-generation candidate search | Single/Multi-pass task loop | Single-pass analysis & convergence | Recursive self-correction loop | Multi-pass discovery & seeding loop | Multi-stage background analysis loop |
| **Candidate Generation** | Multi-branch prompt mutation | Transformer hyperparam mutation | Single/Multi-proposal prompt | Evolved mediation candidates | Patch / refactoring candidate generation | Exploration queue batching | Experience candidate ranking |
| **Fitness Evaluation** | `FitnessEngine` + Reality Gates | Train/Val Loss + Param Penalty | Compile / test validation | Coverage % & fact confidence | JUnit / Maven build execution | Discovery coverage & completeness % | Hypothesis verification score |
| **User Decision Gate** | Optional (`awaitApproval`) | Optional (`awaitApproval`) | Interactive approval | Manual export confirmation | Auto or gated approval | Intent clarification planner | Automatic / Background preemption |
| **Persistence** | Git commit + `EvolutionTree` | `forge-output/` + `.evo` artifact | File edit + Git commit | `resources/` ZIP archive | Platform code commit | `targetRealityModel` metadata | `user_memory.json` entries |

---

## 5. SHARED VS. MODE-SPECIFIC RESPONSIBILITIES

### Shared Responsibilities (`ADarwinEngine`):
- Goal extraction and abstraction level locking (`GoalModel`).
- Execution profile calculation (`EvolutionProfile`).
- State transition logging and event bus publishing (`RuntimeEventBus`).
- Decision gate blocking (`awaitApproval`) and UI branch emission (`emitDarwinBranches`).
- Checkpointing and recovery handling (`IterationManager.saveFullCheckpoint`).

### Mode-Specific Responsibilities:
- **`LLMDarwinEngine`**: Tokenizer training (`SimpleBPETokenizer`), dataset sample extraction, pure-Java Transformer model training (`EvoLlmTrainer`), and `.evo` container serialization (`EvoModelArtifact`).
- **`MediatedEngine`**: Enforces zero source code modification, delegates to `MediationEngine`, and invokes `performMediatedExportConvergence`.
- **`SelfDevelopmentEngine`**: Runs Maven builds, enforces test suite verification, and executes git rollbacks on build failure.
- **`DreamEngine`**: Retrieves candidate experiences from `FailureMemory`, scores candidate cooldowns, and writes `UNVERIFIED_HYPOTHESIS` entries to `MemoryService`.

---

## 6. CURRENT ARCHITECTURAL INCONSISTENCIES & DRIFT

1. **Class Hierarchy Overcrowding:**
   - Both `AbstractDarwinEngine` and `AbstractDarwinEngine2` exist alongside `ADarwinEngine` in package `eu.kalafatic.evolution.controller.orchestration.selfdev`, creating ambiguity regarding the canonical base class.
2. **Commented-Out Factory Routes:**
   - In `DarwinEngineFactory.java`, line 21 contains commented-out code (`//return new DarwinEngine(...)`), showing historical drift during engine specialization.
3. **Inconsistent Mode Identifiers:**
   - Some engines return null or fallback strings for `getMode()` or `getCapabilityType()`, relying on `PlatformType` field references instead of explicit interface methods.
