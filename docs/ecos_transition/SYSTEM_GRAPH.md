# SYSTEM_GRAPH.md

## Overview
This document maps the factual architecture of the EVO platform as identified in the current repository.

## Modules

### eu.kalafatic.evolution.controller
- **Name**: Evolution Controller
- **Purpose**: The primary orchestration and intelligence layer of the system.
- **Inputs**: TaskRequests, User Input, Repository State.
- **Outputs**: Code changes, OrchestratorResponses, Evolution Signals.
- **Dependencies**: `eu.kalafatic.evolution.model`, `eu.kalafatic.utils`

### eu.kalafatic.evolution.model
- **Name**: Evolution Model
- **Purpose**: Defines the EMF-based data structures and domain models for orchestration and evolution.
- **Inputs**: UNKNOWN
- **Outputs**: UNKNOWN
- **Dependencies**: Eclipse EMF

### eu.kalafatic.evolution.supervisor
- **Name**: Evolution Supervisor
- **Purpose**: Provides autonomous supervision, external iteration management, and self-development monitoring.
- **Inputs**: Process status, Result files.
- **Outputs**: Health signals, Restart commands.
- **Dependencies**: `eu.kalafatic.evolution.controller` (subset)

### eu.kalafatic.evolution.view
- **Name**: Evolution View
- **Purpose**: Provides the UI interface for interacting with the evolution kernel (Eclipse based).
- **Inputs**: Runtime Events, User Interactions.
- **Outputs**: UI updates.
- **Dependencies**: `eu.kalafatic.evolution.controller`, `eu.kalafatic.evolution.model`

---

## Core Components

### IterationManager
- **Name**: IterationManager
- **Purpose**: The Kernel Control Plane. Sole authority for state transitions and strategic orchestration.
- **Inputs**: TaskRequest, TaskContext.
- **Outputs**: OrchestratorResponse, EvolutionDecision.
- **Dependencies**: `DarwinEngine`, `Evaluator`, `GitManager`, `StabilityAnalyzer`.

### DarwinEngine
- **Name**: DarwinEngine
- **Purpose**: Materializer of architectural lineages and generator of evolutionary mutations.
- **Inputs**: Goal, StateSnapshot, FailureMemory, Trajectory, Pressure.
- **Outputs**: List of BranchVariants.
- **Dependencies**: `AiService`, `IterationMemoryService`.

### IntentExpansionEngine
- **Name**: IntentExpansionEngine
- **Purpose**: Analyzes user requests and constructs the evolutionary search space by identifying semantic dimensions.
- **Inputs**: Prompt, TaskContext.
- **Outputs**: IntentExpansionResult.
- **Dependencies**: `AiService`, `StructuredResponsePipeline`.

### StabilityAnalyzer
- **Name**: StabilityAnalyzer
- **Purpose**: Evaluates architectural equilibrium to determine convergence and phase progression.
- **Inputs**: Trajectory, TaskContext, Pressure.
- **Outputs**: Stability score (double), Convergence status (boolean).
- **Dependencies**: UNKNOWN

### EvolutionaryPressureEngine
- **Name**: EvolutionaryPressureEngine
- **Purpose**: Identifies persistent forces (ambiguity, resilience, extensibility) driving recursive evolution.
- **Inputs**: Trajectory, TaskContext.
- **Outputs**: EvolutionaryPressureVector.
- **Dependencies**: UNKNOWN

### LlmRouter (ModeRouter)
- **Name**: LlmRouter
- **Purpose**: Manages and routes AI requests across Local, Remote, Hybrid, and Mediated modes.
- **Inputs**: Request, Orchestrator model.
- **Outputs**: PlatformMode.
- **Dependencies**: UNKNOWN

---

## Subsystems

### Lineage System
- **Name**: Lineage System
- **Purpose**: Tracks evolutionary continuity through Trajectories and IterationRecords.
- **Inputs**: Decisions, Mutation traces.
- **Outputs**: Historical cognitive memory.
- **Dependencies**: `IterationMemoryService`, `TrajectoryMemory`.

### Mediated Mode Mechanisms
- **Name**: Mediated Mode
- **Purpose**: Evolves optimized context packages (mediation packages) for external LLMs.
- **Inputs**: User request, Repository snapshot.
- **Outputs**: Mediation Package (ZIP).
- **Dependencies**: `ContextCurator`, `PromptSynthesizer`, `MediatedExportManager`.

### Darwin Evolution Mechanisms
- **Name**: Darwin Flow
- **Purpose**: Orchestrates the generate-evaluate-select-apply loop for multi-branch evolution.
- **Inputs**: TaskContext, Goal.
- **Outputs**: Applied code changes or cognitive updates.
- **Dependencies**: `IterationManager`, `DarwinEngine`.

---

## Workflows and Data Flows

### Evolutionary Loop
1. **Discovery**: `StructureAgent` / `TargetScanner` scan the repository.
2. **Analysis**: `IntentExpansionEngine` discovers semantic dimensions.
3. **Mutation**: `DarwinEngine` spawns divergent trajectories (BranchVariants).
4. **Evaluation**: `Evaluator` and specialized agents produce `EvaluationSignals`.
5. **Selection**: `AuthorityEngine` (via `IterationManager`) selects a survivor.
6. **Application**: `DarwinFlow` / `TaskExecutor` apply the winning mutation.
7. **Convergence**: `StabilityAnalyzer` checks for equilibrium.

### Mediated Workflow
1. **Semantic Extraction**: `SemanticExtractor` analyzes high-value files.
2. **Context Selection**: `ContextCurator` selects 4-16 high-signal files.
3. **Prompt Synthesis**: `PromptSynthesizer` generates optimized instructions.
4. **Export**: `MediatedExportManager` bundles the artifact into a ZIP.

---

## User Interaction Points
- **AI Chat UI**: Primary interface for user prompts and feedback.
- **Progress Monitor**: Visualizes the evolution pipeline and variant status.
- **Approval Prompts**: Human-in-the-loop decision points for variant selection and intent confirmation.

---

## Storage and Persistence
- **EMF Resource**: Persistence for `ChatSession`, `Orchestrator`, and `Task` models.
- **Iteration Memory**: File-based storage (JSON checkpoints) for evolutionary records and trajectories.
- **Git Repository**: Version control for artifact lineage and parallel branch execution.

---

## APIs
- **OrchestratorService**: External API for submitting tasks and managing sessions.
- **KernelFacade**: Internal API for routing requests to the `IterationManager`.
- **AiService**: Abstract API for LLM communication (Ollama, OpenAI, Gemini).
