# Darwin Modes and Instances Architecture Audit

**Milestone Identifier**: 03112026-DARWIN-MODES-AUDIT
**Date**: November 3, 2026
**Scope**: All Darwin modes, execution profiles, workflow configurations, session flags, and routing precedence across `eu.kalafatic.evolution`.

---

## 1. Executive Summary & Objective

The objective of this audit is to establish a unified, coherent mode architecture across the EVO platform. Rather than creating competing engines or ambiguous abstractions, this document clarifies the actual semantics of all existing mode definitions from source code and defines a four-layer taxonomy:

1. **Layer A: User / Application Workflow Modes** (`AiMode`)
   The primary UI- and user-selected AI deployment/interaction modality configured on the `Orchestrator` model (e.g. `LOCAL`, `HYBRID`, `REMOTE`, `PROXY`, `MEDIATED`, `INTENT`, `FORGE`, `DREAM`).
2. **Layer B: Darwin Execution Modes** (`PlatformType` & `ADarwinEngine` subclasses)
   The concrete execution engine targets and runtime state machines managed by `KernelFactory` and `DarwinEngineFactory` (e.g. `SIMPLE_CHAT`, `ASSISTED_CODING`, `DARWIN_MODE`, `SELF_DEV_MODE`, `HYBRID_MANUAL_EXPORT`, `INTENT_RECONSTRUCTION`, `FORGE`, `DREAM_MODE`, `MUTATION`).
3. **Layer C: Specialized Execution Profiles** (`CapabilityType` & `EvolutionProfile`)
   Granular capability traits that dictate iteration limits, concurrency bounds, exploration intensity, and strategy evaluation (e.g. `CapabilityType.CHAT`, `CODE`, `ARCHITECTURE`, `EVOLUTION`, `SELF_DEV`, `MEDIATED`, `INTENT_RECONSTRUCTION`, `FORGE`).
4. **Layer D: Explicit Session Flags & Ephemeral Execution States**
   Short-lived runtime flags stored in `OrchestrationState.getMetadata()`, `TaskContext`, or `BehaviorProfile` (e.g. `isChatRequest`, `isInternalRepair`, `testMode`, `waitingForApproval`, `isSelfIterativeMode`, `isDarwinMode`).

---

## 2. Terminology & Semantics Mapping

Below is the definitive semantic mapping of all mode concepts investigated in the codebase:

| Term / Identifier | Layer | Concrete Type / Representation | Actual Semantics & Implementation |
|---|---|---|---|
| **SIMPLE_CHAT** | Layer B & C | `PlatformType.SIMPLE_CHAT`, `CapabilityType.CHAT`, `ChatEngine` | Direct text interaction. Bypasses multi-variant Darwin exploration loops, tool execution, and code modification. |
| **ASSISTED_CODING** | Layer B & C | `PlatformType.ASSISTED_CODING`, `CapabilityType.CODE`, `CodingEngine` | Guided multi-step development where plan is generated and paused for user review/approval before applying atomic tasks. |
| **DARWIN_MODE** | Layer B & C | `PlatformType.DARWIN_MODE`, `CapabilityType.EVOLUTION`, `DarwinEngine` | Iterative evolutionary search generating competing hypothesis variants, compiling/testing, scoring fitness, and selecting winners. |
| **SELF_DEV_MODE** | Layer B & C | `PlatformType.SELF_DEV_MODE`, `CapabilityType.SELF_DEV`, `SelfDevelopmentEngine` / `SelfDevSupervisor` | Autonomous platform self-evolution with supervisor monitoring, bounded allowed paths, build/export/runtime verification, and rollback safety. |
| **LOCAL** | Layer A | `AiMode.LOCAL` | Direct local execution using local Ollama/llama.cpp inference endpoints without remote network dependencies. |
| **MEDIATED** | Layer A & B | `AiMode.MEDIATED`, `PlatformType.HYBRID_MANUAL_EXPORT`, `MediatedEngine` | Context-bounded execution generating analysis, plans, and exportable ZIP packages without modifying local source code. |
| **INTENT** | Layer A & B | `AiMode.INTENT`, `PlatformType.INTENT_RECONSTRUCTION`, `IntentReconstructionEngine` | Intent-driven reverse engineering and architecture discovery analyzing target repositories/datasets without coding modification. |
| **INFERENCE** | System Service | `ResourceManager.getService("INFERENCE")`, `MemorySource.INFERENCE` | Technical AI inference service (Ollama daemon on port 11434) and provenance tag for AI-generated observations. |
| **CODING** | Layer B & C | `CapabilityType.CODE`, `CodingEngine` | Specialized Darwin execution profile and engine implementation for code generation and refactoring. |
| **FORGE** | Layer A, B & C | `AiMode.FORGE`, `PlatformType.FORGE`, `CapabilityType.FORGE`, `LLMDarwinEngine` | Dedicated LLM model training and dataset acquisition pipeline (.evodata / .evo). Isolated from generic coding or Darwin chat. |
| **DREAM** | Layer A & B | `AiMode.DREAM`, `PlatformType.DREAM_MODE`, `DreamEngine`, `WakeDreamCoordinator` | Background cognitive consolidation analyzing past experiences, failures, and hypotheses during system idle periods. |

