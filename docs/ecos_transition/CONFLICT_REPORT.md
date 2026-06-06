# CONFLICT_REPORT.md

## Overview
This report identifies architectural contradictions, roadmap conflicts, and constraint violations within the current platform based on ECOS vision documents.

## Architectural Contradictions

### 1. Mode-Specific Execution Logic
- **Severity**: HIGH
- **Description**: The system uses `if(isMediated)` and `if(isTestMode)` checks in `IterationManager` and `DarwinFlow` to alter core evolution behavior.
- **Evidence**: `IterationManager.java` (lines 882, 1145), `DarwinFlow.java` (lines 142, 151, 169).
- **ECOS Violation**: Violates Constraint 8 (NO MODE-SPECIFIC SHORTCUTS).

### 2. Stubbed NeuronAI Subsystem
- **Severity**: MEDIUM
- **Description**: `NeuronEngine` and related agents are present in the codebase but are entirely simulated/stubbed, creating a false "neuromorphic" architectural claim.
- **Evidence**: `EVO_ARCHITECTURE_AUDIT.md` (Section 2.C), `NeuronEngine.java`.
- **ECOS Violation**: Not a direct ECOS violation, but represents architectural "noise" and "dead weight" that obscures evolutionary clarity.

### 3. Orphaned/Dead Components
- **Severity**: LOW
- **Description**: `ExampleMain.java` and `SelfDevMain.java` exist alongside the unified `IterationManager` flow.
- **Evidence**: `EVO_ARCHITECTURE_AUDIT.md` (Section 2.E).
- **ECOS Violation**: Violates the "ONE EVOLUTION KERNEL" (Constraint 2) vision by keeping legacy entry points.

---

## Roadmap Contradictions

### 1. Parallel Lineages vs. Branch-Centric Variants
- **Severity**: HIGH
- **Description**: The current implementation is heavily "branch-centric" (Git-based) and uses "BranchVariant" objects, while the roadmap calls for "Surviving Lineages" as the primary persistent structure.
- **Evidence**: `ECOS_TRANSITION_ROADMAP.md` (Phase F), `BranchVariant.java`.
- **Conflict**: Roadmap goal of Lineage Consolidation is currently hampered by the deep coupling to Git branch-based variants.

### 2. Procedural Evaluation vs. Pressure-Centric Evolution
- **Severity**: MEDIUM
- **Description**: Evaluators currently produce scores used for selection, but "Pressure" is largely a calculated derivative rather than the primary driver of mutation generation in all paths.
- **Evidence**: `DarwinEngine.java` (lines 201-203), `EvolutionaryPressureEngine.java`.
- **Conflict**: Roadmap Phase G (Pressure-Centric Evolution) requires a more central role for pressure in mutation planning than is currently implemented.

---

## Constraint Conflicts

### 1. Intelligence Isolation (Constraint 7)
- **Severity**: MEDIUM
- **Description**: While `DarwinEngine` is intended to be pure, `DarwinFlow` (the executor) contains logic that synthesizes results and manages Git states, blurring the line between "cognition" and "execution".
- **Evidence**: `DarwinFlow.java` (line 155).
- **Conflict**: ECOS Constraint 7 (ORCHESTRATOR OWNS COGNITION) is partially bypassed as execution logic influences cognitive reporting.

### 2. Artifact Agnostic Design (Constraint 5)
- **Severity**: HIGH
- **Description**: The kernel (`IterationManager`) has explicit knowledge of "Java", "Maven", and "Git".
- **Evidence**: `IterationManager.java` (lines 125, 237, 240, 1263).
- **Conflict**: Violates Constraint 5 (ARTIFACT AGNOSTIC DESIGN). The kernel is specialized for code artifacts.

---

## Duplicated Responsibilities

### 1. Iteration Management
- **Description**: Both `IterationManager` and `SupervisorMain` (in `eu.kalafatic.evolution.supervisor`) claim to manage iterations, though at different abstraction levels.
- **Evidence**: `eu.kalafatic.evolution.supervisor/src/main/java/eu/kalafatic/evolution/supervisor/IterationManager.java`.
- **Severity**: MEDIUM

### 2. Project Scanning
- **Description**: `TargetScanner` and `MetadataAgent` both perform repository scanning and semantic extraction.
- **Evidence**: `IterationManager.java` (lines 278, 298).
- **Severity**: LOW

---

## Circular Dependencies
- **Status**: NONE DETECTED in primary controller modules via manual inspection, but Tycho/P2 metadata should be audited for hidden OSGi package cycles.

---

## Summary of Critical Conflicts
The most critical architectural conflict is the **leakage of artifact-specific logic (Java/Git) into the core Evolution Kernel** and the use of **mode-specific conditionals** to manage specialized environments. These directly contradict the ECOS vision of a generalized, artifact-agnostic evolutionary platform.
