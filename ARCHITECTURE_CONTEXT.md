# Architecture Context: AI Evolution Kernel

This document provides high-level semantic navigation for AI agents interacting with this codebase.

## Core Vision
The platform is a modular capability-based evolution kernel designed to coordinate small local LLMs for autonomous and semi-autonomous software development.

## Authority Hierarchy
- **AuthorityController**: The single source of truth for execution decisions.
- **Darwin Engine**: Explores and generates branch variants.
- **Evaluators**: Independent signal producers.
- **Kernel/Scheduler**: Decides winners based on signals and policy.

## Semantic Domains
- **Mediation**: Context curation, project mapping, and external LLM export.
- **Trajectory**: Lineage, signal history, and evolutionary memory.
- **Supervision**: Decision authority, policies, and user oversight.
- **Execution**: Scheduling, budgets, and sandbox isolation.
- **Orchestration**: Lifecycle coordination and workflow state management.

## Metadata System
The project uses a dual-layer metadata system:
1. **Annotations**: @EvolutionComponent in Java source.
2. **Sidecars**: .ai.json files for all artifacts.

## Key Invariants
- **Multiple systems may propose; only one system may decide.**
- All SWT updates must be safe (equality-checked) to prevent layout storms.
- Model changes must adhere to unique sequential IDs in EMF packages.
