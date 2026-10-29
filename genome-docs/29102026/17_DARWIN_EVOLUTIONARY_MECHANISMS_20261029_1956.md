# 17. DARWIN EVOLUTIONARY MECHANISMS: WHAT ACTUALLY EVOLVES?

**Document type:** Critical Assessment of Evolutionary Computation Mechanisms
**System under analysis:** Darwin Evolutionary Engines and Trajectory Systems
**Analysis Identifier:** EVO-DARWIN-EVO-MECHANISMS-20261029
**Timestamp:** 2026-10-29 19:56 UTC
**Repository Commit:** `529bc25c2b0d703e3d7db5520fd4438b5cbfe3ab`

---

## 1. CRITICAL ANALYSIS: METAPHOR VS. IMPLEMENTATION

In software engineering, the term "evolutionary algorithm" requires specific mathematical and computational properties:
1. A population of candidate representations (chromosomes/genomes).
2. Variation operators (mutation and crossover/recombination).
3. Objective evaluation functions (fitness functions).
4. Selection pressure favoring higher-fitness candidates across generations.
5. Retention of advantageous traits and inheritance across generations.

When evaluating Darwin against these criteria, the codebase reveals **two fundamentally distinct operational regimes**:

---

## 2. REGIME 1: SOFTWARE DEVELOPMENT & CODING MODES (`ADarwinEngine`)

In general software development, coding, and self-development tasks, Darwin implements **iterative prompt refinement and parallel candidate sampling**, rather than genetic evolutionary search.

### Analysis of Operational Elements:
1. **Unit of Evolution:**
   - The "evolving entity" is the `BranchVariant` object, which encapsulates an LLM prompt strategy, a `SemanticAnchor`, a list of proposed actions (`BranchVariant.Action`), and predicted trade-offs.
   - **Gap:** There is no explicit genetic representation of code. Code patches are generated afresh by an LLM in each iteration based on prompt instructions.
2. **Mutation Mechanics:**
   - "Mutation" is performed by `SiblingGenerationManager` and `DarwinVariantSpawner`, which query an LLM with varied reasoning strategies (e.g. conservative vs. exploratory) or different focus dimensions.
   - **Gap:** Mutation is stochastic natural-language prompting, not deterministic genetic mutation.
3. **Crossover / Recombination:**
   - **Absent.** There is no mechanism to recombining AST nodes or code blocks from Variant A and Variant B to create Variant C.
4. **Fitness Evaluation:**
   - `FitnessEngine` and `DarwinFitnessRanker` compute scores using heuristic formulas combining semantic distance to `SemanticEnvelope`, keyword overlap, and predicted risk scores.
   - **Reality Check:** In `RealityLevel.MEDIUM` or `HEAVY`, compilation passes (`mvn compile`) or build results provide binary fitness gates.
5. **Selection & Retention:**
   - `AuthorityController` marks the winning variant as `ACTIVE` and alternative viable candidates as `KEPT` for diversity in `EvolutionTree`.
   - `ACTIVE` winner changes are committed to Git or applied to workspace files.

---

## 3. REGIME 2: NATIVE MODEL FORGING (`LLMDarwinEngine`)

In `PlatformType.FORGE`, Darwin implements **genuine evolutionary hyperparameter search and neural network training**.

### Analysis of Operational Elements:
1. **Unit of Evolution:**
   - The evolving entity is `LlmConfig` (capturing vocabulary size, embedding size, layer count, attention heads, feed-forward dimension, max sequence length, temperature, top-P, top-K, and repeat penalty) and `EvoLlmModel` parameters.
2. **Mutation Mechanics (`mutate`):**
   - Reproducible mathematical mutation transforms `LlmConfig` fields across generations using seeded pseudo-random deltas:
     - Delta 1: Modifies embedding size and vocabulary size.
     - Delta 2: Modifies layer count and head count.
     - Delta 3: Modifies sequence length, epoch counts, and temperature/top-P.
3. **Fitness Evaluation (`CandidateTrainingResult`):**
   - Candidates are evaluated by training on clean dataset corpus text and measuring actual validation loss (`valLoss`) and parameter count penalties:
     $$\text{Fitness} = (\text{ValLoss} \times 0.75) + (\text{TrainLoss} \times 0.25) + (\text{ParamCount} \times 10^{-8})$$
4. **Selection & Retention:**
   - Candidate results are sorted deterministically via `compareCandidates(c1, c2)`.
   - The global winner across generations (`overallWinner`) is selected, reconstructed, and compiled into a native `.evo` binary container (`EvoModelArtifact`) and GGUF archive (`OllamaExporter`).

---

## 4. PERSISTENT VS. TRANSIENT EVOLUTIONARY CHANGES

| Scope of Change | Mechanism | Persistence Duration | Affected Artifacts |
|---|---|---|---|
| **Transient Context** | Prompt assembly (`PromptComposer`) | Single iteration / pass | LLM context window memory |
| **Session State** | `OrchestrationState`, `TrajectoryMemory` | Duration of active session | In-memory session context |
| **Workspace Code** | Git commits (`GitManager`) / file writes | Permanent on disk | Source files, `.java`, `.json` |
| **Platform Knowledge** | `MemoryService` (`user_memory.json`) | Permanent across sessions | `MemoryEntry` facts, rules, goals |
| **Model Artifacts** | `EvoModelArtifact` (`.evo` ZIP container) | Permanent on disk | Binary float tensors, `metadata.json`, GGUF |

---

## 5. MISSING CAPABILITIES FOR CONTROLLED EVOLUTIONARY SEARCH

To transform Darwin from prompt orchestration into a true evolutionary engine for software, the platform requires:

1. **AST-Based Code Mutation & Crossover Operators:**
   - Deterministic structural mutation operators (statement swapping, condition boundary mutation, method extraction) operating directly on Java ASTs via Spoon or Eclipse JDT.
2. **Test-Driven Objective Fitness Functions:**
   - Replacing heuristic string distance with quantitative execution metrics: unit test pass ratios, execution time benchmarks, code coverage percentages, and memory allocations.
3. **Genetic Crossover between Winning Trajectories:**
   - Merging non-conflicting code patches from multiple `KEPT` variants into a unified hybrid candidate.
4. **Independent Verification Boundaries:**
   - Ensuring candidate evaluation is performed by isolated test runners rather than relying on LLM self-evaluation.
