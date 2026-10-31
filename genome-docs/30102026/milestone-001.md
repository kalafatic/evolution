# Milestone 001: Darwin Engine Contract Completion & Lifecycle Alignment

**Date:** October 30, 2026
**Document ID:** `genome-docs/30102026/milestone-001.md`

---

## 1. Selected Functional Milestone

**Milestone 001:** Fleshing out and validating all core `ADarwinEngine` contract methods to establish a fully compliant, executable Darwin cognitive instance foundation.

---

## 2. Rationale

Before advancing to complex cross-pillar workflows (such as self-dev builds or model forging), the central cognitive execution engine (`ADarwinEngine`) must satisfy its contractual interfaces (`IDarwinEngine` and `IMutationContract`) without throwing `UnsupportedOperationException` or returning `null` stubs.

---

## 3. Acceptance Criteria

1. `ADarwinEngine` provides concrete, functional implementations for:
   - `getMode()`: returns platform type name.
   - `getCapabilityType()`: returns `CapabilityType.EVOLUTION`.
   - `getContext()`: returns active `TaskContext`.
   - `getActiveTrajectory()`: resolves active lineage trajectory from `TrajectoryMemory`.
   - `setActiveTrajectory()`: records trajectory into `TrajectoryMemory`.
   - `getEvaluator()`: returns initialized `Evaluator` bound to `projectRoot`.
   - `evaluateFitness()`: delegates to `Evaluator.evaluate()`.
   - `generateVariants()`: evaluates snapshot, memory, and pressure to generate proposal variants.
   - `runIteration()`: executes `runDarwinIteration()`.
   - `validateVariants()`: validates scores using explicit loop without stream mutation side-effects.
   - `executeWinner()`: executes selected variant through `executeWinner()`.
2. All 30 reactor modules build successfully via `./mvnw clean test`.
