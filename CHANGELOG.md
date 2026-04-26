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
