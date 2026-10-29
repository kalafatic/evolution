# EVO / EVOLUTION
# UNIVERSAL CORE PROCESS ANALYSIS AND EVOLUTIONARY REFACTORING MILESTONE

**Document type:** Universal analytical and implementation milestone
**Project:** EVO / Evolution
**Primary objective:** Understand, analyze, improve, refactor, and verify the project's key processes while preserving its architectural continuity and intended behavior.
**Execution mode:** Evidence-based analysis followed by controlled implementation, verification, and documentation.
**Primary consumers:** EVO development workflows, Genome, Milestone Generator, functionality inventory, and future AI development agents.
**Implementation principle:** Understand first. Establish the intended behavior. Identify the actual cause of the problem. Make the smallest justified architectural or implementation change. Verify the result.

---

# 1. MISSION

Perform a deep analysis and evolutionary refactoring of the key processes in the existing EVO / Evolution project.

The objective is not to rewrite the project.

The objective is to understand what EVO is intended to accomplish, determine how its key processes actually work, identify discrepancies between intended and implemented behavior, and improve those processes through controlled, evidence-based changes.

The work must address both local implementation defects and systemic problems that prevent the relevant functionality from working correctly.

A successful result must improve the actual system rather than merely improve the appearance of its source code.

You must establish:

1. The intended purpose of each selected process.
2. The actual implementation and execution path.
3. The architectural responsibilities involved.
4. The dependencies and contracts that connect the process to the rest of EVO.
5. The discrepancy between intended and actual behavior.
6. The root cause of any demonstrated defect or limitation.
7. The smallest appropriate scope of corrective change.
8. The expected behavior after the change.
9. The tests and evidence required to establish correctness.
10. The resulting architectural and operational state.

The outcome must be a working, verifiable improvement to the existing system, supported by updated documentation.

Do not stop after producing an analysis report when the relevant implementation can be safely corrected.

Do not modify code merely to demonstrate activity.

Every substantive change must address an identified requirement, defect, architectural inconsistency, or demonstrable maintainability problem.

---

# 2. PROJECT IDENTITY AND ARCHITECTURAL CONTINUITY

EVO is an evolving AI-oriented software platform.

Its architecture must be understood in the context of its existing implementation rather than replaced with a generic application architecture.

The known technology context includes:

- Java 21.
- Eclipse RCP.
- OSGi.
- EMF.
- SWT Browser.
- Maven and Tycho.
- Existing session and event infrastructure.
- Existing AI and Darwin-related execution mechanisms.
- Existing Genome and Milestone functionality.
- Existing configuration and resource-management mechanisms.

These are initial investigation hints, not assumptions that every component uses every technology.

Verify the actual implementation before making changes.

## 2.1 Preserve evolutionary continuity

Treat the existing source code as the current evolutionary state of EVO.

Preserve:

- Working behavior.
- Public APIs unless a justified and controlled change is necessary.
- Existing module boundaries where they remain appropriate.
- Existing EMF models and their authoritative role.
- Existing session isolation.
- Existing event contracts.
- Existing application lifecycle.
- Existing process orchestration mechanisms.
- Existing configuration sources of truth.
- Existing tests and build conventions.
- Compatibility with Java 21 and the actual Eclipse RCP / OSGi environment.

Do not replace existing components with newly invented parallel abstractions.

Do not introduce a second service registry, resource-discovery system, session manager, event bus, task scheduler, inference router, or configuration framework when the existing system already owns that responsibility.

If an existing component is defective, determine whether it should be corrected, simplified, or narrowly extended before considering a replacement.

Do not preserve an objectively defective implementation merely because it already exists. Preserve the system's intended contracts and working capabilities, not accidental bugs.

## 2.2 No speculative modernization

Do not:

