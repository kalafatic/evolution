# ARCHITECTURE CONTEXT

## Domain: Orchestration Kernel
The Evo platform is a deterministic evolutionary kernel designed for mediated AI collaboration.

## Core Semantic Domains
* **Mediation**: Handles target scanning, semantic extraction, and context curation for external LLMs.
* **Trajectory**: Manages evolution branches, signal buses, and proposal lineages.
* **Execution**: Governs scheduling, backpressure, and execution budgets.
* **Supervision**: Central authority for decision making, activation, and policy enforcement.
* **Orchestration**: Coordination layer for iteration management and flow execution.

## Invariants
* Single Transition Authority (IterationManager)
* Intelligence Isolation (DarwinEngine/Analyzers)
* Deterministic State Machine
