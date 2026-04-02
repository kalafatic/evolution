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
- **ApprovalPage**: Centralized hub for reviewing AI-proposed changes, featuring an SVG-based process flow visualization and interactive decision buttons.

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

## 4. Autonomous Self-Development (Deep Dive)

### 4.1 Current Implementation Lifecycle
The self-development system operates as a higher-order orchestration loop managed by the `SelfDevSupervisor`.

1.  **Session Initiation**: Triggered via `AiChatPage`. The supervisor creates a `SelfDevSession` with a set number of maximum iterations (default 5).
2.  **Iteration Cycle (`IterationManager`)**:
    - **Branching**: A dedicated Git branch (`selfdev/<session-id>/<iteration-id>`) is created for each iteration.
    - **Planning (`TaskPlanner`)**: A specialized agent analyzes the codebase and generates 1-5 atomic tasks (e.g., "Improve Javadoc in core bundles" or "Refactor redundant exception handling").
    - **Execution (`TaskExecutor`)**: Tasks are delegated back to the `EvolutionOrchestrator`, which uses the full agent suite (`Architect`, `JavaDev`, etc.) to implement the changes.
    - **Evaluation (`Evaluator`)**: A automated `maven clean install` is triggered. Success is defined by a `BUILD SUCCESS` status and a test pass rate of 100%.
    - **Decision**:
        - **Success**: Changes are committed to the iteration branch.
        - **Failure**: The supervisor triggers a `Git rollback` to the state before the iteration began.
3.  **Persistence**: The `RestartManager` ensures that if the system needs to restart (e.g., after a core model update), the session state is preserved and can resume automatically.

### 4.2 Identified Architectural Flaws
- **Loop Safety**: The `EvolutionOrchestrator` looping logic lacks an explicit hard-limit counter within the code, relying on agent-driven termination which could lead to infinite execution if the agent fails to converge.
- **State Volatility**: The `EvolutionOrchestrator` clears existing tasks (`context.getOrchestrator().getTasks().clear()`) at the start of every new request. This prevents multi-turn manual refinement of a single orchestration plan.
- **Thread Safety**: UI updates for progress and status in `AiChatPage` use `Display.asyncExec` but lack robust `isDisposed()` checks in some long-running timer blocks, potentially causing widget disposal errors during editor closure.
- **Fragile Parsing**: The `Evaluator` and `ReviewerAgent` rely on string-matching/substring logic for JSON extraction and Maven output parsing, which is prone to failure if AI output includes unexpected conversational text or Maven output format changes.

### 4.3 Strategic Improvements: User Control & Feedback
To move from "Black Box" automation to "Co-pilot" autonomy, the following architectural enhancements are suggested:

1.  **Interactive Planning**:
    - **Current**: `TaskPlanner` generates and immediately commits tasks for execution.
    - **Proposed**: Pause after `Planning` to allow the user to modify, re-order, or delete proposed tasks in the `ApprovalPage` before execution begins.
2.  **Visual Git Diff Integration**:
    - **Current**: Users see logs of file writes.
    - **Proposed**: Embed a side-by-side Git diff viewer in the `ApprovalPage`. Users should be able to see exactly what code was modified before approving the transition from an iteration branch to the main branch.
3.  **Step-by-Step Execution Mode**:
    - **Current**: Iterations run to completion (success or failure).
    - **Proposed**: A "Manual Step" toggle in `SelfDevSettings`. When active, the supervisor pauses after *every* task completion within an iteration, updating the `ApprovalPage` with the task result and requiring a manual "Continue" click.
4.  **Reactive Re-planning Feedback**:
    - **Current**: If a task fails, the entire iteration is rolled back.
    - **Proposed**: Implement a `CorrectionLoop`. If a task fails during execution, trigger a `RepairAgent` to analyze the error log and propose a fix for *just that task*, rather than discarding the entire iteration's progress.
5.  **Telemetry & Transparency**:
    - Add a "Rationale" field to the `SelfDevSession` model. Each iteration should include a section where the AI explains *why* it chose specific improvements, visible in a dedicated tooltip or info panel in the `ApprovalPage`.
6.  **Degraded Offline Fallback**:
    - Enhance `LlmRouter` to support `DegradedMode`. If a remote provider is unreachable during a `HYBRID` iteration, the system should offer to proceed using only the local `Ollama` model (if capability permits) instead of hard-failing the iteration.
