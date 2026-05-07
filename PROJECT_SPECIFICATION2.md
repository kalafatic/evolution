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
- **EvolutionConstants**: Centralized repository for system-wide constants (Agent types, tool names, default parameters).
- **ToolFactory & AgentFactory**: Architectural registries that decouple component creation from their usage, following the Factory Pattern.
- **EvolutionOrchestrator**: Implements the multi-stage task lifecycle:
    1. **Planning**: `PlannerAgent` decomposes natural language requests into a sequence of `Task` objects.
    2. **Approval Pause**: Mandatory execution block using `CompletableFuture` after planning.
    3. **Plan–Execute–Verify (PEV) Loop**: Every task follows a sophisticated **6-phase** internal cycle:
        - **PLAN (Tactical)**: Agent creates a structured JSON plan (steps, target files, strategy).
        - **CONTEXT**: `ContextBuilder` gathers minimal deterministic context (code/dependencies).
        - **EXECUTE**: Agent performs the task; technical outputs are stored in `artifacts`.
        - **VERIFY**: `ReviewerAgent` and `ConstraintAgent` evaluate results against goals and architectural guardrails.
        - **ANALYZE**: `AnalyticAgent` diagnoses failures for root causes and repetitive patterns.
        - **MUTATE**: System selects a mutation strategy (e.g., Self-Correction via `RepairAgent` or Escalation) and retries.
    4. **Task-Level Approval**: Optional pause before sensitive operations.
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

1. Initiation
   - Triggered via AiChatPage
   - SelfDevSupervisor starts session
   - Delegates control to IterationManager

2. Iteration Cycle (Kernel-driven)

   STATE: INIT → ANALYZING
   - AnalyticAgent refines request

   STATE: ANALYZING → PLAN_LOCKED
   - DarwinEngine generates candidate improvements
   - IterationManager selects/locks finalPlan

   STATE: PLAN_LOCKED → EXECUTING
   - Git branch created (selfdev/<session>/<iteration>)
   - TaskExecutor executes plan via EvolutionOrchestrator

   STATE: EXECUTING → VERIFYING
   - Evaluator runs build/tests and produces EvaluationResult

   STATE: VERIFYING → (DONE | MUTATING | FAILED)
   - DONE: success criteria met
   - MUTATING: generate new variants (loop continues)
   - FAILED: unrecoverable error

   STATE: MUTATING → PLAN_LOCKED
   - New candidate plan selected

3. Decision (Kernel-controlled)

   - Selection based on fitness score (not just pass/fail)
   - Best variant merged into main branch

4. Persistence

   - Full iteration history stored (event log + snapshots)
   - RestartManager restores state and continues loop

---

## 5. Strategic Roadmap: Future Enhancements

1.  **Visual Git Diff Integration**: Embed a side-by-side diff viewer in the `ApprovalPage` to review code changes before iteration commits.
2.  **Parallel Darwinian Variants**: Implement true evolutionary selection by spawning multiple variant branches in parallel during the EXECUTE phase.
3.  **Step-by-Step Execution Mode**: A toggle to pause after *every* task in the self-development loop, allowing granular user oversight.
4.  **Telemetry & Rationale**: Implement the `rationale` attribute in the model so the AI can explain *why* it proposed specific improvements, visible in a dedicated panel.
5.  **Knowledge Graph**: Build a structured shared memory (Knowledge Graph) to persist architectural insights across sessions.
