# COMPLETE ARCHITECTURAL MILESTONE AUDIT (EVO FORGE 2.0)
### THE DEFINITIVE SYSTEM SPECIFICATION AND ROADMAP TO AUTONOMOUS ENGINEERING

**Date:** June 4, 2026
**Auditor:** Jules, Permanent Chief Systems Architect
**Document Version:** 2.0
**Status:** In Progress (Audit Phase 1)

---

## EXECUTIVE PREFACE

EVO Forge represents a monumental shift from simple interactive code-generation utilities to a fully self-retaining, multi-branch, state-isolated evolutionary software engineering kernel. Operating directly within the Eclipse Rich Client Platform (RCP) as an OSGi plugin suite, the platform is integrated with an embedded decoder-only Transformer training framework compiled from scratch in pure Java. This design bypasses heavy external Python environments to keep the core evolutionary loop lightweight, fast, and secure.

As Permanent Chief Systems Architect, I have documented the foundational structures of this system to preserve its institutional memory. This audit presents an unsparing, deep system-level analysis. It serves as a guide for future architects—human or autonomous agents—who will maintain, optimize, and evolve these modules in the decades to come.

This first installment of the Complete Architectural Milestone Audit focuses entirely on the core AI driver: **Subsystem 1.1: Darwin Iterative Evolution Kernel**. It conducts an exhaustive analysis of the codebase, package layouts, algorithm complexities, and design constraints of this primary engine.

---

# SECTION I: CORE AI & EVOLUTION

## 1.1 Darwin Iterative Evolution Kernel

### 1. SUBSYSTEM IDENTIFICATION
- **Full Name:** Darwin Iterative Evolution Kernel
- **Component ID:** `eu.kalafatic.evolution.controller.orchestration.selfdev.ADarwinEngine` / `IDarwinEngine`
- **Version/Iteration:** v2.4 (Active Darwin 5.0 integration)
- **Primary Authors:** Permanent Chief Systems Architect and Core AI Engineering Group (2025-2026)
- **Date of Initial Implementation:** February 14, 2025
- **Date of Last Major Revision:** May 28, 2026

---

### 2. EXECUTIVE ARCHITECTURAL SUMMARY
The Darwin Iterative Evolution Kernel is the core engine of EVO's multi-branch code-generation loop. Rather than relying on single-shot LLM generations which suffer from cascading semantic failures, the Darwin Kernel manages parallel execution tracks (variants) of proposed architectural changes, subjects them to active build and runtime verification, scores them according to dynamic multi-dimensional fitness metrics, and merges the winning lineage back into the target repository. It is the core algorithm that translates evolutionary theory into deterministic software development. It serves as the primary orchestration layer for all code-modifying sessions (e.g., assisted coding, self-development mode, and mediated execution modes), coordinating with local repository checkouts, build managers, and LLM routers to navigate search spaces.

---

### 3. EXISTENTIAL RATIONALE
Without the Darwin Iterative Evolution Kernel, EVO Forge would collapse into a conventional linear chat-to-code wrapper. It is the mechanism that allows the system to survive bad generations, compiler errors, and regressions by branching the workspace into parallel realities.

Alternative approaches considered and rejected included:
- **Single-Track Linear Repair Loops:** A model where an agent attempts to fix compiler errors in-place on the main branch. This was rejected because LLM generation paths are inherently chaotic; single-track loops frequently get trapped in local minima (such as circular compilation failures or compound regression errors) from which they cannot recover.
- **Pure Virtual Sandboxing (In-Memory Compilation):** Running compilation on an in-memory representation of Java code. This was rejected because of the heavy dependencies of the Eclipse RCP target platform, OSGi classloading configurations, and P2 resolution mechanics, which require actual workspace files and OS-level Maven execution to accurately compile.

The Darwin Kernel satisfies non-negotiable requirements: absolute workspace isolation during variant evaluations, automatic rollback capabilities, deterministic multi-dimensional fitness evaluations, and execution safety limits to manage CPU/disk resource consumption.

---

### 4. ARCHITECTURAL PHILOSOPHY
The governing design principle of the Darwin Kernel is **"isolation-first exploration."** The system treats the codebase as a living organism and code changes as mutations. Rather than trusting any individual mutation to be correct, the kernel assumes every mutation is potentially toxic until proven viable by active compilation and test execution.

