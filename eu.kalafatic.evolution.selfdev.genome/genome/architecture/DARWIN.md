# Darwin Evolution Engine

## Overview
The Darwin Engine is the cognitive kernel of the EVO platform. It implements a multi-trajectory evolutionary process where architectural solutions compete for "survival" based on fitness metrics.

## Evolutionary Loop (Phases)
The engine operates through a strictly defined state machine managed by `IterationManager`:

1. **INTENT_EXPANSION**: Resolves ambiguity in the user's goal and discovers semantic dimensions.
2. **ARCHITECTURE_VARIANTS**: Spawns multiple divergent architectural blueprints using `DarwinEngine`.
3. **SELECTION_REFINEMENT**: Evaluates variants in parallel using temporary Git worktrees and selects a winner (AI-driven or manual).
4. **IMPLEMENTATION_PLAN**: Synthesizes a detailed execution graph for the selected trajectory.
5. **FINAL_SYNTHESIS**: Applies the final changes to the main development branch and verifies the result.

## Key Components

### DarwinEngine
The mutation generator. It uses a sequential mutation loop to ensure that sibling variants are semantically divergent rather than just random noise.
- **Input**: Goal, System Snapshot, Lineage Memory, Sibling Memory.
- **Output**: A list of `BranchVariant` objects.

### DarwinFlow
The orchestrator of parallel evaluation. It manages:
- **Worktree Isolation**: Spawning separate directories for each variant.
- **Parallel Validation**: Running builds and tests for all trajectories simultaneously.
- **Fitness Capture**: Recording the actual results of implementation attempts.

### EvolutionTree & EvolutionNode
The phylogenetic memory of the system.
- Tracks the lineage of every change.
- Preserves "rejected" branches to prevent re-exploring failed design space.
- Stores code snapshots of every successful mutation.

### DarwinFitnessRanker
Calculates the survival score of a variant based on:
- **Build Success**: Does it compile?
- **Verification**: Do the tests pass?
- **Architectural Score**: Consistency with patterns.
- **Complexity Penalty**: Favors simpler solutions unless complexity is justified.

## Mediated Mode Evolution
In `isExportOnly` mode, the engine evolves two genomes independently:
- **Genome A**: The instructional prompt for an external LLM.
- **Genome B**: The curated context package (4-16 files) and metadata.

## Design Rationale
- **Parallel Trajectories**: Allows exploring "Modular" vs "Minimalist" approaches simultaneously.
- **Sequential Sibling Memory**: Forces the LLM to differentiate each new variant from the ones it already generated in the current round.

## Related
- [ARCHITECTURE.md](ARCHITECTURE.md)
- [IterationManager.java](../../../eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/orchestration/IterationManager.java)