- Introduce a different programming language.
- Introduce Spring or another application framework without an independently established requirement.
- Replace EMF configuration with a competing mechanism.
- Replace the existing RCP architecture with a different UI platform.
- Rewrite entire modules without evidence that a narrower change is insufficient.
- Add abstractions that duplicate existing responsibilities.
- Introduce dependencies solely to simplify a small local task.
- Redesign unrelated components.
- perform broad formatting or naming changes unrelated to the selected process.

The purpose is to improve EVO as EVO, not to turn it into a different application.

---

# 3. REQUIRED INPUTS AND AUTHORITATIVE SOURCES

Before analyzing or modifying code, inspect the current repository and discover the available project documentation.

If the previous Universal Final Project Analysis Milestone has been executed, use its results as an initial knowledge base.

Look for:

- Project knowledge index.
- Analysis manifest.
- Architecture and dependency maps.
- Execution-flow documentation.
- Functionality inventory.
- AI and Darwin documentation.
- Build and self-development documentation.
- Genome and Milestone integration documentation.
- Findings and technical debt register.
- Existing timestamped analysis packages.
- Existing milestone definitions.
- Existing tests and diagnostic records relevant to the selected processes.

Do not assume these artifacts exist at specific paths.

Discover their actual locations.

If a document is missing, continue by examining the source code rather than fabricating the missing knowledge.

## 3.1 Establish the current state

Record:

- Analysis start time.
- Repository root.
- Current Git branch.
- Current commit identifier, if available.
- Working-tree status.
- Existing uncommitted changes.
- Relevant documentation versions.
- Relevant tests and previous verification results.

Never revert, overwrite, or silently absorb existing user changes.

Distinguish changes introduced by this milestone from pre-existing modifications.

If the repository contains concurrent changes to files relevant to the selected process, inspect them carefully and preserve them.

If safe implementation is impossible because the current state is ambiguous or conflicting, stop the affected implementation step and explain the reason.

## 3.2 Revalidate previous analysis

Historical documentation is evidence about a previous analysis, not an unconditional source of current truth.

Verify every important claim relevant to the selected processes against the current implementation.

Identify:

- Outdated descriptions.
- Changed interfaces.
- New dependencies.
- Removed functionality.
- Previously reported defects that have already been corrected.
- New defects not covered by previous documentation.
- Differences between the documented architecture and actual execution.

Do not repeat obsolete findings as current defects.

---

# 4. PROCESS SELECTION AND PRIORITIZATION

The repository may contain many processes. You must analyze them systematically without attempting an uncontrolled rewrite of the entire codebase.

Identify the key processes using evidence from:

- Core architecture.
- Functionality inventory.
- Dependency relationships.
- Existing findings.
- User-visible capabilities.
- Process orchestration.
- Session and state ownership.
- Runtime failure reports.
- Tests.
- Repeated implementation patterns.
- Critical integration boundaries.

Prioritize processes whose correctness materially affects the reliability, usability, maintainability, or evolution of EVO.

Use criteria such as:

1. Functional importance.
2. Architectural centrality.
3. Severity of demonstrated defects.
4. Number of dependent components.
5. Frequency of execution.
6. Risk of cross-session or cross-process failures.
7. Importance to core AI execution.
8. Importance to Genome and Milestone workflows.
9. Feasibility of reliable verification.
10. Expected benefit relative to implementation risk.

Do not select processes solely because their source files are large or easy to modify.

## 4.1 Define process boundaries

A process is a coherent execution or decision-making flow, not necessarily a single class.

Examples may include, where implemented:

- Application startup and initialization.
- Session creation and lifecycle.
- Event dispatch and handling.
- Task creation and execution.
- State transitions.
- Resource and configuration discovery.
- AI intent analysis and inference.
- Darwin execution modes.
- Model and tool routing.
- Forge training orchestration.
- Self-development workflows.
- Build and runtime supervision.
- Genome analysis.
- Milestone generation.
- Functionality inventory generation.
- UI-to-backend execution paths.
- Persistence and restoration.
- External process or HTTP integration.

These are examples, not instructions to refactor every listed area.