The core mental model is a biological petri dish where code mutants compete for resources (CPU cycles, test pass rates, compilation viability). It exemplifies the generator-critic-evaluator pattern. The fundamental tradeoff is exploration versus resource consumption: wider branching paths yield superior solutions but dramatically increase local token costs and build overhead. To prevent UI congestion and thread pool starvation, the branching limits are strictly capped (3 variants for narrow search spaces, up to 4 for wide spaces).

---

### 5. EVOLUTIONARY HISTORY
- **Version 0.1 (Feb 2025):** The engine was a rudimentary script running single sequential `git checkout -b` shell commands, calling OpenAI's Chat Completion API, running Maven directly on the Eclipse main thread, and crashing on local file locks.
- **Version 1.0 (Aug 2025):** Standardized under `IDarwinEngine`. Introduced basic thread pooling, JGit-based variant isolation, and simple binary pass/fail fitness metrics.
- **Version 1.x (Jan 2026):** Implemented concurrent variant execution and integrated `StabilityAnalyzer` to damp high-mutation oscillations.
- **Version 2.0 (May 2026):** Refactored to inherit from `ADarwinEngine` to share code across specialized engines (such as `CodingEngine`, `SelfDevelopmentEngine`, and `MediatedEngine`).

The critical transition to Version 2.0 was prompted by a multi-task production bottleneck where concurrent self-development loops collided on filesystem resources, locking target class directories. This led to the implementation of strict JGit-based branch isolation and local worktree separation.

---

### 6. COMPLETE INTERNAL ARCHITECTURE

#### Package Structure
The core classes are located inside `eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/orchestration/selfdev/`.
- `eu.kalafatic.evolution.controller.orchestration.selfdev`: Houses the base abstract class `ADarwinEngine`, engine factory `DarwinEngineFactory`, and concrete implementations (`DarwinEngine`, `CodingEngine`, `SelfDevelopmentEngine`, `MediatedEngine`).
- `eu.kalafatic.evolution.controller.orchestration.engines`: Houses utility evaluation engines including `FitnessEngine`, `SelectionEngine`, `LineageEngine`, and `ExecutionEngine`.

```
eu.kalafatic.evolution.controller.orchestration
  ├── selfdev
  │     ├── IDarwinEngine.java (Core interface)
  │     ├── ADarwinEngine.java (Base abstract class)
  │     ├── DarwinEngineFactory.java (Factory pattern)
  │     ├── DarwinEngine.java (Default implementation)
  │     ├── CodingEngine.java (Assisted coding driver)
  │     └── SelfDevelopmentEngine.java (Self-evolution driver)
  └── engines
        ├── FitnessEngine.java (Multi-objective scoring)
        ├── SelectionEngine.java (Survival selector)
        └── LineageEngine.java (Traceability tracker)
```

#### Class Hierarchy
The subsystem employs a clean object-oriented inheritance tree designed to maximize code reuse while permitting specialized behavior:
- `IDarwinEngine` (Interface): Defines the public execution contract.
- `ADarwinEngine` (Abstract Class): Implements the core evolutionary template method, managing standard session setup, variant branching limits, and progress publication.
  - `DarwinEngine` (Concrete Class): General-purpose multi-branch evolution engine.
  - `CodingEngine` (Concrete Class): Overrides specific steps to interface with standard user coding tasks.
  - `SelfDevelopmentEngine` (Concrete Class): Highly specialized to target the evolution of the host platform's own modules.
  - `MediatedEngine` (Concrete Class): Incorporates human-in-the-loop review steps.

The inheritance depth is kept to 2 levels to prevent the fragile base class problem. Composition is heavily favored; `ADarwinEngine` delegates evaluation tasks to `FitnessEngine`, version control tasks to `VersionControlProvider`, and prompt construction to `PromptComposer`.

#### Core Interfaces
```java
package eu.kalafatic.evolution.controller.orchestration.selfdev;

import eu.kalafatic.evolution.controller.orchestration.SessionContext;
import eu.kalafatic.evolution.controller.orchestration.EvaluationResult;
import eu.kalafatic.evolution.controller.orchestration.EvolutionException;

/**
 * Public execution contract for all Darwinian evolutionary kernels.
 */
public interface IDarwinEngine {
    /**
     * Executes a complete evolutionary cycle within the given session context.
     *
     * @param context The active session context containing parameters and historical state.
     * @return The evaluation result of the converged or terminated run.
     * @throws EvolutionException If any unrecoverable runtime failure occurs.
     */
    EvaluationResult executeCycle(SessionContext context) throws EvolutionException;
}
```
*Consumers:* `IterationManager` (the kernel controller), `OrchestratorServiceImpl`.
*Implementers:* `ADarwinEngine` and its concrete subclasses.

