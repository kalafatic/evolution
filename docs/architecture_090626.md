# System Architecture Overview - AI Evolution Platform (090626)

## 1. System Overview
The AI Evolution Platform is a modular, capability-based evolution kernel designed to coordinate small local LLMs for autonomous and semi-autonomous software development. It operates as a deterministic state-transition engine that manages the lifecycle of developer intents through a structured evolutionary process.

## 2. Architecture Type
The system is a **Modular OSGi Monolith** built with Eclipse RCP/Tycho. It employs an **Orchestration-First** design where a centralized kernel governs all state transitions and AI agents act as pluggable functional coprocessors.

## 3. Component Map (Logical Grouping)
- **Kernel Control Plane**: `IterationManager`, `AuthorityEngine`, `PhaseEngine`, `RuntimeInvariant`.
- **Evolutionary Engine**: `DarwinEngine`, `TrajectoryTerritoryMapper`, `DarwinDiversityAnalyzer`.
- **Semantic Layer**: `SemanticWorkspace`, `ContextCurator`, `RealityDiscoveryAgent`.
- **Execution Layer**: `TaskExecutor`, `GitEvolutionAdapter`, `KernelScheduler`.
- **Model Layer**: EMF-based `Orchestrator`, `Task`, `Iteration` definitions.
- **Communication Layer**: `RuntimeEventBus`, `SignalBus`.

## 4. Runtime Execution Flow (The PEV Loop)
The system executes via the **Plan-Execute-Verify (PEV)** core loop:
1.  **PLAN**: `PlannerAgent` and `IntentExpansionEngine` decompose user goals into structured hypotheses.
2.  **SPAWN**: `DarwinEngine` materializes divergent trajectories (BranchVariants) based on blueprints.
3.  **EVALUATE**: `DarwinDiversityAnalyzer` ensures conceptual distance; `Evaluator` assigns fitness scores.
4.  **SELECT**: `AuthorityEngine` decides the winning trajectory (Manual selection in MEDIATED mode).
5.  **EXECUTE**: `TaskExecutor` applies mutations via the chosen strategy.
6.  **VERIFY**: `ValidatorAgent` and `RealityEngine` assess the post-mutation reality against invariants.

## 5. Data Flow Description
User Prompt → `IntentExpansionEngine` (Semantic Decomp) → `RealityDiscoveryAgent` (Context Grounding) → `DarwinEngine` (Branch Generation) → `SignalBus` (Evaluation Telemetry) → `AuthorityEngine` (Winner Selection) → `GitEvolutionAdapter` (Side Effect Application).

## 6. Dependency Structure
- **utils** (Foundation): Base semantic annotations and logging.
- **model** (Core State): EMF definitions.
- **controller** (Logic): Kernel, Darwin Engine, Agents, and Workflow coordination.
- **view** (UI): Eclipse RCP views and editors (Chat, Viz, Settings).
- **selfdev-genome** (Intelligence): Self-improvement logic and proposal generation.

## 7. Integration Points
- **VCS**: Git (via JGit/Git Evolution Adapter).
- **Build**: Maven (via Tycho and Maven Agents).
- **AI**: Local (Ollama) and Remote (OpenAI/Cloud providers).
- **Model Storage**: XMI persistence for EMF.

## 8. Design Patterns
- **State Machine**: Orchestrates high-level system states (`INIT`, `ANALYZING`, etc.).
- **Darwinian Evolution**: Divergent proposal generation and survival-of-the-fittest ranking.
- **Capability-Based Extension**: Registry for pluggable scheduling and evaluation modules.
- **Throttled Observer**: `RuntimeEventBus` decouples UI updates from high-frequency kernel signals.

## 9. Architectural Risks
- **Coupling**: Tight dependency on OSGi/Eclipse bundles complicates standalone testing and deployment.
- **Hidden State**: Reliance on `SessionBoundaryGuard` for thread-local session tracking requires strict adherence to boundary entering/exiting.
- **Fragile Flows**: Iteration loops depend on successful LLM formatting of JSON blueprints; failure leads to "blueprint collapse."
- **Layout Storms**: High-frequency EMF model updates can trigger excessive UI refreshes if not throttled by the `RuntimeEventBus`.