Determine which processes actually exist and which have sufficient evidence to justify implementation work.

## 4.2 Establish a bounded scope

Before modifying code, produce an explicit scope containing:

- Selected process identifiers.
- Their intended outcomes.
- The principal observed problems.
- The dependencies that must be preserved.
- The files likely to require changes.
- The tests needed.
- The expected results.
- Any risks or unresolved questions.

Prefer a coherent set of related processes over a collection of unrelated changes.

If the project contains more high-priority work than can be safely completed in one milestone, complete a meaningful, independently verifiable subset and document the remaining work.

Do not declare the entire project refactored when only selected processes have been addressed.

---

# 5. UNDERSTAND THE INTENDED PURPOSE BEFORE CHANGING CODE

This is a mandatory phase.

For each selected process, reconstruct the intended behavior before proposing a solution.

Use all relevant evidence:

- Existing architecture.
- Public interfaces.
- Callers and consumers.
- Tests.
- UI actions.
- Configuration.
- EMF models.
- Event contracts.
- Runtime logs.
- Previous implementation decisions.
- Existing project documentation.
- Related functionality and milestone definitions.

Determine the problem the process is intended to solve.

Do not infer the complete design intent from class names alone.

## 5.1 Create a process specification

For each selected process, document:

### A. Purpose

What capability does the process provide?

Why does the capability exist in the context of EVO?

Which higher-level functionality depends on it?

### B. Trigger

What initiates execution?

Examples include:

- User action.
- Event.
- Scheduled operation.
- Task.
- Session transition.
- External request.
- Application lifecycle callback.

### C. Inputs

Identify:

- Required parameters.
- Session context.
- Configuration.
- Model state.
- External data.
- Dependencies.
- Preconditions.

### D. Outputs

Identify:

- Return values.
- State changes.
- Events.
- Persisted artifacts.
- External requests.
- Process results.
- UI updates.
- Error information.

### E. Responsibilities

Determine which component owns each decision and operation.

Distinguish the component that decides what should happen from the component that performs the operation.

### F. Invariants

Identify conditions that must remain true throughout execution.

Examples may include:

- A session must not consume another session's mutable state.
- A process must not report success before the required operation has succeeded.
- A state transition must have a single authoritative owner where the architecture requires one.
- A configuration value must come from its authoritative source.
- A failed external operation must not be silently represented as success.
- Resources must be released according to their ownership and lifecycle.

Use only invariants appropriate to the actual process.

### G. Failure behavior

Determine what should happen when:

- Input is invalid.
- Configuration is unavailable.
- A dependency fails.
- A timeout occurs.
- An external process exits unexpectedly.
- A task is cancelled.
- State is inconsistent.
- A retry is unsuccessful.
- Persistence fails.

### H. Acceptance criteria

Define observable conditions that distinguish correct behavior from incorrect behavior.

Acceptance criteria must be specific enough to support meaningful tests.

Do not proceed directly from a vague description of the problem to an implementation.

---

# 6. RECONSTRUCT THE ACTUAL EXECUTION PATH

Trace the process from its initiating entry point to its final outcome.

Follow actual method calls, events, service lookups, state transitions, persistence operations, and external interactions.

Identify:

- Entry points.
- Intermediate components.
- Decision points.
- State ownership.
- External boundaries.
- Error-handling paths.
- Completion conditions.
- Cancellation paths.
- Cleanup.
- Result propagation.

Document the actual sequence.

Where relevant, use a sequence diagram or a numbered execution trace.

For every important transition, identify the responsible class or method.

Do not describe an intended path as an actual path unless the implementation supports the claim.

## 6.1 Analyze all significant branches

Do not inspect only the successful execution path.

Examine:

- Success.
- Failure.
- Cancellation.
- Timeout.
- Missing dependencies.
- Invalid state.
- Partial completion.
- Retry.
- Duplicate invocation.
- Concurrent invocation.
- Recovery or restart, where supported.