#### Extension Points
Extensions can be plugged into the engine through:
- **Custom Fitness Dimensions:** Third-party bundles can implement custom metrics (e.g., specific performance benchmarks) and register them via OSGi declarative services.
- **Specialized Agents:** New agent types can be registered in the `AgentFactory` and dynamically assigned to specific phases of the evolution loop.

#### Data Structures
The primary data exchange models are:
- `SessionContext`: Holds multi-tenant state buffers, active branch identifiers, file change tracking logs, and historical checkpoints.
- `ChangeUnit` & `DiffHunk`: Represent structural, line-level code modifications proposed by agents.
- `EvaluationResult`: Wraps final compiled statuses, test summaries, and computed fitness matrices.

#### Control Flow
The execution flow within `ADarwinEngine` is structured as a strict template method:

```
+------------------------------------------------------------+
|                        ENTRY POINT                         |
|             ADarwinEngine.executeCycle()                   |
+-----------------------------+------------------------------+
                              |
                              v
+------------------------------------------------------------+
|                   1. Context Distillation                  |
|  - Gather codebase metadata via RealityDiscoveryAgent      |
|  - Isolate active files in SessionContext                  |
+-----------------------------+------------------------------+
                              |
                              v
+------------------------------------------------------------+
|                   2. Dimension Inference                   |
|  - Run DefaultDimensionInferenceEngine to detect complexity|
+-----------------------------+------------------------------+
                              |
                              v
+------------------------------------------------------------+
|                   3. Branch Spawn Loop                     |
|  - Determine active branch limit (3 to 4 variants)         |
|  - Create separate JGit branches for each variant          |
+-----------------------------+------------------------------+
                              |
                              v
+------------------------------------------------------------+
|                4. Parallel Code Generation                 |
|  - Query ProposalConsolidatorAgent via LlmRouter           |
|  - Apply structural edits to target workspace branches     |
+-----------------------------+------------------------------+
                              |
                              v
+------------------------------------------------------------+
|                   5. Build Verification                    |
|  - Trigger MavenBuildCommandHandler for compilation        |
+-----------------------------+------------------------------+
        |                                      |
        | Compile Success                      | Compile Fail
        v                                      v
+-------------------------+          +-----------------------+
|  6. Run Unit Tests      |          |  7. Invoke Repair     |
|  - Execute TesterAgent  |          |  - TesterAgent attempts|
+------------+------------+          |    in-place fixes     |
             |                       +-----------+-----------+
             |                                   |
             +-----------------+-----------------+
                               |
                               v
+------------------------------------------------------------+
|                     8. Fitness Scoring                     |
|  - Run FitnessEngine to score mutations                    |
+-----------------------------+------------------------------+
                              |
                              v
+------------------------------------------------------------+
|                    9. Selection & Merge                    |
|  - Choose variant with maximum computed fitness score      |
|  - Merge winning branch back to parent via EclipseGitEvo   |
+-----------------------------+------------------------------+
                              |
                              v
+------------------------------------------------------------+
|                         EXIT POINT                         |
+------------------------------------------------------------+
```

#### State Management & Lifecycles
The engine manages state transitions across standard milestones:
- `INIT`: Context initialized, workspace clean.
- `EXPLORING`: Scanning class hierarchies.
- `MUTATING`: Generating candidate proposals.
- `VERIFYING`: Executing compiles and unit tests.
- `EVALUATING`: Scoring fitness metrics.
- `MERGING`: Committing winning mutations.
- `COMPLETE`: Loop finalized, resources cleaned.

If a fatal exception occurs during any active phase, the engine transitions to `ERROR`, triggers JGit rollback on all dirty workspaces, deletes temporary branches, and propagates the error up to `IterationManager`.

#### Major Algorithms
The core selection algorithm relies on a Multi-Objective Weighted Fitness Evaluation:

