# ECOS_GAP_ANALYSIS.md

## Overview
This document analyzes the gap between the current platform state and the target objectives defined in the ECOS Transition Roadmap.

---

## 1. Lineage Consolidation (Phase F)

### CURRENT STATE
- Evolution is primarily Git-branch-centric.
- "BranchVariant" is the primary unit of exploration.
- Lineage is tracked as a secondary attribute (`IterationRecord`, `Trajectory`).
- Surviving artifacts are "merged" rather than "inherited" in the persistent model.

### TARGET STATE
- "SurvivingLineage" is the primary persistent cognitive structure.
- Artifacts are descendants of surviving ancestors.
- Branches are temporary execution artifacts only.
- The platform operates on artifact-agnostic lineages.

### GAP TYPE
- **Structural**: Unit of evolution needs shifting from Branch to Lineage.
- **Persistence**: Change from branch-history to lineage-history as primary storage.

---

## 2. Kernel Generalization (Phase D)

### CURRENT STATE
- `IterationManager` contains explicit references to Git, Maven, and code-specific agents.
- Mode-specific logic (`if(isMediated)`) exists in core controllers.

### TARGET STATE
- Kernel is artifact-agnostic.
- Environment-specific logic is injected via strategies/adapters.
- No mode-specific shortcuts.

### GAP TYPE
- **Orchestration**: Specialized logic must be moved to environments/strategies.
- **API**: Introduction of artifact and environment abstractions.

---

## 3. Pressure-Centric Evolution (Phase G)

### CURRENT STATE
- `EvolutionaryPressureEngine` exists but acts more as a telemetry signal.
- Mutation generation is driven by "Goals" and "Dimensions" with pressure as a supplementary signal.

### TARGET STATE
- Pressure is the primary driver of mutation planning.
- "What remains unresolved?" is the primary prompt for evolution.

### GAP TYPE
- **Cognition**: Prompting and planning logic must center around pressure resolution.
- **Evolution**: Selection must prioritize pressure-reducing survivors.

---

## 4. Mode Alignment as Environments (Phase E)

### CURRENT STATE
- Mediated Mode and Self-Dev Mode have divergent logic paths inside the unified loop.

### TARGET STATE
- Modes are specialized execution environments providing context and constraints.
- Shared evolutionary infrastructure (Kernel) is used across all modes.

### GAP TYPE
- **Infrastructure**: Mode-specific code in `IterationManager` must be refactored into `EvolutionEnvironment` implementations.

---

## 5. Artifact Agnostic Design (Constraint 5)

### CURRENT STATE
- The system evolves "Code" (Self-Dev) and "Mediation Packages" (Mediated).
- These are treated as special cases in the kernel.

### TARGET STATE
- The kernel evolves "Artifacts".
- Adding a new artifact type (e.g., "Documentation" or "Plan") requires zero kernel changes.

### GAP TYPE
- **Structural**: Unified `Artifact` interface and specialization for existing types.

---

## 6. Mediated Intelligence Stabilization (Phase I)

### CURRENT STATE
- Mediated mode is an iterative Darwin evolution of context.
- Convergence relies on local LLM capabilities which vary significantly.

### TARGET STATE
- High-quality, stabilized mediation package evolution.
- Resilient recovery from malformed/low-signal LLM outputs.

### GAP TYPE
- **Mediated Mode**: Strengthening the `autoRepair` and fallback mechanisms (partially implemented).

---

## Summary of Gaps
| Objective | Gap Severity | Primary Gap Type |
| :--- | :--- | :--- |
| Lineage Consolidation | HIGH | Structural / Persistence |
| Kernel Generalization | HIGH | Orchestration / API |
| Pressure-Centric Evolution | MEDIUM | Cognition |
| Mode Alignment | MEDIUM | Infrastructure |
| Artifact Agnosticism | HIGH | Structural |
