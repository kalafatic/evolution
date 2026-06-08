# Changelog: Evolution Self-Development

## [Iteration 15] - Setup of Iterative Self-Development Loop
- **Date**: Current
- **Goal**: Implement the architect/self-development loop where JULES treats each prompt/commit as a documented iteration.
- **Changes**:
    - Created `architecture.md` as a permanent reference for system goals and SOP.
    - Created `CHANGELOG.md` to track iterative improvements.
    - Established the pattern of reading architecture and updating logs in every iteration.
- **Traceability**: `iterations/15/plan.json`

## [Iteration 16] - Darwin Mode UI and Multi-Branch Iterations
- **Goal**: Add Darwin style iterations (multi-branch) to AI chat with vertical split UI.
- **Changes**:
    - Added "Darwin" checkbox to `InstructionsGroup` in AI Chat.
    - Updated `AiChatPage` to sync Darwin mode state with Orchestrator model.
    - Refactored `chat.html` and `ChatGroup.java` to support vertical side-by-side rendering of branching variants.
    - Modified `IterationManager` to log Darwinian proposals in JSON format for the new UI.
- **Traceability**: `iterations/16/plan.json`

## [Iteration 17] - New AI Thread Shortcut and Wizard Integration
- **Goal**: Add global shortcut and wizard for creating new AI threads.
- **Changes**:
    - Refactored `AiChatPage` to extract reusable `createNewThread(String)` method.
    - Added `getAiChatPage()` to `MultiPageEditor` for programmatic access.
    - Implemented `NewAiThreadWizard` and `NewAiThreadHandler`.
    - Registered `Ctrl+Alt+T` shortcut for the `newAiThread` command in `plugin.xml`.
    - Integrated "New Thread" wizard into the "AI Evolution" category and Evo Perspective shortcuts.
- **Traceability**: `iterations/17/plan.json`

## [Iteration 18] - Plan–Execute–Verify (PEV) and Hybrid Context Builder
- **Goal**: Improve task robustness and hybrid LLM efficiency.
- **Changes**:
    - **EMF Model**: Added `goal`, `plan`, and `artifacts` attributes to `Task`.
    - **Task Lifecycle**: Introduced explicit `PLANNING`, `EXECUTING`, and `VERIFYING` states.
    - **Orchestration**: Implemented structured PEV loop in `EvolutionOrchestrator` with Darwinian mutation on failure.
    - **Routing**: Refactored `LlmRouter` to use local models as 'Context Builders' and cloud LLMs as 'Reasoners' in HYBRID mode.
    - **Documentation**: Updated `architecture.md` and `PROJECT_SPECIFICATION.md` to reflect the new technical design.
- **Traceability**: `iterations/18/plan.json`

## [Iteration 19] - Resilient Routing and Repair Agent
- **Goal**: Improve platform robustness through offline fallback and specialized error recovery.
- **Changes**:
    - **Routing**: Implemented automatic fallback to `LOCAL` mode in `LlmRouter` when `REMOTE` or `HYBRID` requests fail.
    - **Agents**: Created `RepairAgent` specialized in analyzing build logs and providing surgical fixes.
    - **Orchestration**: Integrated `RepairAgent` into the `EvolutionOrchestrator` PEV loop to handle technical failures (Maven, Shell, File).
    - **Documentation**: Created `docs/IMPROVEMENTS_ANALYSIS.md` with detailed architectural analysis and roadmap.
- **Traceability**: `iterations/19/plan.json`

## [Iteration 20] - Refinement of Architecture Documentation and Code Traceability
- **Goal**: Synchronize documentation with the latest technical implementation and improve code traceability.
- **Changes**:
    - **Documentation**: Updated `architecture.md` and `PROJECT_SPECIFICATION.md` to define the 6-phase PEV pipeline (PLAN, CONTEXT, EXECUTE, VERIFY, ANALYZE, MUTATE).
    - **Specialized Agents**: Documented roles of `RepairAgent`, `ConstraintAgent`, and `ProposalConsolidatorAgent`.
    - **Improvements Analysis**: Refactored `IMPROVEMENTS_ANALYSIS.md` to reflect implemented features like Resilient Routing and Parallel Darwinian Variants.
    - **Traceability**: Enhanced code traceability with `@evo:20:A` markers across core orchestration and agent classes.
    - **Hybrid Mode Design**: Created `docs/HYBRID_MODE_DESIGN.md` to document the split-intelligence architecture.
- **Traceability**: `iterations/20/plan.json`

## [Iteration 21] - Architectural Refinement and Boundary Enforcement
- **Goal**: Reduce complexity, eliminate loops, and strengthen component boundaries.
- **Changes**:
    - **Infrastructure**: Introduced `ArchitectureContext` and `ChangeUnit` as explicit units of context and change.
    - **Agents**: Created unified `ValidatorAgent` and enhanced `AnalyticAgent` with progress-aware diagnosis (IMPROVED/SAME/WORSE).
    - **PEV Loop**: Refactored EXECUTE phase into distinct `GeneratePatch` and `ApplyPatch` conceptual steps.
    - **Boundary Enforcement**: Clarified Orchestrator vs Supervisor roles; Orchestrator only signals long-running operations.
    - **Progress Tracking**: Implemented progress-based anti-loop mechanism in the PEV cycle.
    - **Documentation**: Updated `architecture.md` with refined blueprint and pseudo-code.
- **Traceability**: `iterations/21/plan.json`

## [Iteration 22] - Self-Development Bootstrap Flow and Supervisor Unification
- **Goal**: Implement minimal bootstrap flow to trigger external Supervisor for build/restart and unify supervisor logic.
- **Changes**:
    - **Bootstrap**: Created `bootstrap.json` contract and `SelfDevBootstrapController` to trigger external Supervisor process.
    - **Unification**: Renamed external `Supervisor` to `SelfDevSupervisor` and aligned logic with internal supervisor.
    - **Handoff**: Implemented handoff logic in Controller's `SelfDevSupervisor` to delegate build/restart phases to external process.
    - **UI**: Updated `IterationPage` with dynamic polling, session status/progress labels, and integrated "Self-Dev Loop" controls.
    - **Navigation**: Added `showArchitecturePage()` and `showIterationPage()` to `MultiPageEditor`.
- **Traceability**: `iterations/22/plan.json`

## [Iteration 23] - Darwin Flow Optimization and Manual Selection Resilience
- **Goal**: Fix Darwin flow issues related to branching width and manual selection resolution.
- **Changes**:
    - **Branching Width**: Increased minimum `branchingLimit` to 4 and updated `TrajectoryTerritoryMapper` to request 4-6 unique blueprints.
    - **Manual Selection**: Implemented `resolveFuzzyId` in `DecisionResolver` to map user-friendly inputs (indices, types) to internal IDs.
    - **Override Authority**: Updated `createManualDecision` to prioritize the manually selected variant in the ranking, ensuring human override.
- **Architectural Impact**:
    - Aligns with "Survival of the Fittest" by ensuring a diverse candidate pool.
    - Strengthens the Authority boundary by improving human-in-the-loop decision mapping.
- **Risks**: Fuzzy matching may occasionally conflict if proposal indices are ambiguous, but prioritized exact ID and strategy type matches mitigate this.
- **Next Step**: Implement a "Cognitive Pressure Dashboard" to visualize divergence axes and evolutionary tension in real-time.
- **Traceability**: `iterations/23/plan.json`
