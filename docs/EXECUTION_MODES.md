# Execution Modes

## 1. Platform Modes
Platform modes define the environment and infrastructure where the AI operates.

- **LOCAL**: Execution occurs entirely on the local machine using local LLMs (e.g., Ollama).
- **PROXY**: Requests are routed through a proxy to remote services.
- **HYBRID**: Combines local execution for simple tasks with remote LLMs for complex reasoning.
- **REMOTE**: Full reliance on remote API providers.
- **MEDIATED**: A supervision mode where every significant action requires human approval.

## 2. Orchestration Behaviors
Behaviors define *how* the kernel approaches a task.

- **ITERATIVE**: The kernel performs light refinement loops to converge on a solution.
- **DARWIN**: Full evolutionary branching where multiple hypotheses are explored in parallel.
- **ATOMIC**: Single-step execution for simple, high-confidence intents.
- **SELF-DEVELOPMENT**: The kernel operates on its own source code to improve itself.

## 3. Mode Routing
The `ModeRouter` dynamically selects the appropriate flow based on:
- Intent classification (Keywords, EPS).
- User settings.
- Model capability and availability.

## 4. MEDIATED AI
Mediated execution is a core philosophy. The system provides transparency by:
- Presenting Darwin variants for review.
- Waiting for step-by-step approval in high-risk scenarios.
- Allowing manual selection or editing of proposals before execution.
