# Supervision and Authority

## 1. Authority Hierarchy
The platform implements a strict hierarchy to ensure safety and determinism.

| Level | Component | Role | Authority |
|---|---|---|---|
| 1 | Human User | Supervisor | Final Decision |
| 2 | IterationManager | Kernel | State Transition |
| 3 | DecisionResolver | Resolver | Variant Activation |
| 4 | SignalBus | Backplane | Data Propagation |
| 5 | Agents / Darwin | Proposers | Suggestion Only |

## 2. Decision Authority
**The Kernel is the ONLY decision authority.**
No agent or tool is permitted to:
- Change the `SystemState` directly.
- Activate a Git branch for final merging.
- Declare a task "Complete" without Kernel oversight.

## 3. Mediated Supervision
In `MEDIATED` mode, the platform acts as a high-powered assistant that proposes but never imposes.
- **Activation Gate**: Ranks proposals and presents them to the user.
- **Manual Selection**: Users can override the `DecisionResolver` and pick a specific variant.
- **Step-Mode**: The system pauses at critical boundaries (Mutation, Patch Generation, Evaluation) for manual verification.

## 4. Invariant Protection
Authority is protected by code-level encapsulation. Core methods that modify state or activation status are strictly guarded and audited to prevent "intelligence drift" where an LLM might attempt to bypass system constraints.
