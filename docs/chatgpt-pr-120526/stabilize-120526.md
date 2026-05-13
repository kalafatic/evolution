ok, next stabilization request is to ensure key use cases, modes according to my own goals. The main focus is on ITERATIVE/DARWIN + MEDIATED usage of limited local sources! Ensure following points are incorporated:

1.mode LOCAL-using local server like ollama+USES only locally managed models
2.mode PROXY-using remote server like ollama+USES their own models (:cloud)
3.mode HYBRID-using local server like ollama+USES locally managed models AND FORWARDS TO API based heavy models or cloud models managed models
4.mode REMOTE-USES API based heavy models or cloud models managed models
5.mode MEDIATED--using local server like ollama+USES only locally managed models to reformulate prompt+selects cca 10 files for context (java,md..) and then forwards to API based heavy models or cloud models managed models

6.behavior - ITERATIVE (default)-using selected model for multiple iterations and selecting best output for next iteration
7.behavior - DARWIN (default)-using selected model for multiple iterations and selecting best output for next iteration but also mutating prompt or approach based on previous output (like genetic algorithm)
8.behavior - STEPS-using selected model for multiple iterations but also showing steps and allowing user to intervene or select next step
9.behavior - SELF-DEVELOPMENT-using selected model for self development (using external supervisor for restarting RCP and providing feedback on iterations)
10.behavior - STEP mode-using selected model for multiple iterations but also showing steps and allowing user to intervene or select next step
11.behavior - AUTOAPPROVE-using selected model for multiple iterations and automatically approving output without user intervention




# TASK
Refactor and stabilize the orchestration platform around its REAL intended usage model.

IMPORTANT:
This is NOT a simplification rewrite.
This is NOT removal of Darwin/Iterative systems.
This is NOT conversion into a single-pass assistant.

The platform is intentionally:
- iterative
- Darwin/evolution capable
- user-guided
- local-model-first
- architecture/research oriented
- capable of operating with small local LLMs
- capable of mediated workflows with larger cloud LLMs

The goal is:
STABILIZE MODE/BEHAVIOR ARCHITECTURE
NOT add more experimental subsystems.

--------------------------------------------------
# PRIMARY DESIGN RULE
MODES define:
    WHERE models execute
    WHO owns models
    HOW requests are routed

BEHAVIORS define:
    HOW orchestration proceeds

These MUST be orthogonal.

DO NOT hardcode behavior based on mode names.

--------------------------------------------------
# REQUIRED PLATFORM MODES

## 1. LOCAL
Definition:
- Uses local inference server only
- Example:
  - local Ollama
  - llama.cpp
  - local vLLM
- Uses ONLY locally managed models

Rules:
- No cloud/API calls
- No forwarding
- No mediated cloud escalation

--------------------------------------------------

## 2. PROXY
Definition:
- Uses remote inference server
- Example:
  - remote Ollama
  - remote vLLM
  - LAN AI node

Rules:
- Models are managed by remote server
- Kernel does NOT own model lifecycle
- Treated as external compute node

--------------------------------------------------

## 3. HYBRID
Definition:
- Uses local models primarily
- May escalate selectively to external/cloud/API models

Rules:
- Local-first routing
- Escalation policy controlled by kernel
- Both local and remote providers coexist

--------------------------------------------------

## 4. REMOTE
Definition:
- Uses API/cloud managed models only

Examples:
- OpenAI
- Claude
- Gemini
- hosted APIs

Rules:
- No assumption of local models
- Kernel acts as orchestration client

--------------------------------------------------

## 5. MEDIATED
Definition:
- Local small model acts as mediator/orchestrator
- Local model:
  - reformulates prompts
  - prepares context
  - selects relevant files
  - compresses history
  - ranks context relevance
- Final request forwarded to heavy external model

Typical workflow:
1. local model analyzes request
2. local model selects ~10 relevant files
3. local model constructs optimized prompt
4. heavy model executes final reasoning

IMPORTANT:
MEDIATED is NOT a separate orchestration philosophy.
It is a routing/composition strategy.

MEDIATED may still use:
- ITERATIVE
- DARWIN
- STEPS
- AUTOAPPROVE
etc.

--------------------------------------------------
# REQUIRED BEHAVIORS