Prioritize branches that can produce inconsistent state, false success, resource leaks, or cross-session interference.

## 6.2 Find disconnected or contradictory paths

Investigate whether:

- A UI action invokes the intended backend operation.
- The backend uses the correct session context.
- Events reach the intended consumer.
- A task's result reaches its caller.
- Exceptions are preserved rather than silently discarded.
- A successful return value actually means the operation completed.
- Configuration is read consistently.
- Retry behavior is bounded.
- Cancellation reaches the underlying operation.
- Multiple components independently perform the same responsibility.
- A supposedly authoritative component is bypassed by another path.

Do not presume these problems exist. Confirm or reject each relevant concern through evidence.

---

# 7. ROOT-CAUSE ANALYSIS

Before modifying a process, identify the actual cause of its demonstrated problem.

Distinguish among:

1. Incorrect local implementation.
2. Incorrect or incomplete contract.
3. Incorrect responsibility allocation.
4. Missing dependency or integration.
5. Incorrect configuration.
6. Invalid state transition.
7. Concurrency or lifecycle problem.
8. Incorrect error propagation.
9. Missing validation.
10. Environmental limitation.
11. Inadequate verification.
12. Outdated or misleading documentation.

A visible failure may be caused by another component upstream or downstream.

Trace the failure far enough to distinguish the initiating cause from its symptoms.

For example, if a task fails because an external process never creates the expected session, do not automatically rewrite the task's retry loop.

Determine whether the failure originates in process launch, application initialization, project creation, session creation, configuration, communication, or result detection.

Likewise, do not add retry logic to compensate for a deterministic programming error.

## 7.1 Evidence requirements

For each confirmed issue, record:

- Finding identifier.
- Affected process.
- Reproduction conditions, if known.
- Expected behavior.
- Actual behavior.
- Evidence.
- Root cause.
- Affected contracts.
- Proposed corrective approach.
- Verification strategy.

If the cause remains uncertain, perform additional non-destructive investigation.

If uncertainty cannot be resolved safely, document it and avoid speculative changes.

---

# 8. EVALUATE THE APPROPRIATE REFACTORING STRATEGY

Select the smallest change that correctly addresses the root cause.

Possible strategies include:

- Correcting a local defect.
- Removing duplicate logic.
- Centralizing an existing responsibility that is demonstrably fragmented.
- Correcting an interface contract.
- Fixing state ownership.
- Repairing a missing integration.
- Simplifying control flow.
- Improving error propagation.
- Correcting resource lifecycle management.
- Adding missing validation.
- Making asynchronous behavior explicit.
- Improving observability.
- Adding targeted tests.
- Updating documentation.

These are options, not mandatory changes.

## 8.1 Local versus structural correction

Prefer a local correction when it fully restores the intended behavior and preserves the architecture.

Prefer a structural correction when the root cause demonstrates that local patches would preserve or worsen the underlying defect.

For a structural correction, explain:

1. Why the existing structure is insufficient.
2. Which responsibilities are incorrectly allocated.
3. Which existing contracts must remain stable.
4. Why a smaller correction would not be sufficient.
5. How the proposed change preserves architectural continuity.
6. How regressions will be detected.

Do not expand the scope simply because a broader refactor appears cleaner.

## 8.2 Refactoring constraints

Avoid unrelated changes.

Do not combine functional correction with broad formatting, naming, dependency upgrades, or UI redesign.

Do not change public behavior without a documented requirement.

Do not silently change the meaning of existing state values, event types, task statuses, or configuration entries.

If a public or persisted contract must change, analyze its consumers and compatibility implications before proceeding.

---

# 9. ARCHITECTURAL RESPONSIBILITY AND SINGLE AUTHORITY

Analyze where decisions belong in the existing architecture.

Where the project requires a single authority for a responsibility, preserve or restore that authority.

Examples may include:

