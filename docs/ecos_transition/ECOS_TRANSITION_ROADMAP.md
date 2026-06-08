# ECOS Transition Roadmap

## Purpose

This roadmap describes how EVO evolves into ECOS.

The objective is controlled architectural evolution.

Not a rewrite.

Not a replacement.

Not a disruptive redesign.

Every phase must preserve functionality while increasing architectural consistency.

---

# Current State

The platform already contains many ECOS concepts:

* Darwin iterations
* lineage tracking
* trajectory management
* evolution graphs
* pressure analysis
* mediated workflows
* self-development workflows
* supervisors
* checkpoints

However these concepts currently coexist with:

* proposal generation
* branch-centric logic
* duplicated workflows
* mode-specific behavior

The roadmap gradually unifies these concepts.

---

# Phase A

## Discovery And Mapping

Objective:

Understand existing architecture.

Deliverables:

* architecture inventory
* component ownership map
* dependency map
* duplication analysis

No major refactoring.

No behavior changes.

Success:

Clear understanding of current state.

---

# Phase B

## Architectural Clarification

Objective:

Identify core evolutionary abstractions.

Document:

* Artifact
* Lineage
* Mutation
* Evaluation
* Pressure
* Selection

Map existing implementations to these abstractions.

Success:

Common vocabulary exists.

---

# Phase C

## Adapter Introduction

Objective:

Reduce risk.

Create adapters around legacy structures.

Examples:

BranchVariantAdapter

TrajectoryAdapter

MediationAdapter

Adapters allow coexistence of old and new models.

Success:

Migration can occur incrementally.

---

# Phase D

## Kernel Generalization

Objective:

Reduce artifact-specific assumptions.

Review:

* Darwin logic
* mutation logic
* evaluation logic
* persistence logic

Move artifact-specific behavior into strategies.

Success:

Kernel becomes increasingly generic.

---

# Phase E

## Mode Alignment

Objective:

Treat modes as environments.

Darwin Mode
Mediated Mode
Self Development Mode

must share evolutionary infrastructure.

Reduce duplicated cognition.

Preserve specialized execution environments.

Success:

Shared kernel ownership.

---

# Phase F

## Lineage Consolidation

Objective:

Strengthen lineage-centric evolution.

Reduce dependence on:

* proposal collections
* variant inventories
* branch trees

Increase emphasis on:

* surviving lineages
* mutation history
* evolutionary continuity

Success:

Lineages become primary persistent structures.

---

# Phase G

## Pressure-Centric Evolution

Objective:

Make pressure the primary evolutionary driver.

Prefer:

What remains unresolved?

Over:

What other ideas exist?

Success:

Evolution becomes guided by pressure resolution.

---

# Phase H

## Self-Evolution Stabilization

Objective:

Strengthen Self Development Mode.

Improve:

* Git integration
* checkpoint recovery
* supervisor coordination
* rollback safety
* iteration reliability

Success:

Stable self-improvement environment.

---

# Phase I

## Mediated Intelligence Stabilization

Objective:

Strengthen mediation package evolution.

Improve:

* context selection
* prompt evolution
* architecture summarization
* package quality

Success:

High quality mediation packages generated through iterative evolution.

---

# Phase J

## Experimental Cognitive Expansion

Only after previous phases stabilize.

Potential future artifacts:

* goals
* plans
* reasoning structures
* memory structures
* cognitive strategies

No implementation commitment.

Research phase only.

---

# Roadmap Rules

Every phase must:

* preserve working functionality
* reduce architectural duplication
* increase generality
* improve reuse
* strengthen lineage consistency

Avoid:

* large rewrites
* destructive migrations
* architecture resets

The platform itself should evolve using the same evolutionary principles that govern its artifacts.

---

# Final Destination

EVO gradually converges toward:

ECOS

A generalized evolutionary cognition platform built around:

Artifact
→
Mutation
→
Evaluation
→
Selection
→
Lineage
→
Pressure

with Darwin, Mediated, Self Development, and future environments operating on the same evolutionary foundation.
