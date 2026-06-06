# ECOS_EXECUTION_DAG.md

## Overview
This document defines the strict dependency graph and execution order for the ECOS transition.

---

## 1. DAG Rules
- No circular dependencies.
- No impossible ordering.
- Dependency violation prevents execution of downstream steps.

---

## 2. Execution Graph

### [STB-0] Stabilization
- **Description**: Cleanup dead code and stubs.
- **Dependencies**: NONE.
- **Outputs**: Cleaned repository.

### [INF-1] Core Interfaces
- **Description**: Define `IArtifact`, `IEvolutionEnvironment`, `IMutationStrategy`.
- **Dependencies**: [STB-0].
- **Outputs**: Core ECOS API.

### [ENV-1] Environment Implementation
- **Description**: Create `DevelopmentEnvironment` and `MediationEnvironment`.
- **Dependencies**: [INF-1].
- **Outputs**: Artifact-specific execution environments.

### [KRN-1] Kernel Refactoring
- **Description**: Move Java/Git/Mediated logic out of `IterationManager` and `DarwinFlow`.
- **Dependencies**: [ENV-1].
- **Outputs**: Generalized Evolution Kernel.

### [AGT-1] Agent Normalization
- **Description**: Ensure agents cite environments and follow stateless kernel requests.
- **Dependencies**: [KRN-1].
- **Outputs**: Isolated Cognition Layer.

### [LIN-1] Lineage Persistence Refactor
- **Description**: Update `IterationMemoryService` to prioritize `Trajectory` over `Branch`.
- **Dependencies**: [KRN-1].
- **Outputs**: Lineage-centric persistence.

### [SUP-1] Supervisor Integration
- **Description**: Connect external supervisor to the generalized Kernel event bus.
- **Dependencies**: [KRN-1].
- **Outputs**: Coordinated Supervision.

### [PRS-1] Pressure-Driven Evolution
- **Description**: Enable pressure-centric mutation planning in `DarwinEngine`.
- **Dependencies**: [AGT-1], [LIN-1].
- **Outputs**: Full ECOS cognitive drive.

### [FIN-1] Final ECOS Operational State
- **Description**: Enable autonomous self-evolution mode.
- **Dependencies**: [PRS-1], [SUP-1].
- **Outputs**: Autonomous ECOS.

---

## 3. Strict Execution Order

1.  **[STB-0]** Stabilization (Dead code removal)
2.  **[INF-1]** Introduction of Core Abstractions (IArtifact, IEnvironment)
3.  **[ENV-1]** Creation of specialized Environments
4.  **[KRN-1]** Generalization of the Evolution Kernel
5.  **[AGT-1]** Normalization of Agent behavior
6.  **[LIN-1]** Refactoring of Persistence to Lineage-centricity
7.  **[SUP-1]** Integration of Supervisor with Generalized Kernel
8.  **[PRS-1]** Enabling Pressure-Centric Mutation
9.  **[FIN-1]** Activation of Full ECOS Operational Mode

---

## 4. Visualization (Text-based)
```text
[STB-0] → [INF-1] → [ENV-1] → [KRN-1] ──┬──→ [AGT-1] ──┬──→ [PRS-1] ──→ [FIN-1]
                                        │              │
                                        ├──→ [LIN-1] ──┘
                                        │
                                        └──→ [SUP-1] ─────────────┘
```
