# IMPLEMENTED ARCHITECTURE
Cognitive Services — the architecture as actually implemented

## 1. Layout

```
eu.kalafatic.evolution.servers            (extended in place)
  service/IdentityService                 application identities + token lifecycle
  service/CognitiveScopes                 scope vocabulary + sanitization
  service/ValidatedPrincipal              authenticated principal (user, app, scopes)
  model/ApplicationIdentity|ApplicationAccess
  repository/ApplicationRepository|ApplicationAccessRepository
  security/TokenHasher                    SHA-256 token hashing + generation
  validation/ValidationPolicyService      central validation policy (rules/categories/
                                          modes/history/security-fixed rules)
  mcp/server/BearerTokenAuthenticator     app tokens + user session tokens
  mcp/server/JsonRpcDispatcher            (extended) authenticator hook, tool scopes,
                                          prompt authz, hidden protected tools
  database/DatabaseManager                (extended) 21 additive cognitive tables

eu.kalafatic.evolution.controller
  memory/                                 (extended) MemoryEntry + MemoryRelationship/
                                          MemoryVersion/MemoryValidity/
                                          MemoryInformationStatus; MemoryService
                                          versioning, validity lifecycle, relationships,
                                          FTS mirror, events
  cognitive/                              (new)
    CognitiveContextService               facade (getContext/searchContext/
                                          storeMemory/retractMemory/recordContext)
    ConversationHistoryService            JSONL transcript + SQL index + FTS5
    SessionContinuityService              registry, relationships, snapshots, restore,
                                          reconciliation
    SessionIdService                      canonical UUID ids
    UserContextService                    profile + explicit/inferred preferences
    PersonalizationService                precedence-enforced preference store
    ProjectKnowledgeService               components/decisions/issues/build/test/state
    ContextReconstructionService          the 17-stage pipeline
    ContextQualityService                 ContextAssessment
    TokenEstimationService                single token counter (chars/4)
    MemoryConsolidationService            async post-session consolidation
    KnowledgeExtractionService            deterministic fact/preference/decision extraction
    model/                                ContextRequest, ContextPackage, ContextItem,
                                          ContextSource, ContextAssessment,
                                          ConversationRecord, SessionDescriptor
  orchestration/mcp/CognitiveMcpToolProvider   thin MCP adapters + evo:// resources
  orchestration/mcp/McpDemoServerManager       (extended) 28280 wiring
  orchestration/cognitive.html                Cognitive Services admin HTML
  workflow/RuntimeEventType                   (extended) MEMORY_*, SESSION_*,
                                              CONTEXT_RECONSTRUCTED, etc.

eu.kalafatic.evolution.view
  editors/pages/CognitiveAdminPage       "Cognitive Services" editor page (bridge)
  editors/MultiPageEditor                 (extended) page registration

eu.kalafatic.evolution.enr
  EnrInferenceEngine                      (extended) EnrModel.contextLength(+known),
                                          EnrChatResponse.promptTokens/completionTokens
  EnrRuntime / EnrHttpServer              metadata propagation + serialization

eu.kalafatic.evolution.controller.manager.OllamaService
                                          chatWithUsage (prompt_eval_count/eval_count)

DCoder-121126
  plugins/eu.kalafatic.dcode.server       POST /api/v1/sessions/{id}/context ingest
  plugins/eu.kalafatic.dcode.persistence  metadata_json round-trip fix
  plugins/eu.kalafatic.dcode.harness      platform-context SYSTEM section in loop

tryst-121126
  eu.kalafatic.tryst.evo.integration.client.EvoMcpClient
  eu.kalafatic.tryst.domain.ai.AiContextManager  external context overload

DStudio-121126
  eu.kalafatic.dstudio.teip.client.EvoMcpClient
  eu.kalafatic.dstudio.core.config.DStudioConfiguration  evoMcpEndpoint/evoMcpToken
  eu.kalafatic.dstudio.teip.server.TeipExecutionManager  context pre-step
```

## 2. Call flows

### context_get via MCP (28280)

```
Client POST /mcp tools/call context_get + Bearer token
  DemoDocumentController → resolveExecutionContext (bearer extracted)
  JsonRpcDispatcher
    authorize(principal, MCP_TOOL, context_get, EXECUTE)   [AuthorizationService + audit]
    registerToolScope("context_get", context.get)          [scope enforcement]
    authenticateContext → BearerTokenAuthenticator → ValidatedPrincipal
  CognitiveMcpToolProvider → CognitiveContextService.getContext(request, principal)
    ContextReconstructionService.reconstruct
      session resolve (SessionContinuityService)
      user context (UserContextService + PersonalizationService)
      conversation history (ConversationHistoryService)
      memory (MemoryService.search + scope filters + conflicts + staleness)
      decisions/issues/project state (ProjectKnowledgeService)
      rank → dedup → budget → trim-to-budget
      ContextQualityService.assess → ContextAssessment
    runDomainValidation (ValidationPolicyService, default DISABLED)
  → ContextPackage (schemaVersion 1.0, sources, confidence, freshness, tokenEstimate, warnings)
```

### memory_store via MCP

```
tools/call memory_store → scope memory.write enforced
  → CognitiveContextService.storeMemory → MemoryService.addEntry
    version=1, JSON save, memory_index upsert + FTS rebuild, MemoryVersion JSONL append,
    MEMORY_STORED event (session bus when sessionRef present)
```

### Internal (native) path

Internal EVO components call `CognitiveContextService.getInstance().getContext(...)`
directly — never the MCP server over HTTP.

## 3. Decisions that deviate from the blueprint (documented)

1. **ContextPackage is a new DTO** (`cognitive/model/ContextPackage`) rather than
   extending `orchestration/ContextPackage`. The orchestration class is Darwin's
   prompt DTO; the cognitive package is the platform wire contract (schemaVersion
   1.0). Both coexist; Darwin consumers are unaffected.
2. **No OSGi service registrations** were added: the controller bundle has no
   activator and the codebase norm is singletons (`getInstance()`); all consumers
   live in the controller bundle or consume via MCP. Public packages are exported
   in MANIFEST.MF for future cross-bundle consumption.
3. **Resource providers parse URI query params** (`evo://sessions/history?sessionId=...`)
   because the existing ResourceRegistry.ResourceProvider contract only receives
   the URI string (no params object) — no protocol change.
4. **Conversation FTS uses explicit rebuild** (`INSERT INTO ..._fts(...) VALUES('rebuild')`)
   after writes because the existing DatabaseManager creates external-content FTS
   tables without update triggers. LIKE + transcript fallback guarantees search
   correctness when FTS is unavailable.
5. **The Darwin ContextBuilder additive hook was not wired**: reconstruction is
   available via the facade and MCP; Darwin prompt assembly is untouched this
   generation (empty-store behavior preserved).

## 4. What was deliberately NOT built

See KNOWN-LIMITATIONS.md: no graph DB, no embeddings, no LLM consolidation,
no new MCP server/port, no second SessionManager, no parallel memory platform.
