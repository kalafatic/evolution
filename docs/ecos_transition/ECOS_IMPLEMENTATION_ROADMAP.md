# ECOS_IMPLEMENTATION_ROADMAP.md

## Overview
Prioritized implementation sequence for the ECOS transition.

---

## 1. Priority 1: ARCHITECTURAL DEBT REDUCTION (Immediate)

### Item: Remove Dead Code
- **Priority**: CRITICAL
- **Effort**: LOW
- **Risk**: LOW
- **Description**: Delete `ExampleMain.java`, `SelfDevMain.java`, and other legacy entry points.
- **Impact**: Reduces confusion and enforces "One Kernel" vision.

### Item: Formalize NeuronAI
- **Priority**: MEDIUM
- **Effort**: LOW
- **Risk**: LOW
- **Description**: Move `NeuronEngine` to a separate plugin or remove mock implementation from the core.
- **Impact**: Clarifies system capability vs simulation.

---

## 2. Priority 2: KERNEL GENERALIZATION (Core Transition)

### Item: Artifact and Environment Abstraction
- **Priority**: CRITICAL
- **Effort**: HIGH
- **Risk**: HIGH
- **Description**: Introduce `IArtifact` and `IEvolutionEnvironment`. Refactor `IterationManager` to delegate artifact-specific execution to the environment.
- **Impact**: Enables true artifact agnosticism and mode-as-environment architecture.

### Item: Lineage-Centric Persistence
- **Priority**: HIGH
- **Effort**: MEDIUM
- **Risk**: MEDIUM
- **Description**: Refactor memory schema to prioritize Trajectories. Update `Checkpoint` to be artifact-agnostic.
- **Impact**: Strengthens evolutionary continuity.

---

## 3. Priority 3: COGNITIVE REFINEMENT (Evolutionary Drive)

### Item: Pressure-Centric Mutation Planning
- **Priority**: HIGH
- **Effort**: MEDIUM
- **Risk**: LOW
- **Description**: Update `DarwinEngine` prompting to prioritize pressure resolution in blueprint materialization.
- **Impact**: Moves from goal-driven to pressure-driven evolution.

### Item: Intent Expansion Dimension Deepening
- **Priority**: MEDIUM
- **Effort**: MEDIUM
- **Risk**: LOW
- **Description**: Expand dimension discovery to include more architectural and philosophical dimensions.
- **Impact**: Improves semantic grounding.

---

## 4. Priority 4: OPERATIONAL INTEGRATION (Final Alignment)

### Item: Supervisor Event Coordination
- **Priority**: MEDIUM
- **Effort**: LOW
- **Risk**: LOW
- **Description**: Link external supervisor lifecycle to internal Kernel event signals.
- **Impact**: Improves platform stability and autonomous recovery.

---

## 5. Summary Table

| Task | Priority | Effort | Risk | Dependency |
| :--- | :--- | :--- | :--- | :--- |
| **Dead Code Removal** | Critical | Low | Low | None |
| **Artifact Abstraction** | Critical | High | High | Dead Code Removal |
| **Mode Refactoring** | High | Medium | Medium | Artifact Abstraction |
| **Lineage Persistence** | High | Medium | Medium | Artifact Abstraction |
| **Pressure Planning** | High | Medium | Low | Artifact Abstraction |
| **Supervisor Sync** | Medium | Low | Low | Mode Refactoring |