---

## 3. Specification Matrix for All Modes

### 3.1 SIMPLE_CHAT
- **Purpose**: Fast conversational response, general knowledge question answering, greetings, and meta-discussions.
- **Entry Conditions**: User input is a greeting (`isSimpleGreeting`), explicit prompt override `mode: chat`, or LLM classifies as `CHAT` / `UNKNOWN`.
- **Required Context**: Session container, conversational history (`ConversationState`), chat prompt.
- **Allowed Capabilities**: `CapabilityType.CHAT`.
- **Model Routing**: Chat model (`orchestrator.getAiChat().getModel()` or default Ollama model).
- **Tool Permissions**: No code modification, no Maven build, no Git commit.
- **Iteration Behavior**: Strictly single pass (`iterationLimit = 1`), evolutionary loop bypassed.
- **Memory/Genome Interaction**: Read-only conversation history; logs user prompt and assistant response.
- **Persistence Requirements**: Stored in `ConversationState` and SQLite `messages`.
- **Exit Conditions**: Immediate completion upon response generation.
- **Cancellation Behavior**: Instant thread interruption.
- **UI Representation**: Standard AiCommander chat transcript.

### 3.2 ASSISTED_CODING
- **Purpose**: Guided code implementation, refactoring, and debugging with user gatekeeping.
- **Entry Conditions**: Explicit prompt override `mode: assisted`, high-confidence `TASK` classification from `PromptIntentAnalyzer`, or UI assisted coding selection.
- **Required Context**: Project root, open file/selection context, compiler toolchain.
- **Allowed Capabilities**: `CapabilityType.CODE`.
- **Model Routing**: Coding model (`orchestrator.getOpenAi().getModel()` / Ollama code model).
- **Tool Permissions**: Read workspace files, generate proposed changes, await user approval before file write.
- **Iteration Behavior**: Bounded iterations (`iterationLimit = 2`), step-by-step review.
- **Memory/Genome Interaction**: Reads workspace structure and symbols; records execution results to `SessionLogger`.
- **Persistence Requirements**: Review sessions, proposed file changes in `ReviewSession`.
- **Exit Conditions**: User approves or rejects plan, tasks complete.
- **Cancellation Behavior**: Graceful stop; unapplied file diffs discarded.
- **UI Representation**: Approval page task list, review diff viewer.

### 3.3 DARWIN_MODE
- **Purpose**: Multi-variant evolutionary problem solving and optimization.
- **Entry Conditions**: `orchestrator.isDarwinMode() == true` or prompt `mode: darwin`.
- **Required Context**: Workspace project root, Git repository, build tools (Maven/Tycho).
- **Allowed Capabilities**: `CapabilityType.EVOLUTION`.
- **Model Routing**: High-reasoning model.
- **Tool Permissions**: Git branch creation, variant generation, test execution, fitness evaluation.
- **Iteration Behavior**: Multi-iteration loop (default 3-5 iterations) with fitness selection.
- **Memory/Genome Interaction**: Updates `IterationMemoryService`, genome candidate records, and fitness telemetry.
- **Persistence Requirements**: Iteration history in `iteration-report.json`, branch checkpoints.
- **Exit Conditions**: Convergence reached, target fitness achieved, or max iterations exhausted.
- **Cancellation Behavior**: Reverts active experimental branch back to original branch.
- **UI Representation**: Iteration Page, branch visualizer, fitness curves.

