# Terminology and Naming Standardization Rules

**Document Identifier**: `docs/architecture/naming.md`
**Date**: September 9, 2026

---

## 1. Terminology Definitions

To maintain architectural clarity, all class names and interface designations must conform to standardized suffix rules:

| Suffix | Architectural Role | Example Classes |
| :--- | :--- | :--- |
| **Controller** | Application / external coordination layer managing UI/HTTP inputs. | `ForgeEvolutionController`, `AuthController` |
| **Service** | Reusable domain capability service. | `ModelEvaluationService`, `EvoProductBuildService` |
| **Engine** | Specialized computational or state transition execution engine. | `ReferenceEvoInferenceEngine`, `DatasetAcquisitionEngine` |
| **Orchestrator** | Workflow lifecycle owner coordinating end-to-end pipelines. | `ForgeOrchestratorImpl`, `OrchestratorServiceImpl` |
| **Executor** | Low-level executor executing planned atomic operations. | `GraphExecutionEngine`, `ProcessRunner` |
| **Manager** | Resource and lifecycle manager (used strictly for state/resource authority). | `IterationManager`, `ForgeSessionManager` |
| **Agent** | Reasoning, classification, or intelligence worker node. | `PromptIntentAnalyzer`, `RealityDiscoveryAgent` |
| **Provider** | Infrastructure adapter wrapping external capabilities or APIs. | `OllamaProvider`, `GitVersionControlProvider` |

---

## 2. Terminology Mapping Table

- **Training Input Data**: `TrainingSource` (HuggingFace, local files, codebase) -> serialized into `.evodata` (`TrainingDataset`).
- **Initial Model State**: `BaseModel` (`.evo` artifact) -> trained into child `.evo` (`EvoModel`).
- **Operation Workspace**: `Run` (`EvoRun`) -> owned by state authority.
