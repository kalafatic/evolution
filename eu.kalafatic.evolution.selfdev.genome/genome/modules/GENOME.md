# Genome System: Evolutionary Memory

## Overview
The Genome system is the persistent architectural memory of the EVO platform. It encodes patterns, behavioral traits, and "evolutionary lessons" into portable artifacts that can be shared across projects and AI sessions.

## Core Concepts

### GenomeArtifact
The serialized "DNA" package of a system. It can contain:
- **Architecture Genes**: Patterns like Service-Repository or Event-Driven orchestration.
- **Behavioral Traits**: LLM reasoning patterns that proved successful.
- **Metric Artifacts**: Performance and stability benchmarks.
- **Project Snapshots**: High-level structural summaries.

### Genome A vs. Genome B
In Mediated Mode, the system evolves two distinct genomes:
- **Genome A**: Instructional Prompt logic.
- **Genome B**: Context selection and file metadata.

## Key Components

### LocalGenomeRepository
Manages the storage and retrieval of genome artifacts from the local filesystem.
- **Path**: `eu.kalafatic.evolution.selfdev.genome.repository.LocalGenomeRepository`

### SecondhandUpgradeEngine
A reasoning component that attempts to map a genome pattern from one project context to another.
- **Status**: Nascent. Currently uses placeholder logic for mapping.

### MilestoneGenerator
Creates historical markers in the genome to track architectural maturity over time.

## Workflows

### Genome Loading
The platform scans the workspace for `.genome` files or directories to ground its initial understanding of a project.

### Evolution Persistence
During a Darwin cycle, winning variants are persisted into the `EvolutionTree` and eventually summarized into new genome fragments.

## Future Direction
- **Marketplace**: Sharing successful "Architectural Genes" between developers.
- **Behavioral Cloning**: Encoding an LLM's successful reasoning patterns into a "Cognitive Gene."

## Related
- [ARCHITECTURE.md](../architecture/ARCHITECTURE.md)
- [DARWIN.md](../architecture/DARWIN.md)