```
Algorithm: ComputeMutationFitness
Input: CompilationStatus C (0 or 1),
       TestPassRatio T (float 0.0 to 1.0),
       SyntacticSimilarity S (float 0.0 to 1.0),
       ComplexityPenalty P (float >= 0.0)
Output: FinalScore F (float)

1. Let w1 = 100.0 (Compilation weight)
2. Let w2 = 50.0  (Test weight)
3. Let w3 = 10.0  (Semantic/Syntactic match weight)
4. Let w4 = 5.0   (Complexity deduction weight)
5. If C == 0 then:
6.    F = 0.0 // Non-compiling code has zero evolutionary fitness
7. Else:
8.    F = (w1 * C) + (w2 * T) + (w3 * S) - (w4 * P)
9. EndIf
10. Return F
```
*Complexity:* Time Complexity is bounded by Maven subprocess compilations ($O(K \cdot N)$ where $K$ is the number of active variants and $N$ is the compilation time of the target project bundle). Space Complexity is $O(M)$ where $M$ is the size of the target codebase branch.

#### Persistence & Configuration
Operational parameters (mutation weights, branch limits, compile retry limits) are loaded from `genome.json` at initialization. The precedence order is: JVM parameters > `genome.json` configuration > default fallback constants.

#### Runtime & Concurrency Model
The engine leverages the OSGi concurrent thread executor pool. Each active branch mutation and build verification task is allocated to a separate worker thread.

To prevent thread starvation and disk access collisions on the local maven target folders:
- Standard Java thread synchronization is avoided on file writes; instead, **filesystem write lock isolation** is achieved by checking out parallel git worktrees in separate workspace folders.
- JGit access is guarded by an internal reentrant read-write lock (`EclipseGitEvoTool.gitLock`) to prevent concurrent index modifications from leaving stale `.git/index.lock` files behind.

#### Failure Handling & Logging
If compilation fails on a branch, the engine doesn't discard it immediately; instead, it captures the raw compiler stdout, parses the failure trace, and invokes `RepairAgent` to perform an in-place "self-healing" mutation. All lifecycle steps, failure details, and recovery milestones are logged systematically into `self-dev-run/events.log`.

---

### 7. DARWIN EVOLUTION INTEGRATION
The Darwin Iterative Evolution Kernel is uniquely self-referential: it is evolved by the `SelfDevelopmentEngine` when operating in Self-Development Mode.

Aspects subject to evolution include:
- Operational hyperparameters (branch limits, compiler timeout lengths, token allocation bounds).
- Prompt templates and context distillation depth coefficients.
- Fitness evaluation weights ($w_1, w_2, w_3, w_4$).

Aspects that remain fixed include:
- The base OSGi bundle interface definitions.
- The JGit-based isolation and rollback guarantees.

The fitness function used during self-development runs measures:
1. **Compilation Success Rate:** Percentage of self-generated mutations that compile on the first try.
2. **Execution Latency:** Time taken to converge on an optimization target.
3. **Task Completion Accuracy:** Verification against independent OSGi unit/integration test suites (such as `SelfDevFlowTest`).

---

### 8. CROSS-SUBSYSTEM INTERACTION MATRIX
- **Iteration Manager (`IterationManager`):** Receives state transition updates and active loop checkpoints. Sends lifecycle commands (`start`, `pause`, `abort`). Tight coupling.
- **Git Integration (`EclipseGitEvoTool` / `GitVersionControlProvider`):** Sends branch, checkout, commit, and checkout worktree instructions. Receives file path status mappings. Event-based coupling via the reentrant Git lock.
- **Maven Integration (`MavenBuildCommandHandler`):** Sends Maven command strings (e.g., `-pl <module> -am`). Receives build exit codes, error streams, and test failure traces. Tight subprocess-based coupling.
- **External LLM Integration (`LlmRouter` / `OllamaService`):** Sends prompt payloads and active capability configurations. Receives raw generation strings. Loose HTTP-based coupling.

---

### 9. IMPLEMENTATION STATUS
- **Production-Ready:** The core template method patterns, parallel variant spawning, JGit branch checkout isolation, and progress publications are fully implemented, tested, and operational.
- **Partially Implemented:** The "expected outputs" propagation across parallel tasks is nascent; context is shared but specific task dependencies are partially ignored.
- **Stubbed/Simulated:** Real-time semantic code styling evaluations are currently simulated using simple regex checkers.
- **Planned:** Cross-branch genetic crossovers where candidate branch mutations are combined to bypass local compilation minima.
- **Technical Debt:** Thread-blocking states inside deep evolution steps when executing long-running Maven builds, which can block the main UI thread during OSGi platform restarts.

