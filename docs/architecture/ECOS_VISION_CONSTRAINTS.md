# ECOS VISION CONSTRAINTS

## Mandatory Architectural Rules For All Future Development

This document defines the long-term architectural direction of ECOS.

Every implementation task, bug fix, refactor, optimization, and feature addition must be evaluated against these constraints.

The purpose of this document is to prevent architectural drift, local optimizations, hardcoded shortcuts, and accidental degradation of the evolutionary cognition vision.

---

# PRIMARY VISION

ECOS is an experimental Evolutionary Cognition Operating System.

It is NOT:

* a coding assistant
* a prompt optimizer
* a workflow engine
* a self-improvement script
* a collection of AI tools

Those are merely current use cases.

The actual objective is:

A general-purpose evolutionary cognition platform capable of evolving arbitrary artifacts through iterative variation, evaluation, selection, inheritance, and pressure-driven refinement.

---

# CONSTRAINT 1

## EVERYTHING IS EVOLUTION

Every major capability must ultimately be expressible through:

* artifact
* lineage
* mutation
* evaluation
* selection
* pressure

If a new feature introduces an entirely separate optimization process, explain why it cannot be represented using existing evolutionary primitives.

---

# CONSTRAINT 2

## ONE EVOLUTION KERNEL

There must be exactly one primary evolutionary engine.

Forbidden:

* Darwin engine
* Mediated engine
* SelfDev engine

as independent evolutionary implementations.

Allowed:

EvolutionKernel

with specialized environments.

Modes may configure evolution.

Modes must not replace evolution.

---

# CONSTRAINT 3

## MODES ARE ENVIRONMENTS

Modes represent specialized execution environments.

Examples:

Darwin
Mediated
SelfDev
Autonomous

A mode may provide:

* artifact type
* evaluation strategy
* mutation strategy
* execution environment

A mode may NOT introduce an independent cognition model.

---

# CONSTRAINT 4

## SURVIVING LINEAGE IS PRIMARY

The primary cognitive artifact is:

SurvivingLineage

The system should evolve descendants of surviving artifacts.

Avoid architectures centered around:

* proposal collections
* option trees
* variant inventories
* disconnected alternatives

Alternative candidates exist only to determine survivors.

The lineage is what persists.

---

# CONSTRAINT 5

## ARTIFACT AGNOSTIC DESIGN

The kernel must never become specialized for:

* source code
* prompts
* mediation packages
* Java
* Git

Those are artifact types.

The kernel evolves artifacts.

Not implementations.

Future artifact types must be introducible without changing kernel behavior.

---

# CONSTRAINT 6

## PRESSURE DRIVES EVOLUTION

Evolution is driven by pressure.

Not creativity.

Not random exploration.

Not model inspiration.

Examples:

* correctness
* simplicity
* maintainability
* context quality
* reliability
* signal-to-noise ratio
* user preference

Whenever possible, improvements should emerge from pressure analysis.

---

# CONSTRAINT 7

## ORCHESTRATOR OWNS COGNITION

The orchestrator owns:

* lineage control
* recursion
* mutation planning
* pressure propagation
* convergence
* finalization

The model provides candidate materialization.

The model must never determine:

* convergence
* recursion depth
* finalization
* architecture direction

---

# CONSTRAINT 8

## NO MODE-SPECIFIC SHORTCUTS

When fixing a bug:

DO NOT implement:

if(mode == MEDIATED)

if(mode == SELFDEV)

if(mode == X)

unless absolutely unavoidable.

Prefer:

* strategy injection
* evaluator extension
* mutation extension
* artifact specialization
* configuration

Mode-specific conditionals are architectural debt.

Every new mode conditional must be justified.

---

# CONSTRAINT 9

## NO HARD-CODED KNOWLEDGE

Avoid embedding:

* project-specific assumptions
* language-specific assumptions
* workflow-specific assumptions

inside core evolutionary components.

Domain knowledge belongs in plugins, strategies, or environments.

Never in the kernel.

---

# CONSTRAINT 10

## GENERALIZATION OVER PATCHING

When a bug appears:

First ask:

"Which abstraction is missing?"

Before asking:

"What conditional fixes this bug?"

The preferred solution is architectural generalization.

The last resort is a special-case workaround.

---

# CONSTRAINT 11

## EVOLUTION OVER PIPELINES

Avoid converting cognition into fixed pipelines.

Bad:

Analyze
→ Generate
→ Evaluate
→ Done

Preferred:

Analyze
→ Mutate
→ Evaluate
→ Survive
→ Mutate
→ Evaluate
→ Survive
→ Stabilize

The system should remain evolutionary.

Not procedural.

---

# CONSTRAINT 12

## ADAPTERS BEFORE REWRITES

When introducing new abstractions:

Prefer:

Adapter
→ Migration
→ Replacement

Avoid:

Delete
→ Rewrite
→ Hope

Evolutionary transition is preferred over disruptive replacement.

---

# CONSTRAINT 13

## SELF DEVELOPMENT IS A SPECIAL ENVIRONMENT

Git is not cognition.

Builds are not cognition.

Tests are not cognition.

Restarts are not cognition.

Supervisors are not cognition.

They are execution infrastructure around the Evolution Kernel.

Do not move evolutionary logic into infrastructure components.

---

# CONSTRAINT 14

## MEDIATED MODE IS EVOLUTION OF UNDERSTANDING

Mediated Mode exists to evolve:

* prompts
* context
* summaries
* instructions
* understanding

It should reuse evolutionary mechanisms rather than introducing a separate mediation engine.

---

# CONSTRAINT 15

## FUTURE AGENT CAPABILITY

Any architectural decision that prevents future evolution of:

* plans
* goals
* reasoning
* memory
* cognition

must be considered suspect.

The architecture should remain open to future cognitive evolution.

---

# REQUIRED REVIEW BEFORE EVERY TASK

Before implementing any change:

1. Does this increase architectural generality?
2. Does this introduce mode-specific logic?
3. Does this duplicate existing evolutionary behavior?
4. Does this move cognition out of the kernel?
5. Does this create future architectural debt?
6. Can this be represented through lineage, mutation, evaluation, or pressure?

If the answer reveals a conflict with this document:

STOP

Document the conflict before implementation.

---

# FINAL RULE

Short-term correctness must never silently override long-term architectural direction.

If a local fix conflicts with the ECOS vision:

Document the compromise.

Make the debt visible.

Never hide architectural shortcuts inside production code.
