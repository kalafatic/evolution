Recommended Refactor Order

This order minimizes architectural damage while steadily stabilizing the platform.

Do NOT jump directly into “smart AI” improvements first.

Your system already has enough intelligence concepts.
The problem is structural coupling.

PHASE 1 — Extract Signals From Decisions
Goal

Separate:

“what features think”
from
“what kernel decides”
1. Convert Evaluators Into Pure Signal Producers
Current Problem

Evaluators:

score
rank
influence activation
indirectly select winners

Example:

variant.setScore(...)

This leaks authority into evaluation.

Refactor

Create immutable signal objects:

ProposalSignal

Examples:

semantic quality
compile confidence
ambiguity risk
failure similarity
trajectory alignment

Evaluators ONLY emit signals.

No branch activation.
No rank mutation.
No winner selection.

Motivation

This is the foundational decoupling step.

Without this:
every future feature becomes hidden authority.

2. Create Central Signal Bus
Goal

All proposal intelligence flows through one channel.

Refactor

Create:

SignalBus

Responsibilities:

collect signals
attach proposal IDs
timestamp
aggregate history
route to Authority Controller

Signals become observable system state.

Motivation

Prevents:

direct evaluator coupling
hidden branch mutations
scattered decision logic

Equivalent to:

OS interrupt/event bus
PHASE 2 — Centralize Authority

This is your MOST important stabilization phase.

3. Create AuthorityController
Goal

One component owns all execution decisions.

Refactor

Move ALL of these out of DarwinFlow:

activation decisions
branch selection
clarification requests
auto-approval logic
ranking interpretation
user supervision routing

AuthorityController consumes:

proposal signals
user commands
policy config

and produces:

AuthorityDecision

Examples:

ACTIVATE
REJECT
REQUEST_CLARIFICATION
KEEP_AS_RUNNER_UP
EXECUTE
STOP
Motivation

This removes “decision leakage”.

Right now:
every subsystem partially decides things.

This is the root architectural instability.

4. Convert ActivationGate Into Recommendation Engine
Goal

ActivationGate should recommend.

Never decide.

Refactor

Replace:

ActivationGate.activate()

with:

ActivationRecommendation

Examples:

recommended proposal
confidence
ambiguity warning
clarification suggestion

AuthorityController interprets recommendations.

Motivation

Signals advise.
Authority decides.

Critical kernel invariant.

PHASE 3 — Simplify Orchestration
5. Shrink DarwinFlow Into Pure Orchestration
Goal

DarwinFlow becomes a coordinator only.

DarwinFlow SHOULD ONLY:
generate proposals
publish signals
wait for authority decision
execute approved proposal

Nothing more.

Remove From DarwinFlow
scoring logic
activation policy
ranking policy
clarification policy
branch authority
auto approval decisions
Motivation

Currently DarwinFlow is:

scheduler
evaluator
authority
orchestrator
executor

This causes exponential complexity growth.

PHASE 4 — Stabilize Proposal Lifecycle
6. Introduce Proposal Lifecycle State Machine
Goal

Make proposal evolution deterministic.

Refactor

Formal states:

CREATED
ANALYZING
SCORING
RECOMMENDED
APPROVED
ACTIVE
EXECUTING
VERIFIED
REJECTED
ARCHIVED

Only AuthorityController changes lifecycle state.

Motivation

Prevents:

random branch mutation
hidden activation
unclear proposal ownership

Equivalent to:

OS process lifecycle
PHASE 5 — Improve Small-LLM Robustness

Only AFTER architecture stabilizes.

7. Add Ambiguity Detection Signals
Goal

Handle tiny prompts robustly.

Example:

create java class which prints text
Refactor

Emit ambiguity signals:

unclear output target
unclear scope
unclear style
unclear runtime context

AuthorityController decides:

ask user?
continue?
generate alternatives?
Motivation

This solves simple prompts WITHOUT regex hardcoding.

This is architectural clarification handling.

8. Add Ranked Proposal Preservation
Goal

Keep second-best branches alive.

Refactor

AuthorityController can:

activate winner
preserve runner-up
archive low-confidence branches
Motivation

This matches your intended Darwin design:

evolutionary exploration
non-destructive refinement
PHASE 6 — Advanced Evolution

ONLY after stabilization.

9. Trajectory Learning

Learn:

successful branch patterns
failed mutation paths
user preference history

Signals only.

Never direct activation.

10. Adaptive Signal Weighting

Dynamically weight:

semantic quality
compile confidence
user preference
historical success

Still:
AuthorityController decides.

FINAL TARGET ARCHITECTURE
[Proposal Generators]
        ↓
[Signal Producers]
        ↓
====================
      Signal Bus
====================
        ↓
[Authority Controller]
        ↓
[Executors]
        ↓
[Verification]
The Most Important Rule
NEVER AGAIN ALLOW:
Evaluator -> Activation
Evaluator -> Execution
Gate -> Decision
Flow -> Policy

Only:

Signals -> Authority -> Execution
Biggest Expected Result

After Phase 1–3:

simple prompts improve dramatically
Darwin complexity becomes manageable
adding features becomes safe
local small LLMs become usable
user supervision becomes coherent
autonomous execution becomes deterministic

That is the real stabilization point of your platform.