- Session lifecycle.
- State transitions.
- Process execution.
- Configuration ownership.
- Resource discovery.
- Task lifecycle.
- Inference routing.
- Event coordination.

Verify which component actually owns each responsibility before modifying it.

Do not introduce a new manager or controller merely because its name would sound architecturally cleaner.

If responsibility is currently fragmented, identify the authoritative component or contract that should govern the behavior.

Then determine whether the problem can be resolved by reconnecting existing components, correcting their contracts, or eliminating duplicated decisions.

Do not create parallel execution paths.

Do not permit multiple components to independently mutate state that should have a single owner unless the existing design explicitly supports that behavior.

Where centralization is justified, make the smallest change that restores consistent responsibility and preserve compatibility with existing consumers.

---

# 10. SESSION, CONCURRENCY AND STATE CORRECTNESS

For every selected process involving sessions, shared state, background execution, or concurrent operations, inspect:

- Session identity.
- State ownership.
- Mutable shared objects.
- Thread safety.
- Synchronization.
- Event ordering.
- Reentrancy.
- Cancellation.
- Timeouts.
- Resource cleanup.
- Session termination.
- Recovery.
- Duplicate execution.
- Cross-session isolation.

Do not assume that a singleton is automatically wrong or that creating another object automatically fixes isolation.

Establish the actual lifecycle and scope of each relevant object.

Verify that concurrent execution cannot accidentally:

- Reuse another session's state.
- Deliver results to the wrong consumer.
- Apply stale events.
- Complete a task more than once.
- Leak resources.
- Leave a process in a contradictory state.
- Report success after cancellation or failure.

Introduce synchronization, lifecycle changes, or isolation mechanisms only when justified by the existing design and evidence.

Do not add concurrency merely to increase performance.

---

# 11. CONFIGURATION, EMF AND RESOURCE OWNERSHIP

Inspect the configuration and resource-discovery paths used by the selected processes.

Determine:

- Which model or configuration source is authoritative.
- How values are loaded.
- How values are validated.
- How references are resolved.
- Which components consume them.
- What happens when configuration is missing or malformed.
- Whether consumers independently reconstruct paths or defaults.
- Whether duplicate configuration sources can diverge.

Preserve existing EMF models and their intended role.

Do not introduce competing configuration stores or independent discovery mechanisms.

If a resource manager or equivalent facade already owns resource resolution, inspect its actual behavior and consumers before changing anything.

A refactoring must not replace deterministic configuration with heuristic path guessing.

If configuration ownership is inconsistent, identify the minimal correction that restores a clear source of truth.

---

# 12. ERROR HANDLING, RETRIES AND FAILURE SEMANTICS

Analyze the complete error-handling behavior of each selected process.

Determine whether:

- Exceptions are swallowed.
- Error details are lost.
- Errors are reported at the correct architectural boundary.
- A caller can distinguish failure from success.
- Partial completion is represented accurately.
- Retry policies are bounded.
- Retries occur only for appropriate failure categories.
- Timeouts actually terminate or interrupt the relevant operation.
- Cancellation is propagated.
- Cleanup executes after failure.
- State is left consistent.
- Diagnostic output identifies the failing component and operation.

Do not replace all errors with generic exceptions.

Do not use success-shaped fallback values to conceal failures.

Do not introduce unbounded retry loops.

Do not assume that retrying an operation is safe if it may create duplicate projects, sessions, tasks, processes, or persisted artifacts.

Before adding retries, determine whether the operation is idempotent or how duplicate execution is prevented.

For external integrations, distinguish transport success from business-operation success.

For example, receiving a successful HTTP response does not necessarily prove that the requested project, session, or task was created correctly.

Define completion according to the actual operation's required postconditions.

---

# 13. OBSERVABILITY AND DIAGNOSTICS

Improve observability where the absence of useful evidence materially prevents debugging or verification.

Prefer the project's existing logging and diagnostic infrastructure.

For important process boundaries, make it possible to establish:

