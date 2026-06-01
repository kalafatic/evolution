# AI ORCHESTRATION PLATFORM — CORE PRIMITIVES ANALYSIS REPORT

## 1. PRIMITIVE INVENTORY

The following entities represent the discovered fundamental building blocks of the cognitive runtime:

| Primitive | Semantic Meaning | Lifecycle Role | Ownership |
| :--- | :--- | :--- | :--- |
| **Intent** | The high-fidelity engineering goal reconstructed from raw input and repository context. | **Discovery** | `IntentExpansionEngine` |
| **BitState** | A 64-bit policy field defining Mode, Supervision, Interaction, Reasoning, and Workflow. | **Policy** | `OrchestrationState` |
| **Context** | A filtered, compressed manifest of repository state, history, and active signals. | **Grounding** | `ContextBuilder` |
| **Task** | An atomic, executable unit of work targeting a specific tool or agent. | **Execution** | `IterationManager` |
| **Flow** | A state-machine implementing a specific orchestration strategy (Atomic, Iterative, Darwin). | **Kernel** | `IOrchestrationFlow` |
| **Iteration** | A single Plan-Execute-Verify (PEV) lifecycle pass. | **Loop** | `IterationManager` |
| **Signal** | A continuous metric (fitness, novelty) derived from observational events. | **Feedback** | `EvolutionRegistry` |
| **Trajectory** | A long-running adaptive path tracking solution evolution over multiple iterations. | **Feedback** | `EvolutionRegistry` |
| **Variant** | A proposed mutation of the system state (specific to Darwinian reasoning). | **Refinement** | `DarwinEngine` |

---

## 2. DUPLICATION ANALYSIS

| Duplicated Concept | Conflicting Abstractions | Impact |
| :--- | :--- | :--- |
| **Behavioral Policy** | `PlatformMode` (Enum) vs. `AiMode` (EMF) vs. `BehaviorProfile` (Traits) vs. `BitState` (Bitfield). | **Orchestration Entropy**: Conflicting rules on how the system should reason or supervise. |
| **Darwin Logic** | `IterationManager.runDarwin()` vs. `DarwinFlow.runDarwin()`. | **Code Bloat**: Parallel maintenance of branch and variant management logic. |
| **Iteration Ownership** | `SelfDevSupervisor` managing loops vs. `IterationManager` providing `runIteration`. | **Ambiguous Authority**: No single source of truth for the state of the current loop. |
| **Task Execution** | `TaskExecutor` as a middleman vs. `EvolutionOrchestrator` as the actual executor. | **Circular Coupling**: Executor routing back into the kernel creating recursion risks. |
| **Context Scoring** | `ContextBuilder` heuristics vs. `AttachmentInjector` hybrid scoring. | **Inconsistent Grounding**: Different agents seeing different 'relevant' files for the same task. |

---

## 3. EXECUTION LIFECYCLE MODEL (CANONICAL)

The system follows a mandatory 8-stage lifecycle:

1.  **DISCOVERY**: Initial repository inspection and baseline snapshot.
2.  **INTENT RECONSTRUCTION**: Mapping raw prompt to architectural objectives (not just chat).
3.  **POLICY RESOLUTION**: Deriving the `BitState` (The "How").
4.  **PLANNING / MUTATION**: Generation of `Tasks` (Iterative) or `Variants` (Darwin).
5.  **EXECUTION**: Tool application via the blind Orchestrator.
6.  **VALIDATION**: Scoring, testing, and fitness evaluation.
7.  **REFINEMENT**: Evolutionary selection or iterative loop-back.
8.  **SYNTHESIS**: Final response or task-package generation.

---

## 4. ARCHITECTURE LAYER MODEL

| Layer | Responsibility | Primitives |
| :--- | :--- | :--- |
| **Layer 1: Kernel Core** | Pure state management and event propagation. | `BitState`, `SystemState`, `EventBus` |
| **Layer 2: Cognitive Plane** | Translating Intent into Plans using LLM intelligence. | `Intent`, `Variant`, `TaskPlanner` |
| **Layer 3: Orchestration Flow** | Driving the lifecycle state-machine. | `Flow`, `Iteration`, `IterationManager` |
| **Layer 4: Execution Engine** | Blind execution of atomic tool commands. | `Task`, `Orchestrator`, `Agents` |
| **Layer 5: Supervision** | Governance, human-in-the-loop, and external handoffs. | `Supervisor`, `Mediation` |

---

## 5. CANONICAL PRIMITIVE SET (MINIMAL)

The platform is collapsed onto 5 stable pillars:

1.  **BitState**: The single authority for policy-driven behavior.
2.  **Task**: The unit of execution.
3.  **Iteration**: The unit of progress (PEV loop).
4.  **Signal**: The observational feedback loop.
5.  **Context**: The repository grounding authority.

---

## 6. ENTROPY SOURCES

1.  **Recursive Orchestration**: High-level executors calling low-level managers which call high-level planners.
2.  **Flag Fragmentation**: State scattered across EMF models, internal POJOs, and transient context objects.
3.  **Hidden Mutation**: State changes occurring via side-effects in agents or tools rather than explicit kernel transitions.
4.  **Event Drift**: Events carrying commands (imperative) instead of observations (declarative).

---

## 7. REFACTORING PLAN

1.  **BitState Centralization**: Consolidate all behavior logic into `BitState` and `PolicyResolver`.
2.  **Logic Extraction**: Migrate all Darwin/Refinement logic from `IterationManager` into dedicated `Flow` classes.
3.  **Authority Stabilization**: Establish `IterationManager` as the *only* entity allowed to advance `SystemState`.
4.  **Recursion Flattening**: Direct `TaskExecutor` to use the `Orchestrator` directly without routing back through the Kernel.
5.  **Context Unification**: Merge all grounding and scoring logic into the `ContextBuilder` pipeline.

---

## 8. GOLDEN PATH

The **Iterative Darwin Flow** is the canonical execution path.

It grounds **Intent** in the repository, uses **Darwinian Mutation** for architectural exploration, and maintains stability through the **PEV Iteration** loop, guided by continuous **Signals**. All other modes are simply restricted sub-sets (masks) of this flow.
