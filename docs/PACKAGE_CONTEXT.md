# PACKAGE CONTEXT

## Directory: docs/

## Domain: general

## Core Documentation Taxonomy
* `README.md`: Central documentation index and AI navigation map.
* `PACKAGE_CONTEXT.md`: High-level inventory of documentation components and package context (Updated 08/10/2026).
* `architecture_090626.md`: Ground-truth system architecture overview with 08102026 evolution milestone annotations.
* `architecture/`: Canonical system architecture documentation suite:
  * `overview.md`: High-level system architecture, layer taxonomy, design principles, and Genome integration.
  * `modules.md`: OSGi bundle taxonomy, responsibilities, public APIs, and bundle catalog across 29 modules.
  * `dependencies.md`: Package import/export graphs, dependency layering invariants, and coupling analysis.
  * `runtime.md`: OSGi Equinox runtime container, process models, embedded HTTP servers, and DEBUG port isolation.
  * `threading.md`: SWT UI thread boundaries, Eclipse Jobs, background execution, and subprocess I/O safeguards.
  * `configuration.md`: EMF configuration authority (`ResourceManager`), resolution hierarchy, and canonical filesystem boundaries.
  * `problems.md`: Architectural Problem Register detailing discovered technical risks, evidence, and recommendations.

## Subsystem & Feature Documentation File Inventory
* `DARWIN.MD`: Comprehensive architecture specification for Darwin evolutionary agentic loops, parallel branching, mutation strategies, and `IterationManager` state transitions.
* `AI_KERNEL_ARCHITECTURE.md`: Cognitive capability platform specification, decoupling intelligence generation from kernel decision authority.
* `SEMANTIC_WORKSPACE_ARCHITECTURE.md`: Persistent reasoning environment, trajectory memory, context curation, and semantic artifact decay.
* `SELF_DEV_ANALYSIS.md`: Self-Development pipeline analysis, workspace isolation, task graph execution, and Tycho reactor integration.
* `EVO_INFERENCE_ENGINE.md`: Native Java LLM inference engine specification and GGUF runtime bridge.
* `DARWIN_ITERATION.md`: Evolutionary iteration mechanics, mutation branching, evaluation signals, and winning variant consolidation.
* `PROMPT_SYNTHESIS.md`: Transforming evolved understanding of targets into high-signal prompts optimized for LLMs.
* `TARGET_ANALYSIS_PIPELINE.md`: Mediated analysis pipeline phases (Surface Scanning -> Semantic Extraction -> Architecture Inference).
* `SCHEDULING_ARCHITECTURE.md`: Backpressure and execution scheduling layer for Darwin tasks under cognitive resource budgets.
* `ARCHITECTURAL_ANALYSIS.md`: Fundamental cognitive primitives (Intent, BitState, Context, Task, Flow, Iteration, Signal, Trajectory, Variant).
* `IMPROVEMENTS_ANALYSIS.md`: Platform capabilities, PEV loop, hybrid routing, resilient local fallback, and repair agent integration.
* `use_cases_090626.md`: Real-world use cases for atomic, iterative, Darwinian, and mediated workflows.
* `AI_KERNEL_ARCHITECTURE_REFACTOR.md`: Refactor plan for extracting signals from decision authorities.
* `USE_CASE_SIMPLE_TASK.md`: Atomic execution flow for simple high-confidence tasks.
* `AMBIGUITY_LIFECYCLE.md`: Intent expansion, ambiguity detection, hypothesis generation, and clarification strategies.
* `DECISION_AUTHORITY_ARCHITECTURE.md`: ActivationResolver deterministic decision authority for branch activation and ranking.
* `USE_CASE_ITERATIVE_REFACTOR.md`: Multi-pass iterative refactoring with self-correction and build verification.
* `PEER_REVIEW_DESIGN.md`: Peer review UI components, diff view, and change set models.
* `MEDIATED_USE_CASES.md`: Use cases for mediated repository exploration and architecture audit.
* `SUPERVISION_AND_AUTHORITY.md`: Authority hierarchy (Human -> Kernel -> DecisionResolver -> SignalBus -> Agents).
* `STRATEGY_SMALL_MODELS.md`: Architecture strategies for orchestrating small local LLMs effectively.
* `CONTEXT_CURATION.md`: Significance-driven context selection to prevent token floods.
* `CHANGELOG.md`: Chronological log of iteration changes and architecture updates.
* `MIGRATION_SIGNAL_ARCHITECTURE.md`: Standardized telemetry DTO (`EvaluationSignal`) for evaluator observations.
* `USE_CASE_MEDIATED_EXECUTION.md`: High-risk architectural change execution in mediated mode.
* `GENERAL_MEDIATED_MODE.md`: Evolutionary intelligence mediation layer for deep target analysis.
* `STABILIZATION.md`: Deterministic state transitions, cognitive tracing, and authority enforcement.
* `EXECUTION_MODES.md`: Infrastructure modes (LOCAL, PROXY, HYBRID, REMOTE) and behavior profiles (ITERATIVE, DARWIN, ATOMIC).
* `USE_CASE_SELF_DEVELOPMENT.md`: Self-modification and architecture optimization flow.
* `INTENT_EXPANSION_ARCHITECTURE.md`: Structured exploration of the intent space before variant generation.
* `EVENT_SIGNAL_TRAJECTORY.md`: Throttled category-based RuntimeEventBus and trajectory memory tracking.
* `HYBRID_MODE_DESIGN.md`: Local context curation paired with remote deep reasoning and automatic local fallback.