- Which operation started.
- Which session or task owns it, where applicable.
- Which component performed it.
- Which relevant non-sensitive parameters were used.
- Which state transition occurred.
- Which external operation was attempted.
- Whether it succeeded, failed, timed out, or was cancelled.
- Which result was produced.
- Where the failure originated.

Preserve relevant correlation identifiers where the existing architecture supports them.

Do not introduce a competing logging subsystem.

Do not log passwords, tokens, private keys, or sensitive user content unnecessarily.

Avoid excessive diagnostic output that obscures the actual failure.

Instrumentation must help explain behavior, not merely increase the volume of logs.

---

# 14. TESTING AND VERIFICATION STRATEGY

Every substantive implementation change must have an appropriate verification strategy.

Before modifying code, identify the existing tests relevant to the selected process.

Determine which parts can be verified with unit tests, integration tests, existing build checks, or safe runtime tests.

## 14.1 Test the intended behavior

Add or update tests when necessary to establish:

- Correct successful execution.
- Correct failure behavior.
- Correct state transitions.
- Correct result propagation.
- Correct resource cleanup.
- Correct handling of missing dependencies.
- Correct session isolation.
- Correct cancellation and timeout behavior.
- Correct integration contracts.

Do not write tests that merely reproduce the implementation's assumptions.

Tests must verify observable requirements and important invariants.

## 14.2 Preserve existing tests

Do not delete or weaken tests merely to make a build pass.

Do not replace meaningful assertions with superficial checks.

If an existing test conflicts with verified intended behavior, explain the conflict and correct the test only when the intended behavior is sufficiently established.

## 14.3 Verification levels

Report verification separately:

**LEVEL 1 — Static inspection**

The code and contracts were inspected.

**LEVEL 2 — Targeted tests**

Relevant tests were executed successfully.

**LEVEL 3 — Module or project build**

The relevant build completed successfully.

**LEVEL 4 — Integration verification**

The selected components were verified together.

**LEVEL 5 — End-to-end runtime verification**

The actual user-visible or system-level process completed and its required postconditions were observed.

Use only the levels actually achieved.

A passing build does not establish end-to-end correctness.

A test that mocks an external service does not establish that the real external integration works.

If environmental constraints prevent higher-level verification, document the limitation and the remaining uncertainty.

---

# 15. IMPLEMENTATION EXECUTION

After completing the process specification, root-cause analysis, and change plan, implement the justified corrections.

Work incrementally.

For each selected process:

1. Record its current behavior and relevant evidence.
2. Establish the intended behavior.
3. Identify the root cause.
4. Define the smallest adequate change.
5. Identify affected files and contracts.
6. Implement the change.
7. Review the resulting diff.
8. Execute targeted verification.
9. Run broader verification where safe and feasible.
10. Update the relevant documentation.
11. Record the outcome and remaining limitations.

Do not start by modifying every file discovered during the initial scan.

Keep each coherent correction understandable and independently reviewable.

Avoid unrelated changes within the same file.

If an unexpected dependency or architectural problem emerges, pause and reassess the scope before expanding the implementation.

If a change proves more invasive than anticipated, reconsider whether a narrower correction would satisfy the requirement.

Do not hide incomplete work behind a successful summary.

---

# 16. SAFETY AND ENVIRONMENTAL BOUNDARIES

Perform implementation and testing within the existing development environment and its established workflows.

Do not:

- Revert pre-existing user changes.
- Delete unrelated files.
- Reset or rewrite Git history.
- Commit or push changes unless explicitly authorized in a separate instruction.
- Modify production systems.
- Deploy to external environments.
- Change system-wide security settings.
- Disable antivirus or operating-system protections.
- Expose credentials.
- Perform destructive database operations.
- Launch uncontrolled background processes.
- Download large datasets without a clear requirement.
- Change installed toolchains or system dependencies without explicit authorization.
- Alter unrelated application configuration.

When testing process launching, external integrations, or self-development workflows, identify their side effects before execution.

