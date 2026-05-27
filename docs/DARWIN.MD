STABILIZE AND REFACTOR THE EVOLUTIONARY ITERATION/BRANCHING ARCHITECTURE.

The current EVO platform already contains the required features:
- iterative cognition
- Darwin branching
- sequential spawning
- manual/auto approval
- mediated mode
- self-development mode
- lineage persistence
- diversity validation
- checkpoint recovery

The problem is NOT missing functionality.
The problem is ARCHITECTURAL DISCONNECTION between components.

The current implementation still behaves like:
“ask the LLM for multiple variants”.

This is architecturally incorrect for small local models.

The correct architecture is:
the ORCHESTRATOR owns evolutionary planning,
while the LLM only materializes constrained local trajectories.

================================================================================
CORE ARCHITECTURAL GOAL
================================================================================

Refactor the system into a TRUE ITERATIVE EVOLUTIONARY KERNEL.

The system must evolve through:
- iterations
- branch blueprints
- survivor selection
- lineage mutation
- architectural divergence

NOT:
single-pass variant generation.

The final goal is:
a robust experimental AI evolution platform,
NOT a simple prompt-response generator.

================================================================================
CRITICAL REFACTOR
================================================================================

The current flow incorrectly does:

USER PROMPT
→ spawn multiple LLM variants
→ filter duplicates
→ synthetic fallback

This causes:
- semantic convergence
- only 2 surviving branches
- synthetic duplicate branches
- repeated architectural philosophies
- fake diversity
- non-evolutionary behavior

The system MUST instead work like this:

USER PROMPT
→ intent decomposition
→ divergence analysis
→ trajectory blueprint planning
→ sequential constrained spawning
→ diversity validation
→ user branch selection
→ lineage mutation
→ next iteration blueprint planning

================================================================================
REQUIRED ITERATIVE EVOLUTION MODEL
================================================================================

PHASE 1 — INTENT DECOMPOSITION

The orchestrator must analyze:
- implementation dimensions
- ambiguity points
- architectural axes
- execution depth
- stabilization needs
- extensibility potential
- runtime behavior
- integration surface

Example:
“create java class which can print text”

The system should detect possible evolutionary axes:
- console output
- file output
- logger integration
- reusable service
- validation
- append/overwrite strategy
- async output
- buffering
- testing/stabilization
- extensibility

This phase MUST NOT generate branches yet.

The purpose is:
construct evolutionary search space.

================================================================================
PHASE 2 — TRAJECTORY BLUEPRINT PLANNING
================================================================================

THIS IS THE MOST IMPORTANT REFACTOR.

The orchestrator must generate:
TRAJECTORY BLUEPRINTS.

NOT final implementations.

Each blueprint defines:
- engineering philosophy
- required characteristics
- forbidden overlaps
- mutation pressure
- architectural direction

Example:

----------------------------------------------------------------------------
BLUEPRINT A — DIRECT EXECUTION
----------------------------------------------------------------------------

Goal:
minimal executable implementation

Required:
- single class
- println
- main method

Forbidden:
- interfaces
- services
- factories
- layered architecture

----------------------------------------------------------------------------
BLUEPRINT B — FILE OUTPUT
----------------------------------------------------------------------------

Goal:
persistent file writer

Required:
- FileWriter
- append support
- configurable path

Forbidden:
- console-only execution
- unnecessary abstractions

----------------------------------------------------------------------------
BLUEPRINT C — STABILIZATION
----------------------------------------------------------------------------

Goal:
safe/reliable implementation

Required:
- validation
- exception handling
- tests

Forbidden:
- speculative abstractions
- premature architecture

----------------------------------------------------------------------------
BLUEPRINT D — REUSABLE SERVICE
----------------------------------------------------------------------------

Goal:
future extensibility

Required:
- Printer interface
- implementation separation
- reusable execution

Forbidden:
- hardcoded output

Blueprints MUST be structurally different BEFORE LLM generation.

Diversity must be PRE-SPAWN,
NOT post-filtered afterward.

================================================================================
PHASE 3 — SEQUENTIAL CONSTRAINED SPAWNING
================================================================================

The LLM must NO LONGER invent divergence itself.

Instead:
the orchestrator sequentially feeds:
- user goal
- blueprint
- previous branches
- forbidden overlaps
- rejected philosophies
- lineage history

The LLM should only MATERIALIZE the blueprint.

Example:

CALL 1:
Generate direct atomic implementation.

CALL 2:
Generate persistent file-output implementation.
Avoid:
- console-only execution
- direct println
- previous branch philosophy

CALL 3:
Generate stabilization-first implementation.
Avoid:
- service abstraction
- reusable architecture
- overlap with previous branches

CALL 4:
Generate reusable/extensible implementation.
Avoid:
- direct execution
- duplicated persistence logic

