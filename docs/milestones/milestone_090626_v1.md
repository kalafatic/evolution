# Milestone Savepoint v1 - 090626

## System State Summary
The system is currently at version **2.6.5-SNAPSHOT**. It features a stable, production-ready evolutionary kernel that coordinates local and remote LLMs. The platform is capable of autonomous code generation, repository-grounded reasoning, and multi-trajectory Darwinian exploration.

## Core Invariants (Things that must NOT break)
- **Single Transition Authority**: Only `IterationManager` may change the system state.
- **Darwinian Diversity**: The engine must always attempt to generate divergent trajectories to avoid conceptual collapse.
- **Session Isolation**: Every task must run within a strictly isolated `SessionContainer`.
- **Model Integrity**: All state changes must be reflected in the EMF model and persistent trajectory records.
- **Grounding Mandate**: AI agents must always be grounded in real repository evidence (Discovery phase).

## Stable Components
- **`IterationManager`**: Core state machine and orchestration logic.
- **`DarwinEngine`**: Trajectory spawning and diversity analysis.
- **`EMF Model`**: The underlying `evolution.ecore` and generated implementation.
- **`GitEvolutionAdapter`**: Reliable interaction with version control.
- **`CapabilityRegistry`**: Extensible plugin system for kernel engines.

## Experimental Components
- **`selfdev-genome`**: Self-improvement logic and cross-project knowledge sharing (Active development).
- **`NeuronAI`**: Deep learning-based evaluation signals (Inferred, early stage).
- **`IntentExpansionEngine`**: Advanced semantic uncertainty handling (Stabilizing).

## Recommended Freeze Boundaries
- **Kernel State Machine**: The `SystemState` transitions and `IterationManager` flow should be frozen to ensure architectural stability.
- **API Contracts**: `ICapability`, `IAgent`, and `IMutationContract` interfaces are stable and should be used for all future extensions.
- **Model Schema**: The core `orchestration` package in `evolution.ecore` should be modified with extreme caution.

## Evolution Notes
- Future work should focus on optimizing the token-budget driven context curation in `ContextCurator`.
- Improving the diversity analysis in `DarwinDiversityAnalyzer` for small local models.
- Integration of the `selfdev-genome` proposals into the main orchestration flow.
