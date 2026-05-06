# Evolutionary Kernel Architecture – Summary

## 🧠 Core Concept

The system is no longer a multi-agent orchestration platform.
It is a **deterministic state-transition-driven kernel** with clearly separated responsibilities.

All functionality (chat, code assist, Darwin, self-dev) is implemented as **projections of the same state machine**.

---

## 🏗️ High-Level Architecture

```
UI / Supervisor / Tests
          ↓
     KernelFacade
          ↓
   IterationManager  (CONTROL PLANE)
          ↓
 ┌────────┼─────────┐
 ↓        ↓         ↓
Orchestrator   DarwinEngine   Verifier
(EXECUTION)    (INTELLIGENCE) (VALIDATION)
```

---

## ⚙️ Core Principles

### 1. Single Transition Authority

* `IterationManager` is the **only component allowed to change state**
* Enforced via `TransitionToken` + `SystemStateHolder`
* No other class may call `setState()` or equivalent

---

### 2. Deterministic State Machine

#### States:

```
INIT
ANALYZING
PLAN_LOCKED
EXECUTING
VERIFYING
MUTATING
DONE
FAILED
RECOVERING (optional)
```

#### Example Flow (Standard):

```
INIT → ANALYZING → PLAN_LOCKED → EXECUTING → VERIFYING → DONE
```

#### Example Flow (Darwin):

```
INIT → ANALYZING → PLAN_LOCKED → EXECUTING → VERIFYING → MUTATING → loop → DONE
```

---

### 3. Separation of Concerns

#### 🧭 Control Plane – `IterationManager`

* owns state transitions
* performs intent analysis and planning
* decides flow (DONE / MUTATING / FAILED)
* enforces guards and invariants

---

#### ⚙️ Execution Plane – `EvolutionOrchestrator`

* **blind executor**
* runs only when `state == EXECUTING`
* does not analyze, plan, or decide

---

#### 🧠 Intelligence Plane – `DarwinEngine`

* generates and evaluates variants
* no state awareness
* no control over execution or transitions

---

#### 🔍 Validation – `ValidatorAgent`

* combines Reviewer + Constraint checks
* pure function (input → output)
* does NOT influence control flow directly

---

#### 🖥️ Coordination – `SelfDevSupervisor`

* session lifecycle only (start/stop/restart)
* no AI calls
* no reasoning
* communicates with Kernel only

---

## 🔄 Mode Projections

All use cases are projections of the same kernel:

| Mode        | Behavior                            |
| ----------- | ----------------------------------- |
| Simple Chat | Single pass (no VERIFYING/MUTATING) |
| Code Assist | Includes VERIFYING                  |
| Darwin      | Full loop with MUTATING             |
| Self-Dev    | Darwin + Supervisor lifecycle       |

---

## 🔒 Execution Rules

1. **No execution outside EXECUTING state**
2. **No state transitions outside IterationManager**
3. **No direct Orchestrator usage (must go through Kernel)**
4. **No intelligence influencing control flow**

---

## 🧪 Testing Strategy

### Levels:

1. **Kernel Tests**

   * test `IterationManager` with mocks
   * verify transitions and decisions

2. **Execution Tests**

   * test `Orchestrator` independently

3. **Integration Tests**

   * full flow via `KernelFacade`

---

### Testing Principles:

* test **behavior**, not internal call order
* avoid mocking AI call sequences
* assert outcomes and state transitions

---

## 📦 Dependency Injection

All core components are injected:

```
IterationManager(
    AiService,
    Orchestrator,
    DarwinEngine,
    Verifier
)
```

No internal `new` calls for core services.

---

## 🧩 Key Benefits

* deterministic behavior
* no planning duplication
* clear authority boundaries
* testable in isolation
* resilient to LLM variability
* extensible (new modes = new projections)

---

## 🏁 Final Definition

The system is:

> **A deterministic evolutionary kernel where intelligence, execution, and control are strictly separated, and all behavior is enforced through a state-transition machine.**

---
