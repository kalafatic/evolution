# Functional and Behavioral Use Cases - AI Evolution Platform (090626)

## 1. Primary Use Cases

### 1.1. Simple Task Execution
**Name:** Simple Task Execution
**Trigger:** User enters a straightforward coding request (e.g., "Create a new Java class for a JSON parser").
**Preconditions:** System is in a stable state; a session is active.
**Flow steps:**
1. User provides prompt.
2. `ModeRouter` identifies the task as a `SIMPLE_CHAT` or basic file operation.
3. `PlannerAgent` creates a single-step execution plan.
4. `TaskExecutor` generates and applies the code change.
5. `ValidatorAgent` confirms the syntax is correct.
6. `FinalResponseAgent` notifies the user of completion.
**Output:** New file created or modified in the workspace.
**Failure modes:** Syntax errors in generated code; file system permission issues.

### 1.2. Mediated Darwinian Evolution
**Name:** Mediated Darwinian Evolution
**Trigger:** User enters a complex architectural or refactoring request.
**Preconditions:** Git repository initialized; at least one LLM provider configured.
**Flow steps:**
1. User provides a high-level intent.
2. `RealityDiscoveryAgent` scans the repository to provide context grounding.
3. `DarwinEngine` spawns 4-6 divergent architectural trajectories (variants).
4. System pauses and presents the variants to the user for selection.
5. User selects a variant (or provides guidance for regeneration).
6. Kernel executes the chosen trajectory.
7. System performs iterative verification (build/test).
8. Results are summarized in a final report.
**Output:** Complex code changes applied to the repository across multiple files/branches.
**Failure modes:** All generated variants fail diversity analysis; user rejects all proposals; merge conflicts during execution.

---

## 2. Secondary Use Cases

### 2.1. Autonomous Self-Development
**Name:** Autonomous Self-Development
**Trigger:** User requests the system to "Improve your own architecture" or specific self-upgrade.
**Preconditions:** `selfdev-genome` module is active; system has access to its own source code.
**Flow steps:**
1. System identifies hotspots in its own core modules.
2. `DarwinEngine` generates self-improvement proposals based on architectural invariants.
3. `Evaluator` runs internal quality tests and performance benchmarks.
4. Kernel selects the most stable improvement.
5. System applies changes to its own codebase.
**Output:** Self-optimized system components.
**Failure modes:** Regression in core kernel logic; bootstrap failure after self-modification.

### 2.2. Recovery from Failure
**Name:** Recovery from Failure
**Trigger:** An execution step fails (e.g., build error or test failure).
**Preconditions:** System is in the `VERIFYING` state.
**Flow steps:**
1. `AnalyticAgent` diagnoses the failure logs.
2. `RepairAgent` generates a corrective mutation.
3. Kernel applies the fix to the current branch.
4. System re-runs the verification loop.
5. If failure persists, system attempts a rollback or alternative trajectory.
**Output:** Repaired code state or clean rollback to stable state.
**Failure modes:** Infinite repair loop (prevented by safety counters); unrecoverable environment corruption.

---

## 3. Internal System Workflows

### 3.1. Intent Expansion & Grounding
**Name:** Intent Expansion & Grounding
**Trigger:** Arrival of a new user request in the Kernel.
**Flow steps:**
1. `IntentExpansionEngine` decomposes the prompt into semantic dimensions.
2. System checks for unresolved ambiguity (Dimension Discovery).
3. `RealityDiscoveryAgent` performs high-signal context curation from the repository.
4. Grounded context is injected into the prompt for subsequent agents.
**Result:** A structurally rich and repository-aware execution context.
