# ARCHITECTURE CONTEXT

## Domain: Orchestration Kernel
The Evo platform is a deterministic evolutionary kernel designed for **mediated evolutionary cognition**.

## Core Semantic Domains
* **Mediation**: Handles target scanning, semantic extraction, and context curation for external LLMs.
* **Trajectory**: Manages evolution branches, signal buses, and proposal lineages.
* **Execution**: Governs scheduling, backpressure, and execution budgets.
* **Supervision**: Central authority for decision making, activation, and policy enforcement.
* **Orchestration**: Coordination layer for iteration management and flow execution.

## Semantic Authority Hierarchy
To ensure cognitive consistency, semantic metadata is resolved using the following priority:
1. **Source code semantics** (Hardcoded logic/types)
2. **EvolutionComponent annotations** (In-code explicit metadata)
3. **Sidecar metadata** (`.ai.json` files)
4. **Context markdown** (`PACKAGE_CONTEXT.md`)
5. **Inferred heuristics** (Naming conventions, path patterns)

*Note: Lower layers may enrich but must not contradict higher layers.*

## Semantic Freshness
The system validates metadata freshness by comparing last-modified timestamps between artifacts and their sidecar metadata. Stale metadata triggers warning signals.

## Invariants
* Single Transition Authority (IterationManager)
* Intelligence Isolation (DarwinEngine/Analyzers)
* Deterministic State Machine
