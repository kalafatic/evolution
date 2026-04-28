# Evolution Project: Architecture Specification

## 1. Overview
The Evolution Project is an agentic, AI-driven development environment integrated with the Eclipse RCP platform. It utilizes an EMF-based (Eclipse Modeling Framework) domain model to orchestrate autonomous development tasks, leveraging local (Ollama) and remote (OpenAI-compatible, Gemini) LLM providers through a sophisticated hybrid routing system.

---

## 2. Core Architectural Components

### 2.1 Domain Model (EMF)
- **EvoProject**: The root container for multiple orchestrations.
- **Orchestrator**: The central configuration entity.
    - **AI Settings**: Manages `AiMode` (LOCAL, HYBRID, REMOTE), API tokens, and mode-specific models (`localModel`, `hybridModel`, `remoteModel`).
    - **Protocol Settings**: Persistent `mcpServerUrl` for Model Context Protocol integration.
    - **Tool References**: Containment for `Git`, `Maven`, `Database` (JDBC settings), and `FileConfig` (Project Root).
    - **Memory**: `sharedMemory` attribute for persisting context across orchestration sessions.
- **Task**: Represents an atomic unit of work (e.g., `file`, `maven`, `git`, `llm`, `approval`, `shell`).
    - **Execution Control**: `approvalRequired` (boolean), `loopToTaskId` (for iterative refinement), and `priority` (integer).
    - **Task states**: `PENDING`, `PLANNING`, `EXECUTING`, `VERIFYING`, `DONE`, `FAILED`.
    - **Evolutionary Data**: `goal` (high-level intent), `plan` (structured steps), `artifacts` (technical outputs).
    - **Documentation**: `description`, `response`, `feedback`, and `resultSummary`.
- **SelfDevSession**: Manages the state of autonomous improvement loops, containing multiple `Iteration` objects, each tracking its own `Task` list and `EvaluationResult`.

### 2.2 Controller Logic (Orchestration Engine)
- **EvolutionOrchestrator**: Implements the multi-stage task lifecycle:
    1. **Planning**: `PlannerAgent` decomposes natural language requests into a sequence of `Task` objects.
    2. **Approval Pause**: Mandatory execution block using `CompletableFuture` after planning.
    3. **Plan–Execute–Verify (PEV) Loop**: Every task follows an internal 3-phase cycle:
        - **Plan**: Agent creates a structured JSON plan (steps, target files, strategy).
        - **Execute**: Agent performs the task using tools or reasoning; technical outputs are stored in `artifacts`.
        - **Verify**: `ReviewerAgent` evaluates results.
    4. **Darwinian Mutation**: On verification failure, the system selects a mutation strategy (e.g., Syntactic vs. Logic fix) based on failure type and retries with a modified plan.
    5. **Task-Level Approval**: Optional pause before sensitive operations.
- **LlmRouter**: The central dispatching hub.
    - **HYBRID Mode (Context Builder Pattern)**: A signature 3-step sequence:
        1. **Local Context Builder**: Ollama gathers repository context, technical briefings, and constraints.
        2. **Remote Reasoner**: A large remote model (e.g., DeepSeek, GPT-4, Gemini) performs reasoning and coding.
        3. **Local Verification**: Ollama performs a quick safety/format check on the remote output.
- **Support Services**:
    - **NeuronService**: Provides context-aware code completion (Ctrl+Space) by training on user interactions.
    - **McpClient**: Implements the Model Context Protocol to fetch external resources/context.

### 2.3 View Layer (UI)
- **MultiPageEditor**: The primary workspace for `.evo` files, coordinating the following tabs:
    - **AiChatPage**: Main interaction hub with rich text logging, thread management, and a "🚀 Self-Dev" trigger. Includes real-time mode feedback (Color-coded indicator).
    - **ApprovalPage**: Control center for reviewing proposed plans. Features a `TableViewer` for re-ordering (Move Up/Down), renaming, or deleting tasks, and an SVG-based process flow visualization.
    - **ToolsPage**: Unified configuration for Git, Maven, File, and Database tool settings.
    - **McpSettingsPage**: UI for configuring MCP servers and exploring available resources.
    - **AiFlowPage & GraphPage**: Visualizations of the task graph using SVG and Zest respectively.
    - **PropertiesPage**: Deep configuration for Ollama, LLM parameters, and Agent behaviors.

