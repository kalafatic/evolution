# DESIGN DECISIONS

## Event Bus over Direct Calling
**Rationale**: Direct service calls between bundles create tight coupling that breaks the OSGi modularity. The Event Bus allows subsystems like Guidance or UI to react to Kernel events without the Kernel needing to know about them.
**Trade-off**: Slightly higher latency and more difficult debugging of message flows.

## Genome Isolation
**Rationale**: The Genome is a separate module to ensure that architectural memory can be persisted and shared independently of the physical source code of the project being evolved.
**Assumption**: Shared architectural "genes" are transferable across projects with similar tech stacks.

## Multi-Trajectory Darwinism
**Rationale**: Code generation is non-deterministic and prone to hallucinations. By forcing divergence (Modular vs. Minimalist) and evaluating them in parallel, the platform increases the probability of finding a high-quality, stable solution.
**Trade-off**: High computational cost and time (running multiple builds/tests).

## EMF for Orchestration Models
**Rationale**: Using EMF (`.ecore`) provides a standardized, tree-based representation of tasks and sessions that integrates well with Eclipse views and properties.
**Challenge**: Mapping between EMF (XMI) and LLM-friendly formats (JSON) requires manual translation layers in `DarwinEngine`.

## Mediated Mode vs. Local Execution
**Rationale**: For very large repositories (1M+ LOC), the context window of most LLMs is too small for local Darwin evolution. Mediated Mode allows EVO to act as a "Sieve," preparing high-signal context for more powerful external models.