### 3.4 SELF_DEV_MODE
- **Purpose**: Autonomous self-evolution of the EVO platform under external supervision.
- **Entry Conditions**: `isSelfIterativeMode() == true` or prompt `mode: self-dev`.
- **Required Context**: EVO workspace repo, supervisor process (`EVOSupervisorServer` on port 8089), preflight checks.
- **Allowed Capabilities**: `CapabilityType.SELF_DEV`.
- **Model Routing**: Autonomous reasoning model.
- **Tool Permissions**: File modifications strictly bounded to `allowedPaths`, Maven build, export, deploy, supervisor verify.
- **Iteration Behavior**: Multi-iteration loop managed by `SelfDevSupervisor` with rollback safety.
- **Memory/Genome Interaction**: Full milestone generation (`MilestoneGenerator`), `genome.json`, functionality inventory sync.
- **Persistence Requirements**: Milestone archives in `genome/`, diagnostic summaries.
- **Exit Conditions**: Platform build, export, and verification pass, or max iterations reached.
- **Cancellation Behavior**: Rollback to pre-iteration Git commit; supervisor stops process.
- **UI Representation**: Development Page, Supervisor Group, Self-Dev progress bar.

### 3.5 LOCAL
- **Purpose**: Normal local execution path using existing Darwin infrastructure without external network mediation.
- **Entry Conditions**: `AiMode.LOCAL` on orchestrator.
- **Required Context**: Local Ollama server / native inference runtime.
- **Allowed Capabilities**: Local inference and local execution.
- **Model Routing**: Local Ollama model (`localhost:11434`) or native `.evo` engine.
- **Tool Permissions**: Dependent on sub-workflow (chat vs coding vs evolution).
- **Iteration Behavior**: Standard Darwin or Chat loops according to task category.
- **Memory/Genome Interaction**: Local workspace SQLite and filesystem memory.
- **Persistence Requirements**: Local memory only.
- **Exit Conditions**: Completion of execution flow.
- **Cancellation Behavior**: Local process/request cancellation.
- **UI Representation**: Offline status indicator, local model combo.

### 3.6 MEDIATED
- **Purpose**: Context-controlled execution generating bounded analysis and exportable ZIP packages without source code mutation.
- **Entry Conditions**: `AiMode.MEDIATED` on orchestrator, prompt `mode: mediated`, or `analyze target` commands.
- **Required Context**: Target directory or project snapshot.
- **Allowed Capabilities**: `CapabilityType.MEDIATED`, `CapabilityType.ARCHITECTURE`.
- **Model Routing**: Local or hybrid LLM.
- **Tool Permissions**: Strictly read-only analysis; file write permissions restricted to export ZIP generation.
- **Iteration Behavior**: Single-pass convergence (`performMediatedExportConvergence`).
- **Memory/Genome Interaction**: Generates `TargetRealityModel`, architecture facts, and hotspots.
- **Persistence Requirements**: Export ZIP package in output folder.
- **Exit Conditions**: Export package assembled and analysis rendered.
- **Cancellation Behavior**: Deletes partial ZIP artifact.
- **UI Representation**: Export download link, Architecture Page analysis summary.

### 3.7 INTENT
- **Purpose**: Explicit intent-driven discovery, dataset sample analysis, and architecture reconstruction without heavy reasoning when workflow is known.
- **Entry Conditions**: `AiMode.INTENT` on orchestrator, prompt `mode: intent`, or intent discovery keywords.
- **Required Context**: Target path (repository, dataset directory, documentation).
- **Allowed Capabilities**: `CapabilityType.INTENT_RECONSTRUCTION`.
- **Model Routing**: Lightweight discovery prompts; bypasses large reasoning models when workflow is pre-established.
- **Tool Permissions**: Read-only scanning via `TargetScanner` and `SourceAdapterRegistry`.
- **Iteration Behavior**: Single-pass discovery after initial reality model establishment (skips redundant passes).
- **Memory/Genome Interaction**: Enriches `TargetRealityModel` and component deduplication.
- **Persistence Requirements**: `design_model.json` updates.
- **Exit Conditions**: Target classification and inventory generated.
- **Cancellation Behavior**: Immediate scan abort.
- **UI Representation**: Architecture Page component tree and topology.

### 3.8 FORGE
- **Purpose**: Dedicated LLM dataset acquisition, tokenization, training, and model export (.evo/.gguf).
- **Entry Conditions**: `AiMode.FORGE` on orchestrator, prompt `mode: forge`, or Forge Job trigger.
- **Required Context**: Dataset source configuration, output directory (`forge-input`/`gpu-output`), hardware profile.
- **Allowed Capabilities**: `CapabilityType.FORGE`.
- **Model Routing**: Native training pipeline (`LLMDarwinEngine`, `EvoLlmTrainer`, `ForgeOrchestrator`).
- **Tool Permissions**: Download datasets, read/write .evodata and .evo files; strictly isolated from workspace code modification.
- **Iteration Behavior**: Epoch-based training loop with evolutionary candidate architecture screening.
- **Memory/Genome Interaction**: Model weights, vocabulary cache, training telemetry metrics.
- **Persistence Requirements**: `.evodata` archives, `.evo` checkpoints, `training-report.json`.
- **Exit Conditions**: Epoch budget met, target loss achieved, or user stop.
- **Cancellation Behavior**: Checkpoint preserved; training thread safely terminated.
- **UI Representation**: Forge Page live dashboard, SVG loss curves, Observability panel.

