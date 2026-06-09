# System Architecture Overview - AI Evolution Platform (090626)

## 1. System Overview (Ground Truth)
The AI Evolution Platform is a deterministic, state-transition-based evolution kernel designed to coordinate small local LLMs for autonomous software development. It operates as an **Orchestration-First Modular Monolith**, leveraging Eclipse RCP/Tycho for plugin management and the Eclipse Modeling Framework (EMF) for state representation.

## 2. Architecture Classification
- **Type:** Modular OSGi Monolith.
- **Architectural Style:** State-Transition Orchestration with Throttled Category-Based Signaling.
- **Decision Authority:** Centralized `IterationManager` with human-in-the-loop (Mediated) or autonomous selection.

## 3. Component Map (Logical Grouping)

### Kernel Control Plane (`eu.kalafatic.evolution.controller.orchestration`)
- **`IterationManager`**: The authoritative control plane governing the lifecycle of developer intents.
- **`EvolutionPhaseMachine`**: A deterministic 6-phase state graph:
  1. `INTENT_EXPANSION`: Goal decomposition and ambiguity detection.
  2. `ARCHITECTURE_VARIANTS`: Divergent trajectory spawning.
  3. `SELECTION_REFINEMENT`: User/Kernel selection of the winning path.
  4. `IMPLEMENTATION_PLAN`: Step-by-step task generation.
  5. `FINAL_SYNTHESIS`: Outcome summarization.
  6. `TERMINAL_SUCCESS/FAILURE`: Exit states.
- **`SessionBoundaryGuard`**: Enforces strict session isolation using `ThreadLocal<String> currentSessionId`.
- **`RuntimeInvariant`**: Validates session integrity and prohibits global state drift.

### Evolutionary Engine (`eu.kalafatic.evolution.controller.engine`)
- **`DarwinEngine`**: The materializer of architectural lineages.
- **`TrajectoryTerritoryMapper`**: Discovers conceptual blueprints via bulk LLM requests, mapping divergent quadrants of the "Target Reality".
- **`DarwinDiversityAnalyzer`**: Ensures conceptual distance using a 10-dimension vector (Modularity, Resilience, Abstraction, etc.).
- **`DarwinFitnessRanker`**: Scores trajectories based on information density and architectural influence.

### Semantic Reasoning Layer
- **`SemanticWorkspace`**: Persistent reasoning context. Implements **Memory Decay** (factor 0.95) to prune stale artifacts.
- **`ContextCurator`**: Significance-driven context distillation (Significance = Relevance × Centrality × SemanticDensity × Uniqueness).
- **`RealityDiscoveryAgent`**: Constructs the `TargetRealityModel` (domain, hotspots, purpose).

### Execution & Communication
- **`RuntimeEventBus`**: Throttled (100ms) asynchronous signaling system with category-based routing (KERNEL, FLOW, AGENT, EXECUTION, UI, SUPERVISOR, WORKSPACE).
- **`CapabilityRegistry`**: Runtime catalog for technology-neutral contracts (e.g., `ISchedulingContract`).
- **`GitEvolutionAdapter`**: Applies atomic mutations via JGit transactions.

## 4. Runtime Execution Flow (PEV Loop)
1.  **PLAN**: `IntentExpansionEngine` analyzes semantic dimensions.
2.  **SPAWN**: `DarwinEngine` materializes 4-6 divergent trajectories.
3.  **EVALUATE**: `DarwinDiversityAnalyzer` filters duplicates; `Evaluator` assigns fitness.
4.  **SELECT**: `AuthorityEngine` (mediated by `DecisionResolver`) selects the winning lineage.
5.  **EXECUTE**: `TaskExecutor` applies patches; `FileChangeTracker` monitors mutations.
6.  **VERIFY**: `ValidatorAgent` runs `mvn integration-test` or semantic checks.

## 5. Data Flow Description
`User Prompt` → `Intent Decompression` → `Repository Grounding` → `Divergent Branching` → `Throttled Signaling (Bus)` → `Consolidated Selection` → `Git Transaction` → `Verified State`.

## 6. Dependency Structure
- **`eu.kalafatic.utils`**: Base semantic annotations (`@EvolutionComponent`) and logging.
- **`eu.kalafatic.evolution.model`**: EMF-based state definitions (XMI persistence).
- **`eu.kalafatic.evolution.controller`**: Core logic (Kernel, Engine, Agents, Workflow).
- **`eu.kalafatic.evolution.view`**: Eclipse RCP UI (Chat, Architecture Diagram, Progress Monitor).
- **`eu.kalafatic.evolution.selfdev.genome`**: High-level upgrade compiler for cross-project evolution.

## 7. Integration Points
- **VCS**: Native Git integration (JGit).
- **Build**: Maven integration (Tycho/m2e).
- **AI**: Pluggable Local (Ollama) and Remote (OpenAI) model providers.
- **Network**: `NetworkDiscoveryService` for automated host detection.

## 8. Architectural Risks
- **Tight Coupling**: Direct dependencies on OSGi Require-Bundle hierarchy make standalone unit testing difficult.
- **Hidden State**: Potential for session leakage if `SessionBoundaryGuard.exitSession()` is bypassed.
- **Fragile Flows**: High dependence on LLM JSON output for core steering (e.g., blueprints).
- **Layout Storms**: Excessive UI refreshes triggered by high-frequency EMF model updates (mitigated by throttled event bus).
