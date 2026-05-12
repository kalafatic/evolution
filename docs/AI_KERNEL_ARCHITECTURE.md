# AI Kernel Architecture
## Experimental Darwin-Orchestrated AI Platform

---

# 1. Vision

The platform is not designed as a traditional chatbot.

It is designed as a lightweight AI operating system kernel capable of:

- coordinating small local LLMs
- supporting manual and automatic decision workflows
- evolving multiple proposal branches
- evaluating proposals using independent subsystems
- remaining deterministic and stable
- scaling from simple prompts to research-grade exploration

The system prioritizes:
- robustness
- modularity
- deterministic orchestration
- branch exploration
- user-guided evolution

---

# 2. Core Philosophy

## Multiple systems may propose.
## Only one system may decide.

The architecture intentionally separates:

| Layer | Responsibility |
|---|---|
| Darwin | generate possibilities |
| Evaluators | compute signals |
| Gate | rank proposals |
| Kernel | decide winner |
| Executor | apply selected branch |

---

# 3. Operating System Analogy

The architecture behaves more like an operating system than a chatbot.

| AI Component | OS Equivalent |
|---|---|
| IterationManager | Kernel / Scheduler |
| Darwin Engine | Process Forker |
| BranchVariant | Process / Thread |
| Event Bus | Interrupt/System Bus |
| Evaluators | Hardware Sensors / Drivers |
| ActivationGate | Task Manager / Scoreboard |
| ActivationResolver | CPU Scheduler |
| Git Snapshot | Filesystem Snapshot |
| LLM | Coprocessor / External Device |

---

# 4. Kernel Rules

The Kernel is the ONLY decision authority.

Only the Kernel may:
- activate branches
- select winners
- schedule execution
- resolve conflicts

No other component may:
- activate proposals
- select winners
- implicitly trigger execution

---

# 5. Darwin Engine

Darwin is responsible only for exploration.

Responsibilities:
- generate branch variants
- mutate proposals
- preserve lineage
- explore alternatives

Darwin MUST NOT:
- rank variants
- activate variants
- select winners

---

# 6. Event Bus

The Event Bus is a propagation layer.

It distributes:
- telemetry
- metrics
- diagnostics
- scoring events

It MUST NOT:
- decide outcomes
- activate branches
- store winner state

---

# 7. Evaluator System

Evaluators are independent signal producers.

Each evaluator analyzes one dimension:
- correctness
- complexity
- performance
- semantic quality
- hallucination risk
- test pass rate

Evaluators output signals only.

Evaluators MUST NOT:
- activate branches
- select winners
- coordinate decisions

---

# 8. EvaluationSignal

EvaluationSignal is the standardized telemetry object.

It represents:
- score
- confidence
- diagnostics
- evaluator identity
- branch identity

Example:

```java
EvaluationSignal {
    String variantId;
    String evaluatorId;
    double score;
    double confidence;
    String explanation;
    SignalSeverity severity;
}
```

All evaluators publish signals through the Event Bus.

---

# 9. ActivationGate

ActivationGate is NOT an authority.

It behaves like a scoreboard:
- aggregates signals
- ranks variants
- explains ranking

It MUST NOT:
- activate branches
- enforce thresholds
- trigger execution

---

# 10. DecisionSnapshot

DecisionSnapshot is a temporary kernel-side aggregation object.

It contains:
- all signals for all variants
- rankings
- diagnostics
- branch metadata
- execution history

The Kernel uses this snapshot to make deterministic decisions.

---

# 11. ActivationResolver

ActivationResolver is the scheduling core.

Responsibilities:
- resolve final active branch
- apply deterministic policies
- process user overrides
- apply tie-breaking logic

Resolution order:
1. explicit user selection
2. highest ranked variant
3. lowest complexity tie-break
4. refinement if no acceptable branch exists

---

# 12. Deterministic Flow

```text
User Input
    ↓
Kernel
    ↓
Darwin Engine
    ↓
Branch Variants
    ↓
Event Bus
    ↓
Evaluators
    ↓
Evaluation Signals
    ↓
ActivationGate
    ↓
DecisionSnapshot
    ↓
ActivationResolver
    ↓
Executor
    ↓
Result
```

---

# 13. Design Goal

The architecture must:
- work with very small local LLMs
- remain useful with manual supervision
- scale toward larger autonomous systems
- avoid hardcoded prompt heuristics
- support evolutionary workflows
- remain deterministic and debuggable

---

# 14. Non-Goals

The architecture is NOT attempting to:
- simulate AGI
- replace deterministic orchestration
- create fully autonomous uncontrolled systems

The kernel must remain authoritative at all times.

---

# 15. Long-Term Direction

The system should evolve toward:
- pluggable evaluators
- pluggable ranking systems
- configurable scheduling policies
- distributed branch execution
- local-first AI orchestration
- AI-native operating system concepts