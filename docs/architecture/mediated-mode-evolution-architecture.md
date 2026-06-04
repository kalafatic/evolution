# Mediated Mode Evolution Architecture

## Overview

Mediated Mode in EVO has been refactored from a static one-shot synthesis pipeline into a specialized application of the Darwin evolutionary engine. Instead of evolving code implementations, Mediated Mode focuses on the iterative refinement of **mediation artifacts** until a high-quality package is produced for external LLM processing.

## Core Components

### 1. `MediationCandidate` Model
The primary unit of evolution in Mediated Mode. It encapsulates all artifacts required for an external LLM session:
- **Prompt**: Optimized reasoning instructions for the external LLM.
- **Selected Files**: A high-signal set of repository files relevant to the task.
- **Architecture Summary**: A structural mapping of the relevant system components.
- **Dependencies**: Descriptions of internal module relationships and third-party dependencies.
- **Execution Instructions**: Specific operational steps for the external LLM to follow.
- **Evaluation**: A self-assessment of the candidate's quality and reasoning density.

### 2. Specialized Darwin Blueprints
The `DarwinEngine` generates competing blueprints specifically tailored for mediation evolution:
- **Concise Mapping**: Focuses on information density and minimal context.
- **Architecture-Driven**: Prioritizes structural topology and architectural patterns.
- **Implementation-Driven**: Focuses on functional logic and implementation hotspots.
- **Dependency-Expanded**: Analyzes broad module interactions and cross-cutting concerns.

### 3. Evolutionary Spawning and Validation
The `DarwinVariantSpawner` materializes these blueprints by requiring the LLM to populate the `MediationCandidate` schema. The `DarwinVariantValidator` ensures structural integrity and prohibits placeholders.

## Lifecycle and Workflow

1. **Initialization**: The orchestrator detects Mediated Mode and performs an initial repository scan to ground the evolution.
2. **Iterative Evolution**:
   - Competing `MediationCandidate` variants are generated based on divergent blueprints.
   - Variants are evaluated based on their reasoning density and signal-to-noise ratio.
   - The user or an automatic ranker selects the surviving candidate.
   - Subsequent generations refine the surviving candidate, addressing architectural pressures.
3. **Convergence and Export**:
   - Once a high-quality candidate emerges, the evolution converges.
   - The winning `MediationCandidate` is selected as the authoritative source.
   - The `MediatedExportManager` assembles the final artifacts into a ZIP package.

## Export Package Structure

The final ZIP package represents a single coherent mediation candidate:
- `prompt.md`
- `architecture.md`
- `dependencies.md`
- `execution-instructions.md`
- `metadata.json`
- `evolution-analysis.md`
- `affected-files/` (Contains the full content of selected high-signal files)

## Integration with Darwin Infrastructure

Mediated Mode reuses the same robust infrastructure used for code evolution:
- **`DarwinFlow`**: Coordinates the multi-branch evolution.
- **`TrajectoryEngine`**: Tracks the mutation lineage and stability of mediation strategies.
- **`PressureEngine`**: Identifies architectural ambiguities to drive refinement.
- **`AuthorityEngine`**: Manages selection and lifecycle of mediation variants.

By treating mediation as an evolutionary process, EVO ensures that the context and prompts provided to external LLMs are not just synthesized once, but iteratively improved and validated against the real repository structure.
