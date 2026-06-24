# Darwin Responsibility Map

| Responsibility | Current Owner | Correct Owner | Status |
| :--- | :--- | :--- | :--- |
| **Evolutionary Lifecycle (Recursive Loop)** | `IterationManager` | `DarwinEngine` | PENDING |
| **Iteration Orchestration** | `IterationManager` | `DarwinEngine` | PENDING |
| **Branching/Proposal Orchestration** | `DarwinFlow` | `DarwinEngine` | PENDING |
| **Execution Orchestration (Winner)** | `DarwinFlow` | `DarwinEngine` | PENDING |
| **Population Scaling (Branch Count)** | `DarwinEngine` / `DarwinFlow` | `DarwinEngine` | PENDING |
| **Iteration Count & Stopping Criteria** | `IterationManager` | `DarwinEngine` | PENDING |
| **Convergence & Semantic Saturation** | `IterationManager` / `DarwinFlow` | `DarwinEngine` | PENDING |
| **Retry Policy (Discovery)** | `TrajectoryTerritoryMapper` | `DarwinEngine` | PENDING |
| **Retry Policy (Materialization/Repair)** | `DarwinVariantSpawner` | `DarwinEngine` | PENDING |
| **Mutation Scheduling & Parent Selection** | `DarwinEngine` | `DarwinEngine` | PENDING |
| **Fitness Orchestration** | `DarwinEngine` | `DarwinEngine` | PENDING |
| **Mediation Orchestration** | `IterationManager` / `DarwinEngine` | `DarwinEngine` | PENDING |
| **Chat Orchestration** | `IterationManager` / `DarwinEngine` | `DarwinEngine` | PENDING |

## Deterministic Services (Logic Blocks)

- **TrajectoryTerritoryMapper**: ONLY discovers candidate territories. No retries, no iterations, no evolution policy.
- **DarwinVariantSpawner**: ONLY materializes a supplied blueprint. No retries, no convergence, no scheduling.
- **ImplementationPlanner**: ONLY converts a validated solution into executable actions. No evolutionary behavior.
- **DarwinFitnessRanker**: ONLY computes fitness. No influence on scheduling.
- **DarwinVariantValidator**: ONLY validates structure. No repair logic.
