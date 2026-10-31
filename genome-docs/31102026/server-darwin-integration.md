# Server & Darwin Engine Integration Architecture

**Date:** October 31, 2026
**Document ID:** `genome-docs/31102026/server-darwin-integration.md`

---

## 1. Remote Client to Darwin Integration Architecture

Remote clients interact with the Darwin Engine through the asynchronous execution services exposed by `EvolutionServer` and `OrchestratorServiceImpl`:

```
REMOTE HTTP CLIENT
  ↓
EVOLUTION SERVER (NanoHTTPD POST /task or /api/darwin/*)
  ↓
ORCHESTRATOR SERVICE (OrchestratorServiceImpl.execute(TaskRequest))
  ├── Registers TaskResult (Status: RUNNING)
  ├── Enqueues execution on SessionContainer.getExecutorService()
  └── Returns immediate TaskResult with sessionId / taskId
        ↓
DARWIN ENGINE (ADarwinEngine via KernelFacade)
  ├── Registered in DarwinEngineRegistry.register(sessionId, this)
  ├── Progressively updates TaskResult listeners (logs, waiting message)
  └── Persists execution results to SessionLogger & SessionRepository
        ↓
OBSERVATION & MONITORING (GET /task/:id, GET /server/conversation/:sessionId)
```
