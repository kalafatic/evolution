# APPLICATION INTEGRATION
How EVO-family applications consume EVO Cognitive Services

## Principles

- EVO owns the cognitive infrastructure; MCP on 28280 is the external contract.
- Consumers are wire-only (JSON-RPC over HTTP + bearer). No EVO classes are
  imported by sibling applications.
- Every consumer degrades gracefully when EVO is unavailable: local pipelines
  remain the fallback and are unchanged.

## 1. DCode (DCoder-121126)

- **Context ingestion**: `POST /api/v1/sessions/{id}/context` with
  `{contextId, schemaVersion, sections, tokenEstimate}`. Persisted as a SYSTEM
  message (`{"source":"EVO_COGNITIVE_SERVICES",...}`) so it survives restart.
- **Loop integration**: `ToolCallingAgentLoop` extracts ingested platform
  contexts into a dedicated "Platform context (EVO)" system section and
  excludes them from the history summary window.
- **Persistence fix**: `SqliteSessionService.mapSession` now round-trips
  `metadata_json` (previously dropped).
- **ENR budget chain**: ENR now returns `promptTokens`/`completionTokens` per
  chat response and `contextLength` (+`contextLengthKnown`) per model; DCode's
  existing defensive reads become meaningful.
- Verified: builds (persistence/server/harness bundles). Live E2E pending.

## 2. TRYST (tryst-121126)

- **`EvoMcpClient`** (`eu.kalafatic.tryst.evo.integration.client`): JSON-RPC 2.0
  client following the TeipClient convention (hand-rolled JSON, Java 21
  HttpClient, bearer header, retries, graceful degradation). Methods:
  `contextGet`, `memoryStore`, `sessionCreate`, `contextRecord`, `recordOutcome`.
- **Context merge point**: `AiContextManager.assembleConversationContext(...)`
  gains an optional external-context overload: the EVO section is bounded
  (12k chars) and the local history window shrinks first so the total stays
  inside the existing budget. Domain stays EVO-agnostic (plain string section).
- **Correlation**: `sessionCreate(kind=TASK, correlationId=<tryst session id>)`
  returns the EVO cognitive session id to store on the TRYST session row.
- **Recording**: after task completion, `recordOutcome` stores an EPISODE
  memory + context snapshot asynchronously; failures never block the pipeline.
- Verified: bundles build. Live E2E pending (requires a running EVO on 28280).

## 3. DStudio (DStudio-121126)

- **`EvoMcpClient`** (`eu.kalafatic.dstudio.teip.client`): Jackson-based
  JSON-RPC client with the same degradation policy.
- **Configuration**: `DStudioConfiguration` gains `evoMcpEndpoint` and
  `evoMcpToken` (properties `evo.mcp.endpoint`/`evo.mcp.token`; sysprops
  `dstudio.evo.mcp.endpoint`/`dstudio.evo.mcp.token`; env equivalents).
- **Context assembly point**: `TeipExecutionManager.augmentInstruction`
  performs a bounded `context_get` (project/memory/continuity/decisions,
  3500 tokens) before harness dispatch and correlates a cognitive session.
  Unconfigured or unreachable EVO → instruction passes through unchanged.
- **Identity (Phase 1)**: application token model; per-user identity deferred.
- Verified: full reactor builds. Live E2E pending.

## 4. ENR (eu.kalafatic.evolution.enr)

- ENR remains stateless. Additive metadata only: usage counters per response
  (Ollama `prompt_eval_count`/`eval_count` via `OllamaService.chatWithUsage`),
  per-model `contextLength` (conservative default 4096 with
  `contextLengthKnown=false` when the backend does not report it; 0 for
  native/llama-cpp artifacts). No new persistence in ENR.

## 5. Cross-application isolation (enforced in EVO)

- Every token binds (application, user, scopes). Sessions and memory are tagged
  with the principal's application/user; other apps see them only with
  `*.any` scopes (admin-only by default).
- DStudio's application token cannot read TRYST sessions; TRYST's cannot read
  DStudio executions' data — enforced at the EVO service layer, not just the
  wire layer.