---

## 3. User Use-Cases (Step-by-Step)

### 3.1 Initial Project Setup
1. **Launch Wizard**: Navigate to `File -> New -> Project -> Evolution -> New Evo Project`.
2. **Project Naming**: Enter project name and choose location.
3. **Configuration**: The wizard initializes the structure, and the user configures tool paths (Maven, Git) in the `ToolsPage`.
4. **AI Setup**: Configure Ollama URL and Remote Tokens in `AiChatPage` or `PropertiesPage`. Use `Test Connection` to verify.

### 3.2 AI Chat & Manual Orchestration
1. **Select AI Mode**: Choose `LOCAL`, `HYBRID`, or `REMOTE`.
2. **Submit Request**: Write a request (e.g., "Implement Javadoc for the model package").
3. **Plan Review**:
    - The editor automatically switches to the `ApprovalPage`.
    - User inspects the task list, modifies descriptions, or changes order.
    - Click `Approve & Apply` to start execution.
4. **Execution Tracking**:
    - Monitor progress via the `System Status` group and real-time logs in `AiChatPage`.
    - Tasks update colors in `AiFlowPage` (Yellow=Running, Green=Done, Red=Failed).
5. **Interactive Feedback**: Respond to approval prompts for individual tasks if configured.

---

## 4. Autonomous Self-Development (Deep Dive)

### 4.1 Implementation Lifecycle
Managed by the `SelfDevSupervisor`, this loop allows the system to improve itself iteratively.

1.  **Initiation**: Triggered via `AiChatPage`. The supervisor creates a `SelfDevSession`.
2.  **Iteration Cycle (`IterationManager`)**:
    - **Branching**: A dedicated Git branch (`selfdev/<session-id>/<iteration-id>`) is created.
    - **Planning (`TaskPlanner`)**: Analyzes the codebase to generate 1-5 atomic improvements.
    - **Execution (`TaskExecutor`)**: Dispatches tasks to the `EvolutionOrchestrator`.
    - **Evaluation (`Evaluator`)**: Executes Maven builds and parses output to determine success.
3.  **Decision**:
    - **Success**: Iteration branch is committed and prepared for merge.
    - **Failure**: Automatic `Git rollback` is triggered to preserve environment stability.
4.  **Persistence**: `RestartManager` ensures session continuity across platform restarts.

### 4.2 Identified Architectural Flaws
- **Loop Safety**: The orchestration loop relies on agent-driven convergence; a global hard-limit counter is needed to prevent infinite loops in edge cases.
- **Thread Safety**: Long-running UI updates in `AiChatPage` and `PropertiesPage` require more robust `isDisposed()` checks to prevent platform errors during editor closure.
- **Parsing Fragility**: `ReviewerAgent` and `Evaluator` utilize string matching and index-of logic for JSON/Maven parsing, which is sensitive to AI verbosity or environment-specific output changes.

---

## 5. Strategic Roadmap: Future Enhancements

1.  **Visual Git Diff Integration**: Embed a side-by-side diff viewer in the `ApprovalPage` to review code changes before iteration commits.
2.  **Correction Loop (RepairAgent)**: Instead of immediate rollback on task failure, trigger a specialized agent to analyze logs and propose a surgical fix.
3.  **Step-by-Step Execution Mode**: A toggle to pause after *every* task in the self-development loop, allowing granular user oversight.
4.  **Telemetry & Rationale**: Implement the `rationale` attribute in the model so the AI can explain *why* it proposed specific improvements, visible in a dedicated panel.
5.  **Degraded Offline Fallback**: Enable `LlmRouter` to automatically switch to `LOCAL` mode if remote providers become unreachable during a session.
