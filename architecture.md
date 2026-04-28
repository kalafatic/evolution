# Architecture: Evolution Project

## 1. Core Goal
An agentic, autonomous development environment integrated with Eclipse RCP that allows the system to improve its own codebase through iterative "Darwinian" loops.

## 2. Self-Development Loop (Standard Operating Procedure)
Every prompt/iteration follows this cycle:
1.  **Read Architecture**: Always start by reading `architecture.md` and `PROJECT_SPECIFICATION.md` to maintain context.
2.  **Analyze & Verify**: Verify if the requested change is already implemented or if it can be achieved with minimal changes. **Do not reinvent the wheel.**
3.  **Plan**: Define atomic tasks (File, Maven, Git, Review) in a `plan.json` for the current iteration.
4.  **Execute**: Apply changes with mandatory traceability markers (`// @evo:<iter>:<var> reason=<kebab-case-reason>`).
5.  **Evaluate**: Run Maven builds and tests. Rollback if quality or build fails.
6.  **Document**: Update `CHANGELOG.md` and `architecture.md` (if the blueprint changed) before committing.

## 3. System Blueprint
- **Orchestration**: `EvolutionOrchestrator` implements a strict **Plan–Execute–Verify (PEV)** loop for every task.
- **Routing**: `LlmRouter` uses a **Hybrid Context Builder** approach (Local Ollama gathers repository context, Cloud LLM handles reasoning/coding).
- **Self-Dev Supervisor**: `SelfDevSupervisor` coordinates the iterative cycles, branch management, and automatic rollbacks.
- **Traceability**: Strict enforcement of `@evo` markers in code and `plan.json` in `iterations/<id>/`.
- **UI**: Multi-page editor in Eclipse with specialized tabs for Chat, Approval, and Flow visualization.

## 4. Key Constraints
- **Fail-Fast**: Any validation error (missing markers, unplanned files) terminates the build immediately.
- **Atomic Changes**: Prefer many small, successful iterations over one large, complex change.
- **Traceability**: All self-developed code MUST be traceable back to an iteration and a reason.