Use isolated test resources where appropriate and supported by the existing architecture.

Do not assume that a task is safe simply because it is named a test.

If an integration requires external permissions, user interaction, unavailable services, or a risky operation, document the limitation rather than bypassing the protection.

---

# 17. GENOME AND MILESTONE CONTINUITY

The refactoring must remain connected to the existing knowledge and development workflow.

Inspect how the current Genome and Milestone mechanisms represent functionality, dependencies, findings, tasks, and results.

Do not create a competing development-planning framework.

For each completed correction, preserve the relationship between:

**Original finding → affected functionality → root cause → implementation change → verification → resulting state.**

Use existing identifiers and schemas wherever possible.

If no suitable identifier exists, introduce a stable identifier in the new documentation without changing application contracts unnecessarily.

Where the existing functionality inventory supports it, update the relevant entry using its established mechanism.

Where automatic inventory updates are not supported, document the necessary update and its location.

Do not claim that Genome has been updated automatically unless the actual operation succeeded.

Do not modify Genome or Milestone implementation code merely to make the current milestone easier unless such a change is independently justified, within scope, and necessary to achieve a defined acceptance criterion.

---

# 18. REQUIRED DOCUMENTATION OUTPUTS

Create a timestamped documentation package using the repository's existing naming conventions.

The intended date-based directory is:

`genome-docs/29102026/`

Discover the actual documentation conventions before selecting the final location.

If the directory already exists, preserve its contents and add uniquely named artifacts.

Use the actual execution timestamp, not an assumed timestamp from a previous analysis.

## 18.1 Process analysis

Suggested filename:

`11_CORE_PROCESS_ANALYSIS_<TIMESTAMP>.md`

Include:

- Selected processes.
- Intended purpose.
- Execution paths.
- Dependencies.
- Invariants.
- Failure behavior.
- Existing defects.
- Root-cause analysis.
- Unresolved questions.
- Evidence references.

## 18.2 Refactoring plan

Suggested filename:

`12_REFACTORING_PLAN_<TIMESTAMP>.md`

Include:

- Prioritized findings.
- Proposed changes.
- Affected files and contracts.
- Justification for the selected scope.
- Rejected alternatives where materially relevant.
- Regression risks.
- Verification strategy.
- Acceptance criteria.

Record which proposed changes were actually implemented.

## 18.3 Implementation report

Suggested filename:

`13_REFACTORING_IMPLEMENTATION_REPORT_<TIMESTAMP>.md`

Include:

- Changes performed.
- Root causes addressed.
- Files changed.
- Contracts preserved or modified.
- Reasons for any necessary structural changes.
- Behavior before and after.
- Verification performed.
- Test results.
- Remaining risks.
- Incomplete work.

## 18.4 Verification report

Suggested filename:

`14_REFACTORING_VERIFICATION_<TIMESTAMP>.md`

Include:

- Tests executed.
- Build commands executed.
- Relevant execution environment.
- Actual results.
- Failed checks.
- Skipped checks and reasons.
- Integration limitations.
- End-to-end verification status.
- Outstanding acceptance criteria.

Do not record a check as successful unless the result was observed.

## 18.5 Knowledge-base update

Update the existing project knowledge index or create a new, linked analytical index when required by repository conventions.

Record:

- The analysis package.
- Findings addressed.
- Functionality affected.
- Source files changed.
- Verification status.
- Remaining findings.
- Links to the previous analysis baseline.

Do not overwrite historical analysis results.

Preserve the distinction between the original baseline and the post-refactoring state.

## 18.6 Timestamped universal prompt

Preserve the complete prompt used for this milestone.

Preferred filename:

`genome-docs/29102026/UNIVERSAL_CORE_PROCESS_REFACTORING_PROMPT_<TIMESTAMP>.md`

If an equivalent canonical prompt location already exists, follow the existing convention.

Do not overwrite an earlier timestamped prompt.

