# FINAL WORKSPACE BASELINE
State recorded before the final integration and verification phase

Date: 2026-11-17 (continuation of the same day's implementation)
Recording: read-only git inspection, no modifications

## 1. Repositories

| Repository | Branch | HEAD | Modified | Untracked | Conflicts | Deleted |
|---|---|---|---|---|---|---|
| evolution-121126 (EVO) | master | cbbfa121 | 64 | 56 | 0 | 0 |
| DCoder-121126 | master | f8e886e | 0 | 20 | 0 | 0 |
| tryst-121126 | master | 9ae5e07 | 41 | 30 | 0 | 0 |
| DStudio-121126 | master | df857a9 | 0 | 9 | 0 | 0 |
| evo-native-runtime-161126 | master | c5a962c | 1 | 16 | 0 | 0 |

No staged files in any repository (all changes are working-tree only).

## 2. Work classification (EVO, evolution-121126)

### Pre-existing uncommitted work (before this project; preserved, not modified further)
- Phase 6/7 family integration: TEIP docs + DTOs + tests, `controller/harness/*`,
  capability classes, KernelFactory/LlmRouter/DeveloperEngine/PortRegistry/
  ResourceManager modifications, `pom.xml`, feature.xml, supervisor files,
  GUIFactory. (≈ original 105-file set)

### Cognitive Services implementation (this project)
- servers: IdentityService, CognitiveScopes, ValidatedPrincipal,
  ApplicationIdentity/ApplicationAccess + repositories, TokenHasher,
  BearerTokenAuthenticator, ValidationPolicyService + validation model,
  JsonRpcDispatcher/AdminApiController/DatabaseManager extensions
- controller: memory model extensions + MemoryService, cognitive/ package
  (12 classes), CognitiveMcpToolProvider, McpDemoServerManager wiring,
  cognitive.html, RuntimeEventType additions, OllamaService.chatWithUsage
- enr: EnrInferenceEngine/EnrRuntime/EnrHttpServer metadata extension
- view: CognitiveAdminPage + MultiPageEditor registration
- controller.tests: 6 new cognitive test suites
- COGNITIVE-PLATFORM/: 12 implementation documents + this baseline

### DCode (DCoder-121126)
- DCodeApi ingest endpoint, SqliteSessionService metadata fix,
  ToolCallingAgentLoop platform-context section (all untracked files belong
  to the pre-existing scaffolding; these three files modified by this project)

### TRYST (tryst-121126)
- EvoMcpClient (new), AiContextManager overload (new; rest of modified files
  are pre-existing Phase 6/7 work)

### DStudio (DStudio-121126)
- EvoMcpClient (new), DStudioConfiguration evoMcp fields,
  DStudioConfigurationServiceImpl keys, TeipExecutionManager pre-step,
  teip MANIFEST export

### Generated/build artifacts (not source)
- target/ directories everywhere (existing, pre-dating this session)
- controller.tests/data/evolution.db modified by test runs (tests-bundle DB;
  tables are additive CREATE IF NOT EXISTS)

## 3. Databases

- Backups verified readable: `backups/2026-11-17-cognitive-baseline/`
  (evolution.db + tests-evolution.db, integrity ok, 2026-11-17 12:57).
- Live DBs (checked this phase): data/evolution.db, tests data/evolution.db,
  ~/.tryst/tryst.db, ~/.dstudio/dstudio.db, ~/.dcode/dcode.db — integrity ok.

## 4. Runtime state at baseline

- EVO: not running.
- Port 28080: held by PID 3763 (root), `open-webui serve --port 28080`
  (unrelated external application, python venv in ~/Projects/AI/ai_env).
- Port 28280 (EVO MCP): free.
- OpenCode recovery artifacts: untouched.

## 5. Previously documented limitations to resolve (from FINAL-IMPLEMENTATION-REPORT.md)

1. Darwin ContextBuilder hook not wired (mandatory gap).
2. Port 28080 conflict + no live runtime verification.
3. Live cross-repo E2E not run.
4. Direct tests missing for PersonalizationService, ProjectKnowledgeService,
   MemoryConsolidationService, KnowledgeExtractionService.
5. Admin UI runtime exercise pending.
