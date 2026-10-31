# EVO System Integration & Cross-Component Architecture

**Date:** October 30, 2026
**Document ID:** `genome-docs/30102026/evo-system-architecture.md`

---

## 1. Subsystem Integration Topology

EVO integrates four primary subsystems:
1. **DARWIN ENGINE** (`ADarwinEngine`, `DarwinEngineRegistry`): Cognitive execution, goal modeling, trajectory variant generation, and parallel evaluation.
2. **SELF-DEVELOPMENT LOOP** (`SelfDevOrchestrator`, `SelfDevSupervisor`, `EvoRcpRuntime`): Independent process supervision, workspace cloning, Maven/Tycho builds, product exports, and runtime verification.
3. **EVO SERVER PLATFORM** (`EvolutionServer`, `AdminApiController`, `McpServer`, `JsonRpcDispatcher`): Asynchronous HTTP task dispatch, SQLite persistence, RBAC authorization, and MCP tool execution.
4. **EVO WORKBENCH UI** (`MultiPageEditor`, `AiCommanderPage`, `ForgePage`, `DevelopmentPage`, `IterationPage`): Observational SWT/Browser UI pages.

---

## 2. Cross-Component System Dependency Graph

```
USER / HTTP CLIENT
  ↓
EVOLUTION SERVER PLATFORM (EvolutionServer)
  ├── Auth & Authorization (AuthService, AuthorizationService, DatabaseManager)
  ├── Session Tracking (SessionRepository, SessionLogger)
  └── Task Submission (/task -> OrchestratorServiceImpl)
        ↓
DARWIN COGNITIVE CORE (ADarwinEngine)
  ├── Session Boundary (SessionManager, SessionContext)
  ├── Registry Registration (DarwinEngineRegistry)
  ├── Variant Spawning & Worktree Isolation (GitManager)
  └── Decision Gate (AuthorityController, awaitApproval)
        ↓
SELF-DEVELOPMENT SUPERVISOR (SelfDevSupervisor / EVOSupervisorServer)
  ├── Workspace Preparation (CopySourceTask)
  ├── Tycho Build & Export (BuildEvoTask, ExportEvoTask)
  ├── External Runtime Verification (EvoRcpVerifier, /server/status)
  └── Failure Recovery & Rollback (GitManager.rollback())
        ↓
OBSERVATIONAL WORKBENCH UI (MultiPageEditor / AEvoPage)
  ├── Event Subscription (RuntimeEventBus, EvolutionProgressPublisher)
  └── Timestamp Guards (sessionLastTimestamp, isStateStale)
```