### 3.9 DREAM
- **Purpose**: Background analytical cognitive processing analyzing memories, failures, and hypotheses during idle times.
- **Entry Conditions**: `AiMode.DREAM` on orchestrator, prompt `mode: dream`, or automatic activation via `WakeDreamCoordinator`.
- **Required Context**: Iteration memory, failure memories, Functionality Inventory.
- **Allowed Capabilities**: Analytical hypothesis generation and verification.
- **Model Routing**: Background inference model.
- **Tool Permissions**: Read-only workspace inspection; write to `MemoryService` (unverified hypotheses).
- **Iteration Behavior**: Bounded 6-stage analytical cycle.
- **Memory/Genome Interaction**: Consolidates `UNVERIFIED_HYPOTHESIS` into `MemoryService`.
- **Persistence Requirements**: `dream_checkpoint.json` for crash recovery.
- **Exit Conditions**: Cooldown reached, analysis complete, or preemption by foreground WAKE request.
- **Cancellation Behavior**: Immediate preemption and state save on user interaction.
- **UI Representation**: Dream status icon, Instructions group toggle.

---

## 4. Mode Precedence and Dispatch Hierarchy

To eliminate ambiguity and prevent uncontrolled escalation loops (such as "hi" escalating into `ASSISTED_CODING` or malformed LLM responses escalating into technical tasks), the authoritative dispatch precedence is:

```
[User Input / Prompt Received]
           │
           ▼
1. Authoritative Explicit UI Mode Overrides (NON-NEGOTIABLE)
   ├─ AiMode.FORGE              ──> PlatformType.FORGE
   ├─ AiMode.DREAM              ──> PlatformType.DREAM_MODE
   ├─ AiMode.MEDIATED           ──> PlatformType.HYBRID_MANUAL_EXPORT
   ├─ AiMode.INTENT             ──> PlatformType.INTENT_RECONSTRUCTION
   ├─ isSelfIterativeMode()     ──> PlatformType.SELF_DEV_MODE
   └─ isDarwinMode()            ──> PlatformType.DARWIN_MODE
           │ (None configured / default)
           ▼
2. Pure Conversational Greeting Short-Circuit (PromptIntentAnalyzer.isSimpleGreeting)
   └─ "hi", "hello", "hey", "thanks", etc. ──> PlatformType.SIMPLE_CHAT
           │ (Not a greeting)
           ▼
3. Explicit Prompt Directives (Manual User Routing)
   ├─ "mode: chat"              ──> PlatformType.SIMPLE_CHAT
   ├─ "mode: assisted"          ──> PlatformType.ASSISTED_CODING
   ├─ "mode: darwin"            ──> PlatformType.DARWIN_MODE
   ├─ "mode: self-dev"          ──> PlatformType.SELF_DEV_MODE
   ├─ "mode: intent"            ──> PlatformType.INTENT_RECONSTRUCTION
   ├─ "mode: mediated"          ──> PlatformType.HYBRID_MANUAL_EXPORT
   ├─ "mode: dream"             ──> PlatformType.DREAM_MODE
   └─ "mode: forge"             ──> PlatformType.FORGE
           │ (No explicit directive)
           ▼
4. Fast Technical Mode / Intent Bypass
   └─ If forced mode pre-established, bypass LLM intent analysis completely.
           │
           ▼
5. Cognitive Intent Analysis (PromptIntentAnalyzer / CognitiveAnalysisPipeline)
   ├─ Malformed response / unparseable JSON ──> UNKNOWN ──> Safe Fallback: SIMPLE_CHAT
   ├─ Low confidence (< 0.6)                ──> Safe Fallback: SIMPLE_CHAT
   ├─ Valid TASK (confidence >= 0.6)        ──> ASSISTED_CODING
   ├─ Valid CONTROL                         ──> HYBRID_MANUAL_EXPORT / Workflow Control
   └─ Default / Unsure                      ──> SIMPLE_CHAT
```

This ensures:
1. "hi" or general conversation never escalates to `ASSISTED_CODING` or `DARWIN_MODE`.
2. Malformed model responses safely fail or classify as `UNKNOWN`/`CHAT`, never triggering uncontrolled repair loops into `TASK`.
3. Forced modes never incur latency from unnecessary LLM classification passes.
