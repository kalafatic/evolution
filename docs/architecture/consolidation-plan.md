# Architecture Consolidation Plan and Component Recommendations

**Document Identifier**: `docs/architecture/consolidation-plan.md`
**Date**: September 9, 2026

---

## 1. Executive Summary

This consolidation plan defines the roadmap for converging the current EVO codebase toward the unified target architecture established in `docs/milestones/milestone_architecture_consolidation_090926.md`.

The plan adheres to the **Audit Before Refactoring** directive: no premature class renames or deletions are executed immediately; instead, every component is classified with its architectural status and recommended action.

---

## 2. Priority Ranking of Architectural Issues

| Priority | Category | Problem Summary | Affected Components |
| :--- | :--- | :--- | :--- |
| **P0** | **Correctness & Safety** | State leakage across sessions via static singletons and non-thread-safe global state. | `SessionBoundaryGuard`, `OrchestratorServiceImpl` static caches |
| **P1** | **Major Duplication** | Competing dataset acquisition and model loading paths between UI dialogs and Forge backend. | `DatasetEditorGroup`, `ForgeSettingsDialog`, `LLMDarwinEngine` |
| **P2** | **Structural Debt** | Direct UI invocation of domain orchestration and low-level filesystem resolvers. | `AiChatPage`, `ForgeSettingsDialog`, `OllamaProvider` |
| **P3** | **Cleanup & Naming** | Inconsistent naming suffixes (`Manager` vs `Service` vs `Engine`) across OSGi bundles. | Legacy utility classes, duplicate provider abstractions |

---

## 3. Component Classification Matrix

Every major platform module is classified under two dimensions:
- **Architectural Status**: `AUTHORITATIVE`, `ACTIVE`, `COMPATIBILITY`, `LEGACY`, `DUPLICATE`, `OBSOLETE`
- **Recommended Action**: `KEEP`, `MERGE`, `MOVE`, `REPLACE`, `DEPRECATE`, `REMOVE`

### 3.1 Core & Orchestration
| Component | Package / Class | Architectural Status | Recommended Action | Notes |
| :--- | :--- | :--- | :--- | :--- |
| `OrchestratorServiceImpl` | `eu.kalafatic.evolution.controller` | `AUTHORITATIVE` | `KEEP` | Main orchestration service for AI chat, intent routing, and capability dispatch. |
| `ADarwinEngine` | `eu.kalafatic.evolution.controller` | `AUTHORITATIVE` | `KEEP` | Abstract base for Darwin evolution engine; enforces cognitive state transitions. |
| `IterationManager` | `eu.kalafatic.evolution.supervisor` | `AUTHORITATIVE` | `KEEP` | Transition authority for Self-Dev / Darwin iteration loops. |
| `SessionManager` | `eu.kalafatic.evolution.controller` | `AUTHORITATIVE` | `KEEP` | Session context authority maintaining isolated `TaskContext` instances. |

### 3.2 Forge & Model Pipeline
| Component | Package / Class | Architectural Status | Recommended Action | Notes |
| :--- | :--- | :--- | :--- | :--- |
| `ForgeOrchestratorImpl` | `eu.kalafatic.evolution.forge.controller` | `AUTHORITATIVE` | `KEEP` | Single pipeline authority for source analysis, dataset composition, and model training. |
| `EvoLlmModel` | `eu.kalafatic.evolution.forge.model` | `AUTHORITATIVE` | `KEEP` | Core causal transformer math and canonical state holder. |
| `EvoModelArtifact` | `eu.kalafatic.evolution.forge.model` | `AUTHORITATIVE` | `KEEP` | Serialization boundary for `.evo` native binary containers and folder layouts. |
| `EvoLlmTrainer` | `eu.kalafatic.evolution.forge.trainer` | `AUTHORITATIVE` | `KEEP` | Native Java backpropagation and AdamW optimizer implementation. |
| `DatasetAcquisitionEngine` | `eu.kalafatic.evolution.forge.data` | `AUTHORITATIVE` | `KEEP` | Streamed dataset fetching, quality scoring, deduplication, and `.evodata` serialization. |
| `ReferenceEvoInferenceEngine` | `eu.kalafatic.evolution.forge.model.inference` | `AUTHORITATIVE` | `KEEP` | Zero-dependency KV-Cache accelerated JVM native inference runtime. |
| `LLMDarwinEngine` | `eu.kalafatic.evolution.controller` | `LEGACY` / `DUPLICATE` | `MERGE` | Contains duplicate forging/training loops that should delegate directly to `ForgeOrchestratorImpl`. |

### 3.3 Exporters & Integrations
| Component | Package / Class | Architectural Status | Recommended Action | Notes |
| :--- | :--- | :--- | :--- | :--- |
| `OllamaExporter` | `eu.kalafatic.evolution.forge.agent.api` | `AUTHORITATIVE` | `KEEP` | GGUF v3 exporter with independent `GGUFValidator` validation gate. |
| `GGUFValidator` | `eu.kalafatic.evolution.forge.agent.api` | `AUTHORITATIVE` | `KEEP` | Independent binary validator parsing exported `.gguf` files before registration. |
| `SelfDevSupervisor` | `eu.kalafatic.evolution.supervisor` | `AUTHORITATIVE` | `KEEP` | Out-of-process build, deployment, and runtime control server. |
| `GitVersionControlProvider` | `eu.kalafatic.evolution.controller.git` | `AUTHORITATIVE` | `KEEP` | Low-level Git operations and mutation workspace isolation. |

---

## 4. Phased Convergence Roadmap

```text
PHASE 1: Application Service Boundary Hardening
  └── Wrap domain orchestrators behind clean application interfaces (e.g. ForgeApplicationService).
  └── Remove direct UI calls into low-level model resolvers and data cleaners.

PHASE 2: Forge Pipeline Unification
  └── Consolidate LLMDarwinEngine training routines to delegate to ForgeOrchestratorImpl.
  └── Enforce strict separation between BaseModel (.evo) and TrainingSources (.evodata).

PHASE 3: Session & State Isolation Hardening
  └── Eliminate remaining static mutable caches across SessionBoundaryGuard and ProviderConfig.
  └── Enforce request-scoped context isolation across all asynchronous threads.

PHASE 4: Protocol & Artifact Validation Invariants
  └── Enforce mandatory preflight checks and post-execution validation across all workflows (Invariant I9).
  └── Standardize filesystem workspace directory structures to match docs/architecture/filesystem.md.
```