================================================================================
CRITICAL REQUIREMENT
================================================================================

Sequential spawning alone is NOT sufficient.

The orchestrator must PREDEFINE divergence space FIRST.

The LLM must NOT decide:
what makes branches different.

The orchestrator owns:
- divergence
- mutation pressure
- evolutionary topology
- lineage continuity

The LLM only generates:
localized constrained trajectories.

================================================================================
ITERATIVE EVOLUTION REQUIREMENT
================================================================================

The current system still behaves like:
single iteration + branch selection.

This is wrong.

The platform must evolve through MULTIPLE GENERATIONS.

Even simple prompts should evolve iteratively.

Example:

USER:
“create java class which can print text”

----------------------------------------------------------------------------
ITERATION 0
Question:
“What kind of output strategy should survive?”
----------------------------------------------------------------------------

Branches:
- console printer
- file printer
- logger printer
- reusable service

User selects:
file printer

----------------------------------------------------------------------------
ITERATION 1
Question:
“How should persistence behave?”
----------------------------------------------------------------------------

Branches:
- append mode
- overwrite mode
- buffered writing
- async writing

User selects:
append mode

----------------------------------------------------------------------------
ITERATION 2
Question:
“How should reliability evolve?”
----------------------------------------------------------------------------

Branches:
- retries
- validation
- safe recovery
- test-first stabilization

User selects:
safe recovery

NOW:
the system behaves like TRUE evolution.

NOT:
multi-response generation.

================================================================================
USER INTERACTION RULES
================================================================================

MANUAL MODE (default):
- user selects winning branch each iteration
- orchestration pauses between generations
- all branches displayed together in one UI response
- no auto-execution

AUTO-APPROVE MODE:
- system automatically selects highest-fitness trajectory
- iterations continue automatically
- no user blocking

STEP MODE:
- extremely granular execution
- waits for:
  - clarifications
  - execution approval
  - branch approval
  - implementation steps
  - mutations
  - recovery operations

Default mode:
manual evolutionary branch selection only.

================================================================================
DIVERSITY VALIDATION REFACTOR
================================================================================

The diversity analyzer currently operates too late.

Current behavior:
spawn → detect similarity → delete duplicates

This guarantees convergence.

Refactor it so:
branch diversity is validated AGAINST BLUEPRINTS,
not only semantic wording.

The analyzer must compare:
- architecture
- execution philosophy
- abstraction strategy
- persistence strategy
- runtime behavior
- stabilization philosophy
- integration depth

NOT:
text similarity alone.

================================================================================
REMOVE INVALID EVOLUTION SHORTCUTS
================================================================================

Remove or refactor logic that:
- fast-forwards simple tasks directly to final synthesis
- bypasses evolutionary generations
- collapses iterative cognition into atomic execution
- reduces branch counts automatically
- generates synthetic fallback too early

Simple prompts must STILL evolve iteratively.

The architecture goal is:
evolutionary exploration,
NOT minimal execution.

================================================================================
FINAL EXECUTION REQUIREMENT
================================================================================

After final branch survival:
- execute implementation
- propagate ALL changed/generated files into git workspace
- refresh workspace state
- expose generated artifacts for review/commit
- persist lineage/checkpoint state

Final response must include:
- generated files
- changed files
- winning lineage
- iteration history
- surviving architectural philosophy

================================================================================
MEDIATED MODE REQUIREMENT
================================================================================

Mediated mode must use the SAME iterative Darwin kernel.

Goal:
prepare high-quality context package for external reasoning models.

Flow:
- analyze repository/folder
- build metadata
- identify critical files
- summarize architecture
- evolve prompts iteratively
- select most relevant files
- generate final zip package

The final artifact is:
a curated prompt + relevant files zip,
NOT direct execution.

Mediated mode must reuse:
- iterations
- branch selection
- blueprint planning
- stabilization logic
- lineage persistence

NOT separate orchestration logic.

================================================================================
SELF-DEVELOPMENT MODE REQUIREMENT
================================================================================

Self-development mode must also use:
the SAME unified evolutionary kernel.

External supervisor may:
- rebuild project
- restart RCP
- switch git branches
- recover checkpoints
- continue lineage

BUT:
all cognition must still flow through:
the unified iterative Darwin kernel.

================================================================================
FINAL ARCHITECTURAL REQUIREMENT
================================================================================

The EVO platform is:
an experimental AI evolutionary cognition platform.

The primary goal is:
clean evolutionary architecture.

Correct outputs are a BYPRODUCT
of correct cognition architecture.

The orchestrator must become:
the owner of evolution itself.

The LLM must become:
a constrained local trajectory materializer.

This architectural distinction is CRITICAL.

Implement stabilization/refactor accordingly.