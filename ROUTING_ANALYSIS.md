# Analysis: Mode Detection and Routing System

## Overview
The EVO platform utilizes a multi-layered routing architecture designed to balance conversational agility with the depth of iterative architectural evolution. The system has converged toward a **Unified Evolutionary Kernel** where most goal-oriented tasks are processed through a single evolutionary pipeline (`DarwinFlow`), while simple conversational interactions are fast-tracked through a specialized bypass.

## 1. Routing Hierarchy
The `ModeRouter` serves as the entry point for determining the `PlatformMode`. It operates through three distinct layers of priority:

### Layer 1: Cognitive State Engine (Strategic Intent)
This is the primary routing mechanism. It analyzes the user prompt and conversation history to maintain a `SessionCognitiveState`.
- **Capability Mapping**: The engine maps the dominant `CapabilityType` (EVOLUTION, ARCHITECTURE, CODE, CHAT) to a corresponding `PlatformMode`.
- **Convergence**: Routing is not just based on the current message but on the "trajectory" of the conversation, allowing the system to "sink" into deeper evolutionary modes (e.g., from CODE to EVOLUTION) as complexity increases.

### Layer 2: Fast Rule Detection (Explicit Overrides)
A rule-based bypass for explicit user directives and common conversational patterns.
- **Explicit Overrides**: Users can force modes using keywords like `mode: mediated` or `mode: darwin`.
- **Greeting Detection**: Simple greetings (e.g., "hello", "hi") are caught here to prevent triggering expensive evolutionary analysis for non-task interactions.

### Layer 3: Model Fallback (Legacy/Persistence)
If no cognitive state or explicit rules apply, the system falls back to the configured state of the `Orchestrator` model (e.g., if `isDarwinMode` is set to true in the EMF model).

---

## 2. The Unified Evolutionary Kernel (`DarwinFlow`)
The architecture has undergone a "Strategic Unification." Instead of having separate flows for every mode, the platform now routes all "high-depth" tasks through `DarwinFlow`.

### Consolidated Modes
The following modes are now handled by the **Unified Darwin Loop**:
- `DARWIN_MODE`: Full autonomous evolution.
- `SELF_DEV_MODE`: Self-modification with expanded workspace access.
- `HYBRID_MANUAL_EXPORT` (Mediated Mode): Iterative refinement of an export package for external LLMs.
- `ASSISTED_CODING`: Single or multi-step implementation tasks.

### Why a Unified Flow?
By using `DarwinFlow` as the sole executor for these modes, the platform ensures that **every complex task benefits from iterative refinement, multi-variant trajectory competition, and parallel validation**, regardless of whether the goal is a simple bug fix or a major architectural overhaul.

---

## 3. `SIMPLE_CHAT` Bypass
To maintain responsiveness, `IterationManager` implements a cognitive fast-track:
- **Detection**: If the `COGNITIVE_SIMPLE_CHAT` trait is present or the `ModeRouter` returns `SIMPLE_CHAT`, the system bypasses the entire evolutionary kernel.
- **Execution**: The request is routed directly to the `GeneralAgent`. This bypasses:
    - Target Reality Discovery
    - Trajectory Generation/Mutation
    - Parallel Evaluation (Build/Test)
    - Variant Selection
- **Result**: Immediate response for low-complexity conversational goals.

---

## 4. Adaptive Specialization in `DarwinEngine`
While the flow is unified (`DarwinFlow`), the *behavior* within that flow is highly specialized via **Execution Policies** and **Instruction Modules** inside the `DarwinEngine`.

- **Policy Resolver**: Converts the platform state (BitState) into an `ExecutionPolicy`.
- **Instruction Modules**:
    - `MediatedInstructionModule`: Injects directives to evolve "Genome A" (Prompt) and "Genome B" (Files) instead of applying source patches.
    - `DarwinIterativeInstructionModule`: Activates multi-generation lineage tracking.
    - `SelfDevInstructionModule`: Relaxes security invariants to allow modification of core controller files.
- **DarwinFlow Adaptations**:
    - In **Mediated Mode**, `DarwinFlow` skips physical Git operations (worktrees/commits) and source modification, focusing instead on "Cognitive Materialization" and understanding refinement.

## Conclusion
EVO does **not** use a single architecture-iterative flow for *all* routes; it uses a **dual-pathway architecture**:
1. **The Fast-Track Path**: For `SIMPLE_CHAT`, prioritizing latency and conversational ease.
2. **The Unified Evolutionary Path**: For everything else, prioritizing fitness, architectural integrity, and iterative convergence through `DarwinFlow`.
