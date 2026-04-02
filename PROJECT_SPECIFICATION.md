# Evolution Project: Architecture Specification

## 1. Overview
The Evolution Project is an agentic, AI-driven development environment integrated with the Eclipse RCP platform. It utilizes an EMF-based (Eclipse Modeling Framework) domain model to orchestrate autonomous development tasks, leveraging local (Ollama) and remote (OpenAI-compatible) LLM providers.

---

## 2. Core Architectural Components

### 2.1 Domain Model (EMF)
- **EvoProject**: The root container for orchestrations.
- **Orchestrator**: The central configuration entity. Manages `AiMode` (LOCAL, HYBRID, REMOTE), LLM settings, and a collection of `Task` objects.
- **Task**: Represents an atomic unit of work (e.g., `file`, `maven`, `git`, `llm`, `approval`). Includes status tracking, retry logic, and looping capabilities.
- **SelfDevSession**: Manages the state of autonomous improvement loops.

### 2.2 Controller Logic (Orchestration Engine)
- **EvolutionOrchestrator**: Implements the task execution lifecycle:
    1. **Planning**: `PlannerAgent` decomposes natural language requests into a sequence of `Task` objects.
    2. **Execution Loop**: Iterates through tasks, dispatching to specialized agents (`Architect`, `JavaDev`, `Tester`, `Reviewer`) or tools (`FileTool`, `MavenTool`, `GitTool`).
    3. **Evaluation**: `ReviewerAgent` validates task output using AI-driven JSON-based criteria.
    4. **Looping**: Supports jumping back to previous tasks based on `loopToTaskId` for iterative refinement.
- **LlmRouter**: Dispatches requests based on `AiMode`.
    - **HYBRID Mode**: Implements a 3-step sequence:
        1. Local Optimization: Ollama refines the user prompt.
        2. Remote Execution: A large remote model processes the optimized prompt.
        3. Local Simplification: Ollama simplifies the remote response for the user.

### 2.3 View Layer (UI)
- **MultiPageEditor**: The main editor for `.evo` or `.xml` orchestration files, featuring tabs for `AiChatPage`, `GraphPage`, `PropertiesPage`, `AiFlowPage`, and `ApprovalPage`.
- **AiChatPage**: The primary interaction interface for real-time AI communication and manual orchestration triggering.
- **GraphPage**: Provides Zest-based visualization of the orchestration graph.

---

## 3. User Use-Cases (Step-by-Step)

### 3.1 Initial Project Setup
1. **Launch Wizard**: Navigate to `File -> New -> Project -> Evolution -> New Evo Project`.
2. **Project Naming**: Enter project name and choose location.
3. **Configuration**:
    - Configure Git (URL, branch, credentials).
    - Configure Ollama (URL, default local model).
    - Configure LLM settings (default temperature, specific model names).
    - Configure Maven goals and profiles.
4. **Completion**: Click `Finish`. The wizard creates the project structure, a default `.evo` file, and opens the `MultiPageEditor`.

### 3.2 AI Chat & Manual Orchestration
1. **Select AI Mode**: In `AiChatPage`, choose `LOCAL`, `HYBRID`, or `REMOTE` from the dropdown.
2. **Configure Remote Provider**: If using `HYBRID` or `REMOTE`, select a provider (e.g., `DeepSeek`, `OpenAI`), enter the API Token, and verify the API URL.
3. **Test Connection**: Click `Test Connection`. The `LlmRouter` verifies connectivity to the selected provider.
4. **Submit Request**: Write a natural language request (e.g., "Implement a Fibonacci utility in Java") and press `Send`.
5. **Monitor Progress**:
    - The `PlannerAgent` generates a list of tasks.
    - Tasks appear in the `GraphPage` and `AiFlowPage`.
    - Real-time logs appear in the `AiChatPage` history.
6. **Approval**: If a task requires approval (e.g., a `file` write or `git` commit), the UI displays "Approve" and "Reject" buttons. The orchestrator waits for user input.

### 3.3 Autonomous Self-Development
1. **Initiate Self-Dev**: Click the `🚀 Self-Dev` button in `AiChatPage`.
2. **Autonomous Loop**:
    - `SelfDevSupervisor` starts a session.
    - `IterationManager` plans improvements, executes them on a temporary Git branch, and runs Maven tests.
    - If successful, changes are merged; if they fail, the supervisor attempts a rollback and retries up to 3 times.

---

## 4. Identified Architectural Flaws & Missing Steps

### 4.1 Identified Flaws
- **Loop Safety**: The `EvolutionOrchestrator` looping logic lacks an explicit hard-limit counter within the code, relying on agent-driven termination which could lead to infinite execution if the agent fails to converge.
- **State Volatility**: The `EvolutionOrchestrator` clears existing tasks (`context.getOrchestrator().getTasks().clear()`) at the start of every new request. This prevents multi-turn manual refinement of a single orchestration plan.
- **Thread Safety**: UI updates for progress and status in `AiChatPage` use `Display.asyncExec` but lack robust `isDisposed()` checks in some long-running timer blocks, potentially causing widget disposal errors during editor closure.

### 4.2 Missing Steps / Improvements
- **Context Persistence**: Missing a mechanism to persist `TaskContext.sharedMemory` between different editor sessions; memory is currently transient and exists only during active execution.
- **Tooling Verification**: The `FileTool` and `MavenTool` lack a "Dry Run" mode to preview changes before application, placing high reliance on the `ReviewerAgent`.
- **Offline Resilience**: `HYBRID` mode fails completely if the remote provider is unreachable, even if the local model could handle the request in a degraded "local-only" fallback state.
