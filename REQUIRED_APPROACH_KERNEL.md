# 🔒 ARCHITECTURE ENFORCEMENT PREFIX (MANDATORY)

## CONTEXT

This system is a **deterministic state-transition evolutionary kernel**.

Architecture is NOT flexible. It is STRICT and MUST NOT degrade during bug fixing.

---

## ❗ PRIMARY RULE

You are NOT allowed to introduce shortcuts, hardcoding, or local fixes that violate architectural rules.

Every change MUST preserve the kernel design.

---

## 🧠 CORE INVARIANTS (NON-NEGOTIABLE)

1. **Single Transition Authority**

   * ONLY `IterationManager` may change system state
   * No exceptions

2. **Stateless Execution**

   * `EvolutionOrchestrator` is a blind executor
   * No planning, no analysis, no decisions

3. **Intelligence Isolation**

   * `DarwinEngine` and `ValidatorAgent` are pure
   * No state awareness
   * No control flow decisions

4. **No Bypass Paths**

   * All flows MUST go through Kernel (`IterationManager` / `KernelFacade`)
   * No direct use of Orchestrator or agents

5. **Deterministic State Machine**

   * All behavior must be explainable as:
     `STATE → TRANSITION → STATE`

---

## 🚫 FORBIDDEN FIXES

* Hardcoded values to “make tests pass”
* Adding logic into Orchestrator
* Adding state checks inside agents
* Bypassing IterationManager
* Fixing tests instead of fixing architecture (unless tests are wrong)
* Adding new “temporary” flags or modes

---

## ✅ REQUIRED APPROACH

Before implementing ANY fix:

1. **Identify root cause**

   * Is it architectural violation or test expectation?

2. **Explain fix in terms of architecture**

   * Which invariant was violated?
   * How is it restored?

3. **Implement minimal correct fix**

   * No side effects
   * No hidden behavior changes

---

## 🧪 SELF-CHECK (MANDATORY)

After your change, explicitly verify:

* Can any component change state outside IterationManager? (must be NO)
* Can Orchestrator execute outside EXECUTING? (must be NO)
* Did any agent gain control responsibility? (must be NO)
* Is behavior still explainable via state transitions? (must be YES)

---

## 📦 OUTPUT FORMAT

You MUST include:

1. Root cause analysis
2. Architectural invariant affected
3. Fix description (architecture-aligned)
4. Confirmation of all invariants

---

## 🧠 MINDSET

You are not fixing bugs.

You are maintaining a **deterministic evolutionary kernel** under strict constraints.

If a simple fix violates architecture, it is WRONG.

======================================================
FIX FOLLOWING FLOW: