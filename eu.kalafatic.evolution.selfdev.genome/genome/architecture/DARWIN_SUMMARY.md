# AI SUMMARY: Darwin Evolution Engine

## What is this subsystem?
The core iterative logic of EVO. It implements a survival-of-the-fittest model for code changes.

## Why does it exist?
To move beyond "guessing" code. By generating multiple divergent trajectories and testing them in parallel worktrees, the system identifies the most stable and architecturally sound solution.

## Key Mechanisms
- **Trajectory Branching**: Spawning divergent blueprints (e.g., "Modular" vs. "Atomic").
- **Parallel Evaluation**: Simultaneously building and testing all variants.
- **Phylogenetic Memory**: Keeping a record of everything tried, including failures.

## Interaction Map
- Controlled by **IterationManager**.
- Mutates code via **Git worktrees**.
- Ranks variants using **DarwinFitnessRanker**.

## Next Steps
- [ARCHITECTURE_SUMMARY.md](ARCHITECTURE_SUMMARY.md): For the big picture.
- [WORKFLOW_INDEX.md](../WORKFLOW_INDEX.md): For the 5-phase lifecycle details.