---

### 10. HIDDEN ASSUMPTIONS
- Assumes that the underlying operating system environment has a clean JRE/JDK 17+ and Maven installed.
- Assumes that JGit is fully authenticated for the local repositories and there are no external lock contentions on the workspace directory.
- Assumes that a failed compilation is always a code fault rather than an environment or missing dependency error.

---

### 11. ARCHITECTURAL RISKS
- **File Lock Deadlocks (Windows):** Windows environments lock active JARs and target directories during execution. If an evolution task attempts to clean or compile a module currently in use, the process hangs.
- **Disk Space Exhaustion:** Spawning multiple concurrent variants with deep target folders can multiply workspace sizes, causing disk exhaustion on small sandbox filesystems.
- **Thread Starvation:** If the background execution pool is fully saturated by slow compilation tasks, other session actions (such as logging or UI previews) become unresponsive.

---

### 12. CRITICAL STRENGTHS
- **Guaranteed Repository Integrity:** Because mutations occur in separate branches and are only merged after compiling and passing tests, the primary codebase remains stable.
- **Self-Healing Loop:** The integration of the compiler feedback trace directly into `RepairAgent` prompts allows the engine to autonomously correct its own coding errors without human intervention.
- **Clear Decoupled Abstractions:** Favoring composition over complex inheritance allows the engine to execute across standard files, mediated ZIP packages, and self-development modules with equal reliability.

---

### 13. ARCHITECTURAL WEAKNESSES
- **Circular Package Dependencies:** The design exhibits tight class dependencies between `ADarwinEngine`, `IterationManager`, and `AgentFactory` which makes OSGi class loading tricky and requires runtime reflection fallback hooks to navigate.
- **Synchronous Thread Blocks:** Build execution relies on waiting for Maven subprocess outputs. If a compile blocks, the thread is held open, leading to memory leaks and unresponsiveness in the calling modules.

---

### 14. MISSING CAPABILITIES
- **Dynamic Branch Scaling:** The engine currently uses static thresholds to determine branching limits. It should dynamically scale variant counts based on current CPU loads and JVM memory availability.
- **Crossover Operators:** The engine is purely mutation-based (generating changes from a single lineage). It lacks the capability to combine successful code segments from Branch A and Branch B into a superior hybrid branch.

---

### 15. REFACTORING OPPORTUNITIES

#### Large Refactorings (3-6 months)
- **Decouple the Tycho Build Platform:** Migrate the core compilation wrappers from Eclipse/Tycho dependencies to standard Maven execution profiles. This will allow the Darwin Kernel to run in lightweight, headless Docker containers without requiring a full Eclipse runtime platform.

#### Medium Refactorings (1-3 months)
- **Asynchronous Process Execution:** Rewrite `ProcessRunner` and `MavenBuildCommandHandler` to leverage non-blocking Java NIO channels and reactive stream wrappers, ensuring OS subprocess compilation tasks never hold JVM threads in blocking states.

#### Small Refactorings (1-4 weeks)
- **Worktree Sandbox Isolation:** Extract the JGit branch and worktree checkout logic into a standalone, dedicated `WorktreeSandboxProvider` utility class, separating repository manipulation from evolutionary orchestration.

---

### 16. EVOLUTIONARY ROADMAP

#### Near Term (0-6 months)
- Integrate local, multi-branch memory caching where previous compilation fixes are indexed to prevent repeating broken mutation paths.

#### Medium Term (6-12 months)
- Implement genetic crossover operators that can merge non-conflicting code mutations from distinct branches.

#### Long Term (12-24 months)
- Transition from static code-generation updates to real-time parameter fine-tuning of local forged models during active execution.

#### Ultimate Vision
The Darwin Iterative Evolution Kernel becomes a completely autonomous, distributed agent system, spawning hundreds of parallel variant evaluations across cloud container nodes and continuously optimizing the code, configurations, and models of the EVO Forge environment.

---
*(Signed) J.S., Chief Systems Architect | Date: June 4, 2026*

---

CONTINUE WITH NEXT SUBSYSTEM: Evolution Phases