## ITERATIVE (DEFAULT)
Definition:
- multiple iterations
- next iteration informed by previous outputs
- user may select preferred result

Purpose:
Improve small model quality through refinement.

--------------------------------------------------

## DARWIN (DEFAULT)
Definition:
- iterative branching
- proposal mutation
- competing candidate outputs
- ranking/recombination possible

IMPORTANT:
Darwin is NOT autonomous chaos.
It is structured proposal evolution.

Rules:
- losing branches may survive as alternative trajectories
- runner-up branches may mutate into future winners
- branch history is preserved

--------------------------------------------------

## STEPS
Definition:
- exposes internal orchestration steps
- user may intervene
- user may select next action manually

Purpose:
debugging, supervision, research

--------------------------------------------------

## SELF-DEVELOPMENT
Definition:
- platform modifies/improves itself
- supervised externally
- may restart runtime
- may use watchdog/supervisor

Rules:
- external authority required
- kernel must never fully self-authorize

--------------------------------------------------

## AUTOAPPROVE
Definition:
- orchestration proceeds automatically
- kernel resolves proposal selection automatically

Rules:
- still uses ranking/signals
- still deterministic
- still auditable

--------------------------------------------------
# CRITICAL ARCHITECTURE RULES

## RULE 1 — AUTHORITY
Authority order:
1. User
2. Kernel policy
3. Recommendation systems
4. Raw scores

Scores NEVER directly activate branches.

--------------------------------------------------

## RULE 2 — EVALUATION
Evaluators produce SIGNALS only.

Evaluators MUST NOT:
- activate branches
- select winners
- mutate orchestration state

--------------------------------------------------

## RULE 3 — ACTIVATION
ActivationGate is recommendation-only.

ActivationGate MAY:
- rank
- filter
- recommend
- estimate confidence

ActivationGate MUST NOT:
- auto mutate orchestration state directly

--------------------------------------------------

## RULE 4 — FINAL DECISION
Introduce centralized:
DecisionResolver

Responsibilities:
- aggregate signals
- apply policy
- determine selected proposal
- generate audit trail

DecisionResolver becomes:
THE ONLY COMPONENT allowed to:
- activate variants
- select winners
- authorize continuation

--------------------------------------------------

## RULE 5 — EVENT BUS
EventBus becomes:
SYSTEM BACKPLANE

Subsystems communicate ONLY via:
- signals
- events
- telemetry

Avoid direct subsystem entanglement.

--------------------------------------------------

## RULE 6 — DARWIN STABILITY
Darwin must support:
- nonsense branches
- weak branches
- runner-up branches
- experimental mutations

DO NOT aggressively prune early.

Purpose:
preserve evolutionary diversity.

--------------------------------------------------

## RULE 7 — SIMPLE TASKS
Simple prompts MUST remain functional.

Example:
"create java class which can print text"

Expected behavior:
- ask minimal clarifications if ambiguity matters
- produce ranked options
- avoid over-evolution
- avoid architecture explosion

Kernel should:
scale orchestration depth to ambiguity/confidence.

--------------------------------------------------
# REQUIRED REFACTORING TASKS

1. Separate MODE from BEHAVIOR completely
2. Remove hidden mode-dependent behavior logic
3. Introduce DecisionResolver
4. Move all branch activation into DecisionResolver
5. Convert ActivationGate into pure recommendation engine
6. Standardize event/signal propagation
7. Add orchestration audit snapshots
8. Preserve Darwin branch lineage
9. Add orchestration depth scaling
10. Ensure simple prompts remain efficient

--------------------------------------------------
# IMPORTANT NON-GOALS

DO NOT:
- remove Darwin
- force single-pass execution
- hardcode regex solutions
- add more autonomous hidden logic
- bury orchestration inside evaluators
- make activation implicit

--------------------------------------------------
# EXPECTED OUTCOME

After refactor:
- architecture becomes understandable
- modes become stable
- behaviors become composable
- Darwin becomes controllable
- mediated workflows become first-class
- small local models become practical
- simple prompts remain usable
- advanced experimentation remains possible
- authority becomes deterministic and auditable

The platform should resemble:
an operating system kernel for orchestration,
NOT an autonomous black box agent.