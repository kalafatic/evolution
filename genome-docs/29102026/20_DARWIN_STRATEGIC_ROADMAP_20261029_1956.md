# 20. DARWIN STRATEGIC ROADMAP AND EXPERIMENTAL VALIDATION PLAN

**Document type:** Prioritized Strategic Roadmap & Experimental Validation Framework
**System under analysis:** Future Development Horizons for Darwin and EVO Platform
**Analysis Identifier:** EVO-DARWIN-ROADMAP-20261029
**Timestamp:** 2026-10-29 19:56 UTC
**Repository Commit:** `529bc25c2b0d703e3d7db5520fd4438b5cbfe3ab`

---

## 1. FOUR-HORIZON STRATEGIC ROADMAP

```
 HORIZON 1: ESTABLISH THE CORE  ──►  HORIZON 2: ENHANCE CAPABILITIES  ──►  HORIZON 3: CONTROLLED EVOLUTION  ──►  HORIZON 4: PLATFORM EXPANSION
 (Class hierarchy cleanup,           (Feedback loops, progress           (Multi-gen candidate search,            (Distributed Darwin nodes,
  contract unification, logging)       evaluation, context management)     test-driven fitness gates)              AI supervisor hosting)
```

---

### HORIZON 1 — ESTABLISH THE CORE (Immediate)
**Objective:** Clean up class hierarchy, enforce contract unification, and eliminate architectural drift.

- **Tasks:**
  1. Consolidate `AbstractDarwinEngine`, `AbstractDarwinEngine2`, and `ADarwinEngine` into a single canonical abstract base class `ADarwinEngine`.
  2. Enforce explicit interface implementation across all specialized engines in `DarwinEngineFactory`.
  3. Replace blocking thread sleep/wait loops with explicit asynchronous event-driven state transitions where appropriate.
  4. Ensure all specialized engines explicitly report capability health and lifecycle status via `ICapability`.

---

### HORIZON 2 — MAKE EXECUTION MORE CAPABLE (Near-Term)
**Objective:** Enhance cognitive context management, progress evaluation, and feedback loops.

- **Tasks:**
  1. Integrate `RejectionPatternAnalyzer` outcomes directly into `PromptComposer` to actively suppress repeating failed strategies across iterations.
  2. Implement quantitative progress detection in `IterationManager` to early-terminate stagnant evolutionary loops.
  3. Standardize `TargetRealityModel` discoveries across all operational modes, ensuring consistent architectural facts are available to LLM prompts.
  4. Strengthen session checkpoint serialization for complex metadata maps.

---

### HORIZON 3 — INTRODUCE CONTROLLED EVOLUTION (Mid-Term)
**Objective:** Expand true candidate search and fitness evaluation beyond model forging to software refactoring.

- **Tasks:**
  1. Introduce AST-level code mutation operators for Java source files.
  2. Replace string distance heuristics in `DarwinFitnessRanker` with automated unit test pass ratios and compiler diagnostic scores.
  3. Implement candidate recombination (crossover) across non-conflicting `KEPT` variants in `EvolutionTree`.
  4. Enforce strict independent verification boundaries for self-development tasks.

---

### HORIZON 4 — EXPLORE PLATFORM EXPANSION (Long-Term / Optional)
**Objective:** Explore distributed Darwin execution nodes and supervisor process hosting.

- **Tasks:**
  1. Expose `TaskContext` and `ADarwinEngine` execution endpoints via REST/gRPC for distributed worker node processing.
  2. Expand `EVOSupervisorServer` into a full AI process supervisor managing memory, permissions, and IPC.
  3. Investigate long-running cognitive state persistence across platform restarts.

---

## 2. PROPOSED FALSIFIABLE EXPERIMENTS

To resolve key architectural uncertainties before implementation, the following safe, falsifiable experiments are proposed:

### Experiment 1: Heuristic Fitness vs. Test-Driven Fitness in Variant Selection
- **Hypothesis:** Selecting candidate variants based on automated unit test pass ratios yields higher code correctness than heuristic LLM self-scoring.
- **Baseline:** `DarwinFitnessRanker` using semantic distance and LLM self-scores.
- **Independent Variable:** Objective test execution pass ratio from `mvn test`.
- **Measurable Outcome:** % of selected winning variants that pass final build verification without requiring iteration retries.
- **Falsification Condition:** If test-driven selection produces equal or lower first-pass build success rates than heuristic ranking, the hypothesis is rejected.

### Experiment 2: Impact of Retained Rejection Memory on Iteration Convergence
- **Hypothesis:** Injecting explicit rejected sibling strategies from prior iterations reduces duplicate candidate generation by >= 50%.
- **Baseline:** Variant generation without historical rejection prompts.
- **Independent Variable:** Active inclusion of `rejectedSiblings` context in `basePrompt`.
- **Measurable Outcome:** Count of duplicate or semantically identical candidate proposals generated across 5 iterations.
- **Falsification Condition:** If candidate duplication rates do not decrease by at least 50%, the hypothesis is rejected.
