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
