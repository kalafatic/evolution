# Unified EVO Architecture Overview

**Document Identifier**: `docs/architecture/overview.md`
**Date**: September 9, 2026
**System Version**: `2.6.5-SNAPSHOT`

---

## 1. Architectural Essence

EVO is **one unified evolutionary platform**, not a loose collection of disparate tooling subsystems. Every operation within EVO follows the fundamental lifecycle abstraction:

```text
EVO REQUEST ──► ANALYZE ──► PLAN ──► EXECUTE ──► EVALUATE ──► EVOLVE ──► ARTIFACT
```

Specialized capabilities realize this sequence:
- **Chat / Inference**: Understand request ──► Infer ──► Evaluate response ──► Return result
- **Darwin Evolution**: Understand task ──► Generate trajectories ──► Execute variants ──► Evaluate fitness ──► Select winner
- **Forge / Training**: Analyze sources ──► Prepare `.evodata` ──► Train ──► Evaluate loss & metrics ──► Export `.evo`
- **Self-Dev / Mutation**: Analyze codebase ──► Plan mutation ──► Mutate ──► Build & Deploy ──► Supervisor Validation ──► Accept / Rollback

---

## 2. Platform Component Diagram

```text
USER / COMMANDER (Eclipse RCP / Web UI)
               │
               ▼
          EVO REQUEST
               │
               ▼
     ORCHESTRATION PIPELINE
  (OrchestratorServiceImpl / ForgeOrchestrator)
               │
   ┌───────────┼───────────┬───────────┐
   ▼           ▼           ▼           ▼
 INFERENCE   DARWIN      FORGE     SELF-DEV / MUTATION
 (Ollama /  (Variant   (Dataset /  (Supervisor /
  Native)   Selection) Trainer)    Workspace)
   │           │           │           │
   └───────────┼───────────┴───────────┘
               ▼
        DOMAIN SERVICES
  (ModelRegistry, DatasetRegistry, GitManager)
               │
               ▼
      IMMUTABLE ARTIFACTS
       (.evodata & .evo)
```

---

## 3. Core Subsystem & Abstraction Architecture

### 3.1 AI Task & Context Model
Every workflow in EVO executes within a request-scoped `TaskContext` managed by `SessionManager`. The task model encapsulates intent classification, system state transitions, execution metrics, and file change tracking:
```text
AI Task ──► Intent ──► TaskContext ──► Capability Resolution ──► Execution ──► Result
```

### 3.2 Inference & Reasoning Protocol Architecture
Inference requests flow through `ILlmProvider` implementations (`OllamaProvider`, `ReferenceEvoInferenceEngine`, `LlmRouter`). Reasoning models (RLLM) strictly segregate internal thinking tags (`<think>...</think>`) into `LlmResponse.getReasoning()` while preserving user-facing final answers in `LlmResponse.getContent()`. UI widgets render thinking blocks in dedicated visual containers separate from final conversation turns.

### 3.3 Evolutionary Darwin Engine
`ADarwinEngine` drives evolutionary optimization across prompts, plans, code mutations, and neural model architectures. Every iteration generates candidate variants, evaluates fitness deterministically, publishes trajectory states via `[DARWIN_BRANCHES]`, and preserves elite winning configurations.

### 3.4 Forge Model & Data Subsystem
Model forging and dataset acquisition operate as canonical platform authorities:
- **Dataset Pipeline**: `TrainingDataAcquisitionService` aggregates multi-provider sources into normalized `.evodata` archives.
- **Training & Inference**: `EvoLlmTrainer` runs pure Java backpropagation with AdamW optimization, persisting native binary `.evo` artifacts (EVO Native V2 protocol). Zero-dependency native JVM inference is executed via `ReferenceEvoInferenceEngine`.

---

## 4. Core Architectural Invariants

1. **I1 — Single State Authority**: Every run has exactly one state transition authority (`IterationManager`, `ForgeJob`, or `SelfDevSupervisor`).
2. **I2 — Decoupled UI Layer**: The UI starts, observes, and displays state but never contains domain orchestration logic.
3. **I3 — Canonical Artifact Protocols**: `.evodata` represents training data; `.evo` represents trained neural model state.
4. **I4 — Explicit BaseModel Distinction**: Existing `.evo` artifacts serve as model bases for continued training, never as training data.
5. **I5 — Heterogeneous Source Composition**: Heterogeneous inputs (HuggingFace, local files, codebase) normalize into composable mixtures.
6. **I6 — Artifact Immutability**: Completed `.evodata` and `.evo` artifacts are immutable; evolution creates child artifacts.
7. **I7 — Traceable Lineage**: Every model retains full provenance tracking parent model, dataset sources, hyper-parameters, and evaluations.
8. **I8 — LLM as Planner, Not Authority**: LLM outputs represent proposals validated and executed by EVO safety bounds.
9. **I9 — Mandatory Final Validation**: No operation completes without final validation (smoke test, loss verification, or supervisor check).
10. **I10 — Session Isolation**: Runs execute in isolated contexts without shared global mutable state contamination.

---

## 5. Extensibility Framework for Future Workflows

The platform architecture is designed to accommodate arbitrary future AI operational capabilities (such as multimodal audio/video processing, vector RAG databases, MCP agent tools, or distributed inference) without redesigning core orchestration:
1. Implement `ICapability` under `eu.kalafatic.evolution.controller.orchestration.capability`.
2. Define the execution contract and register with `CapabilityRegistry`.
3. Route intent from `PromptIntentAnalyzer` to invoke the resolved capability via standard `TaskContext` dispatch.
