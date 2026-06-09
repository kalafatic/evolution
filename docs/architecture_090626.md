# System Architecture Overview - AI Evolution Platform (090626)

## 1. System Overview
The AI Evolution Platform is a modular, capability-based evolution kernel designed to coordinate small local LLMs for autonomous and semi-autonomous software development. It operates as a deterministic state-transition engine that manages the lifecycle of developer intents through a structured evolutionary process.

## 2. High-Level Architecture
The system follows an **Orchestration-First** architectural style, where a centralized kernel (`IterationManager`) governs the execution flow, treating AI models as pluggable coprocessors rather than autonomous controllers.

### Conceptual Diagram
```text
[ User Interface ] <--> [ Event Bus ] <--> [ Iteration Manager (Kernel) ]
                                                |
        -------------------------------------------------------------------------
        |               |               |               |               |
[ Model (EMF) ] [ Darwin Engine ] [ Evaluators ] [ VCS (Git) ] [ Build (Maven) ]
        |               |               |               |               |
        -------------------------------------------------------------------------
                                |
                        [ AI Service / LLM ]
```

## 3. Core Modules and Responsibilities
- **`eu.kalafatic.evolution.model`**: Defines the system state using the Eclipse Modeling Framework (EMF). It contains the `Orchestrator`, `Task`, `Iteration`, and `SelfDevSession` models.
- **`eu.kalafatic.evolution.controller`**: The heart of the system.
    - `orchestration`: Contains `IterationManager`, the single authority for state transitions.
    - `engine`: Implements `DarwinEngine` for generating divergent evolutionary branches.
    - `agents`: Modular AI agents (Planner, Critic, Analyst, etc.) that perform specific tasks.
    - `mediation`: Manages context curation and external export for complex tasks.
- **`eu.kalafatic.evolution.view`**: The Eclipse RCP-based UI, providing views for chat, architecture visualization, and session management.
- **`eu.kalafatic.evolution.selfdev.genome`**: An AI-first runtime for cross-project knowledge sharing and self-improvement proposals.
- **`eu.kalafatic.utils`**: Shared semantic utilities and annotations.

## 4. Data Flow (The PEV Loop)
The platform executes tasks through a **Plan-Execute-Verify (PEV)** loop:
1.  **Input**: User provides a prompt or intent.
2.  **Analysis**: `IntentExpansionEngine` analyzes the intent and identifies semantic uncertainty.
3.  **Discovery**: `RealityDiscoveryAgent` builds a model of the target repository.
4.  **Branching**: `DarwinEngine` generates multiple divergent trajectories (BranchVariants).
5.  **Evaluation**: `FitnessEngine` scores variants based on stability and goal alignment.
6.  **Selection**: The user (Mediated) or Kernel (Auto) selects the winning trajectory.
7.  **Execution**: `TaskExecutor` applies the changes via the chosen strategy.
8.  **Verification**: `ValidatorAgent` checks the outcome (build, tests, or semantic validation).
9.  **Synthesis**: `FinalResponseAgent` summarizes the results for the user.

## 5. Execution Lifecycle
The system state transitions through a formal machine:
`INIT` → `ANALYZING` → `(CLARIFYING)` → `EXECUTING` → `VERIFYING` → `DONE` / `FAILED`

## 6. Integration Points
- **VCS**: Native Git integration for branch management and change tracking.
- **Build Systems**: Maven integration for dependency management and running tests.
- **AI Models**: Support for local models (Ollama) and remote APIs (OpenAI).
- **Persistence**: XMI-based persistence for EMF models and `IterationMemoryService` for trajectory history.

## 7. Key Design Patterns
- **State Machine**: Centralized authority for system transitions.
- **Darwinian Evolution**: Divergent proposal generation and survival-of-the-fittest selection.
- **Capability Registry**: Pluggable architecture for engines and evaluators.
- **Observer/Event Bus**: Throttled UI updates and decoupled communication.

## 8. Architectural Risks
- **Local Model Constraints**: Dependence on the performance and context window of small local LLMs.
- **OSGi Complexity**: High overhead for dependency resolution and plugin management due to Tycho/Eclipse environment.
- **State Synchronization**: Ensuring the EMF model, Git state, and UI remain consistent during high-frequency iterations.
