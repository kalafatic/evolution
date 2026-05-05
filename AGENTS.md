# Agent Instructions

## Architecture Enforcement
You are assisting in the development of a deterministic evolutionary kernel. Architecture is STRICT and non-negotiable.

For every change you make to this codebase, you MUST:
1.  **Read and follow [ARCHITECTURE_GUIDELINES.MD](ARCHITECTURE_GUIDELINES.MD)**.
2.  **Ensure all Primary Rules and Core Invariants are preserved.**
3.  **Perform the MANDATORY SELF-CHECK** before concluding any task.
4.  **Adhere to the REQUIRED OUTPUT FORMAT** for all root-cause analyses and fix descriptions.

Any change that introduces shortcuts, hardcoding, or bypasses the `IterationManager` state machine is considered WRONG and MUST be rejected.
