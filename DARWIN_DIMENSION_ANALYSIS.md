# Darwin Dimension Analysis Architecture

This document describes the key Java classes responsible for sibling dimension analysis and discovery within the Darwin evolutionary orchestration engine.

## Key Classes

### 1. `DarwinEngine`
The central orchestrator for the Darwin evolutionary loop. It manages the high-level flow:
- **Discovery**: Building a semantic repository snapshot and formal reality model.
- **Iterative Evolution**: Running recursive iterations of discovery, mutation, execution, and selection.
- **Terminal Handling**: Finalizing the response once goals are satisfied.

### 2. `DimensionEngine`
Manages the lifecycle of technical decision points (dimensions).
- **Genome Management**: Creates and updates the `SemanticGenome`.
- **Dimension Selection**: Delegates to `GenomeDimensionScheduler` to pick the next mutable dimension.
- **Discovery Trigger**: Orchestrates `DimensionDiscoveryAgent` when the genome is exhausted.

### 3. `GenomeDimensionScheduler`
Responsible for the deterministic selection of the next dimension to mutate.
- **Constraint Handling**: Ensures dependencies between dimensions are respected (locked).
- **Prioritization**: Currently uses `significanceScore` to rank available dimensions.

### 4. `DimensionDiscoveryAgent`
An AI agent that discovers new technical polymorphism points.
- **Lineage Awareness**: Uses the current genome and trajectory history to avoid repetition.
- **Reality Grounding**: Attempts to root new dimensions in the 'Target Reality' of the project.

### 5. `EvolutionDimension`
The data model for a technical decision point.
- **Metadata**: Stores `significanceScore`, `ambiguityScore`, and `evolutionaryPressure`.
- **State**: Tracks whether the dimension is `DISCOVERED`, `MUTATING`, or `LOCKED`.

### 6. `SemanticGenome`
The persistent record of all identified dimensions and their state for a specific goal.
- **Locking Mechanism**: Prevents re-mutating already resolved technical decisions.
- **Mutation History**: Records which strategies were used for each dimension.

### 7. `TrajectoryTerritoryMapper`
A stabilization agent that generates blueprints for siblings.
- **Dimension Mandate**: Ensures siblings compete *exclusively* on the active mutation dimension while fixing other architectural variables.
- **Divergence**: Uses history to ensure each new sibling provides a unique branch in the technical territory.

## Iterative Loop Interaction

1. **Intent Analysis**: `DarwinEngine` triggers intent expansion to identify initial dimensions.
2. **Genome Seeding**: `DimensionEngine` initializes the `SemanticGenome` with these dimensions.
3. **Dimension Selection**: `GenomeDimensionScheduler` picks the most significant unlocked dimension.
4. **Sibling Generation**: `SiblingGenerationManager` uses `TrajectoryTerritoryMapper` to spawn competing blueprints for the active dimension.
5. **Selection & Locking**: Once a winner is chosen, its dimension is `LOCKED` in the genome, and the loop proceeds to the next dimension.
6. **Exhaustion & Discovery**: If no unlocked dimensions remain, `DimensionDiscoveryAgent` is called to expand the genome.

## Dimension Creation Points

Technical dimensions are instantiated in the following locations:

1.  **`IntentExpansionEngine.expand(...)`**:
    - **Initial Discovery**: Created during the first phase of orchestration based on user intent analysis.
    - **Metadata**: Sets `id`, `description`, `abstractionLevel`, `semanticDomain`, `significanceScore`, and `ambiguityScore`.

2.  **`DimensionDiscoveryAgent.discover(...)`**:
    - **Genome Expansion**: Created when all existing dimensions are locked but the goal is not yet satisfied.
    - **Grounding**: Uses `TargetRealityModel` (hotspots/facts) to discover project-specific decision points.
    - **Metadata**: Sets composite scoring fields including `evolutionaryPressure`.

3.  **`DimensionEngine.selectNextDimension(...)`**:
    - **Fallback**: Creates a default `IMPLEMENTATION` dimension if no other dimensions are available.
