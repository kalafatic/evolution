# Architecture: Evolution Project

## 1. Core Goal
An agentic, autonomous development environment integrated with Eclipse RCP that allows the system to improve its own codebase through iterative "Darwinian" loops.

## 2. Self-Development Loop (Standard Operating Procedure)
Every prompt/iteration follows this cycle:
1.  **Read Architecture**: Always start by reading `architecture.md` and `PROJECT_SPECIFICATION.md` to maintain context.
2.  **Analyze & Verify**: Use `AnalyticAgent` to verify if the requested change is already implemented or if it can be achieved with minimal changes. **Do not reinvent the wheel.**
3.  **Plan**: Define atomic tasks (File, Maven, Git, Review) in the EMF model or a `plan.json` for the current iteration.
4.  **Execute**: Apply changes with mandatory traceability markers (`// @evo:<iter>:<var> reason=<kebab-case-reason>`). Follow the **6-Phase Execution Pipeline**.
5.  **Evaluate**: Run Maven builds and tests via `Evaluator`. Rollback if quality or build fails.
6.  **Document**: Update `CHANGELOG.md` and `architecture.md` (if the blueprint changed) before committing.

## 3. System Blueprint
- **Orchestration**: `EvolutionOrchestrator` implements a 6-phase **Plan–Execute–Verify (PEV)** loop for every task:
    1.  **PLAN (Tactical)**: Determine the specific technical approach for the task.
    2.  **CONTEXT**: Gather minimal, relevant code and dependency context via `ContextBuilder`.
    3.  **EXECUTE**:
        - **GeneratePatch**: LLM generates the `ChangeUnit`.
        - **ApplyPatch**: Supervisor-level application (signaled by Orchestrator).
    4.  **VERIFY**: Evaluate the result using the unified `ValidatorAgent`.
    5.  **ANALYZE**: If failed, diagnose the root cause and check **PROGRESS** (IMPROVED/SAME/WORSE) using `AnalyticAgent`.
    6.  **MUTATE**: Adjust strategy (Self-Correction, Repair, or Escalation) based on diagnosis.

### PEV Loop Pseudo-code
```python
def pev_loop(task):
    for attempt in range(MAX_RETRIES):
        # 1. PLAN
        plan = agent.plan(task)
        # 2. CONTEXT
        context = ContextBuilder.build(task, arch_context)
        # 3. EXECUTE
        patch = agent.generatePatch(context, plan)
        change_unit = ChangeUnit(patch)
        result = applyPatch(change_unit)
        # 4. VERIFY
        eval = Validator.evaluate(result)
        if eval.success: return DONE
        # 5. ANALYZE
        diagnosis = AnalyticAgent.diagnose(result, eval.feedback)
        # 6. MUTATE
        strategy = diagnosis.suggestedStrategy
        if diagnosis.progress == "SAME": escalate_strategy()
```
- **Specialized Agents**:
    - `AgentFactory`: Centralized registry and factory for all AI agents.
    - `ValidatorAgent`: Unified role merging `ReviewerAgent` and `ConstraintAgent`.
    - `AnalyticAgent`: Handles both pre-planning intent analysis and post-failure progress-aware diagnosis.
    - `RepairAgent`: Specialized in surgical fixes for build and technical failures.
    - `ProposalConsolidatorAgent`: Unifies and deduplicates agent proposals with deterministic risk-aware logic.
- **Tools Architecture**:
    - `ToolFactory`: Centralized registry and factory for all system tools (File, Maven, Git, Shell, etc.).
    - `ShellTool`: Robust execution engine with security whitelisting, timeouts, and resource limits.
- **Routing**: `LlmRouter` uses a **Hybrid Context Builder** approach (see `docs/HYBRID_MODE_DESIGN.md`) with **Automatic Local Fallback**. If remote providers fail, the system degrades gracefully to local Ollama.
- **Repair Loop**: Integration of `RepairAgent` into the PEV cycle for automated error recovery.
- **Self-Dev Supervisor**: `SelfDevSupervisor` coordinates the iterative cycles, branch management, and automatic rollbacks.
- **Traceability**: Strict enforcement of `@evo` markers in code and `plan.json` in `iterations/<id>/`.
- **UI**: Multi-page editor in Eclipse with specialized tabs for Chat, Approval, and Flow visualization.

## 4. Key Constraints
- **Fail-Fast**: Any validation error (missing markers, unplanned files) terminates the build immediately.
- **Atomic Changes**: Prefer many small, successful iterations over one large, complex change.
- **Resilience**: The system must prefer local execution (degraded mode) over total failure when remote LLMs are unavailable.
- **Traceability**: All self-developed code MUST be traceable back to an iteration and a reason.
