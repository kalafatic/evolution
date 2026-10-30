# EVO Codebase Architecture Audit & Technical Assessment

**Date:** October 30, 2026
**Document ID:** `genome-docs/30102026/evo-codebase-architecture-audit.md`
**Scope:** Whole EVO System Architecture (Darwin Engine, Self-Development Loop, Server Platform)

---

## 1. Executive Summary

EVO is an evolutionary software development and AI process orchestration platform constructed on Java 21, Eclipse RCP, OSGi, EMF, Tycho, and SWT. This technical audit provides a baseline analysis of the repository's three core pillars:
1. **Darwin Engine**: The cognitive orchestration engine managing iterative prompts, trajectory variants, mutation lines, state transitions, and decision authority.
2. **Self-Development Loop**: The autonomous evolutionary loop acquiring canonical source code, preparing isolated build workspaces, running Maven/Tycho builds, exporting Eclipse RCP products, launching independent Supervisors, and running runtime verification.
3. **EVO Server**: The asynchronous execution server exposing REST, MCP tools, authorization (RBAC), and session management.

---

## 2. Platform Module Topology & Responsibilities

| OSGi Bundle / Maven Module | Primary Responsibilities | Dependencies |
| :--- | :--- | :--- |
| `eu.kalafatic.utils` | Low-level utilities, JSON repair, logging, security helpers. | None (Core) |
| `eu.kalafatic.evolution.model` | EMF Ecore domain model (`Orchestrator`, `Task`, `Iteration`, `ChatSession`). | EMF, Equinox |
| `eu.kalafatic.evolution.controller` | Darwin Engine, process adapters, self-dev tasks, mediation, reasoning. | `model`, `utils` |
| `eu.kalafatic.evolution.servers` | HTTP Server (NanoHTTPD), SQLite DB, RBAC Auth, MCP Dispatcher, REST APIs. | `controller`, `model` |
| `eu.kalafatic.evolution.supervisor` | Independent process supervisor, HTTP control API, monitoring loop. | `servers` |
| `eu.kalafatic.evolution.view` | Eclipse RCP workbench, MultiPageEditor, SWT UI pages, Browser JS bridges. | `controller`, `servers` |
| `eu.kalafatic.evolution.forge.*` | LLM model training, tokenizer, math engine, quantization, dataset pipeline. | `controller`, `model` |

---

## 3. Three Pillars Cohesion & Execution Flow

```
+-----------------------------------------------------------------------------------+
|                                 EVO WORKBENCH                                     |
|  (MultiPageEditor: IterationPage, ForgePage, AiCommanderPage, ArchitecturePage)  |
+----------------------------------------+------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|                            PILLAR 1: DARWIN ENGINE                                |
|  - Process & Instance Management (DarwinEngineRegistry)                          |
|  - Execution Modes: SIMPLE_CHAT, ASSISTED_CODING, MEDIATED, INTENT, SELF_DEV      |
|  - Cognitive Loop: GoalModel -> Mutation -> Variant Generation -> Selection       |
+----------------------------------------+------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|                         PILLAR 2: SELF-DEVELOPMENT LOOP                           |
|  - Isolated Workspace Preparation (Git check & copy)                              |
|  - Tycho/Maven Product Export (`mvn clean package -P export-product`)              |
|  - Supervisor Launch & Runtime Verification (`evo_ready.txt`, `/server/status`)    |
+----------------------------------------+------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|                           PILLAR 3: EVO SERVER PLATFORM                           |
|  - NanoHTTPD Server (REST APIs: `/server/*`, `/api/admin/*`, `/task`)              |
|  - Asynchronous Operation State Tracking & Durable Persistence                     |
|  - Model Context Protocol (MCP) Dispatcher & RBAC Authorization                   |
+-----------------------------------------------------------------------------------+
```

---

## 4. Key Cross-Pillar Findings & Architectural Integrity

1. **Unified State & Identity**:
   - `sessionId` acts as the universal correlation key across Darwin execution, process adapters, HTTP endpoints, and logging sinks (`SessionLogger`).
   - Every Darwin process registers its active engine instance in `DarwinEngineRegistry` keyed by `sessionId`.

2. **State Transition Authority**:
   - `OrchestrationState` and `IterationManager` own state transitions (`INIT` -> `ANALYZING` -> `EXECUTING` -> `VERIFYING` -> `WAITING_FOR_USER_DECISION` -> `DONE` / `FAILED`).
   - UI pages act purely as observers using `EvolutionProgressPublisher` events rather than mutating state directly.

3. **Error Handling & Diagnostic Truth**:
   - Process exit code success is never treated as proof of functional success; full HTTP status polling and readiness marker checks are performed prior to reporting task completion.