The saved file must contain the complete reusable prompt, not merely its title or a summary.

The timestamp must correspond to the actual prompt artifact creation or explicitly documented prompt version.

---

# 19. FINAL DIFF REVIEW

Before declaring completion, inspect the complete set of changes.

For every modified source file, establish:

- Why the file changed.
- Which finding or acceptance criterion justifies the change.
- Whether the change addresses the root cause.
- Whether existing behavior is preserved where required.
- Whether public contracts remain compatible.
- Whether the implementation introduces duplicated responsibility.
- Whether error handling remains correct.
- Whether resource cleanup is preserved.
- Whether concurrency and session isolation remain correct.
- Whether tests adequately cover the change.
- Whether documentation accurately describes the resulting implementation.

Inspect the full diff rather than relying only on a list of modified filenames.

Check for:

- Accidental formatting changes.
- Unrelated refactoring.
- Temporary debugging code.
- Hard-coded environment-specific paths.
- Sensitive information.
- Unnecessary dependencies.
- Dead code introduced by the change.
- Missing tests.
- Incorrect comments.
- Stale documentation.
- Untracked files unrelated to the milestone.

Do not remove unrelated pre-existing untracked files.

Correct problems introduced by this milestone before finalizing, provided the correction is safe and within scope.

---

# 20. DEFINITION OF DONE

A selected process is complete only when:

1. Its intended purpose has been established sufficiently.
2. Its actual execution path has been reconstructed.
3. Its relevant dependencies and contracts have been identified.
4. Demonstrated problems have been investigated to their root causes.
5. The implementation change is justified by evidence.
6. The change is limited to the necessary scope.
7. Existing architectural responsibilities remain coherent.
8. Relevant tests have been added or updated where necessary.
9. Verification has been performed to the achievable level.
10. The complete diff has been reviewed.
11. Documentation reflects the resulting implementation.
12. Remaining uncertainty and limitations are explicitly recorded.

The overall milestone is complete only when every selected process has a documented outcome.

If some processes remain incomplete, report the milestone as partially complete.

Do not claim full success merely because code changes were produced or the build passed.

---

# 21. FINAL RESPONSE

Provide a factual completion report containing:

## A. Execution summary

- Analysis identifier.
- Start and completion timestamps.
- Repository and analyzed commit.
- Selected processes.
- Overall completion status.

## B. Findings

- Most important root causes.
- Architectural issues discovered.
- Issues confirmed or ruled out.
- Remaining uncertainties.

## C. Implementation

- Files modified.
- Important behavior changes.
- Contracts preserved.
- Any necessary structural changes.
- Changes deliberately not made.

## D. Verification

- Tests executed and their actual outcomes.
- Build results.
- Integration verification.
- End-to-end verification.
- Failed or skipped checks.
- Environmental limitations.

## E. Documentation

- Exact path to the timestamped prompt.
- Exact path to the process analysis.
- Exact path to the implementation report.
- Exact path to the verification report.
- Exact path to the updated knowledge index.

## F. Remaining work

- Unresolved findings.
- Incomplete processes.
- Additional evidence needed.
- Recommended next milestone.

Do not claim that a file was created unless it exists.

Do not claim that a defect was fixed unless the relevant implementation was changed and the result was appropriately verified.

Do not claim that the application works end to end when only source inspection or a build was performed.

---

# 22. GOVERNING PRINCIPLE

Understand the purpose before changing the implementation.

Understand the complete execution path before correcting a local symptom.

Identify the root cause before choosing a refactoring strategy.

Preserve working behavior and architectural continuity.

Centralize responsibilities only where the actual architecture requires it.

Make the smallest change that correctly solves the demonstrated problem.

Verify the result using observable evidence.

Record the outcome so that future Genome and Milestone processes can build on the improved state.

**EVO must evolve through precise, evidence-based improvements to its existing architecture—not through speculative rewrites, duplicated abstractions, or disconnected patches.**
