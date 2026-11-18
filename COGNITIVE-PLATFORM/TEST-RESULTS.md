# TEST RESULTS
Cognitive Services — actual executed test results

## 1. New test suites (eu.kalafatic.evolution.controller.tests)

Command: `./mvnw -o -pl eu.kalafatic.evolution.controller.tests -am
-Dtycho.localArtifacts=ignore -Dcheckstyle.skip=true -Dtest=... verify`
(Java 21, Tycho surefire). Executed 2026-11-17.

| Suite | Tests | Result |
|---|---|---|
| CognitiveMemoryServiceTest | 9 | 9/9 PASS |
| ConversationHistoryServiceTest | 4 | 4/4 PASS |
| SessionContinuityServiceTest | 6 | 6/6 PASS |
| ContextReconstructionTest | 6 | 6/6 PASS |
| CognitiveSecurityTest | 8 | 8/8 PASS |
| CognitiveMcpSecurityTest | 6 | 6/6 PASS |
| **Total** | **39** | **39/39 PASS** |

### Coverage by spec requirement

- Security: invalid token ✓, revoked token ✓, unknown application ✓,
  invalid scopes sanitized ✓, missing scope → -32002 ✓, missing token →
  -32001 ✓, spoofed userId rejected ✓, anonymous tool hiding ✓, security
  rules fixed ✓, audit rows generated (dispatcher + IdentityService) ✓.
- Memory: store ✓, retrieve ✓, search ✓, update/versioning ✓, provenance
  (version history) ✓, retract-never-delete ✓, supersede ✓, contradictions ✓,
  legacy JSON backward compatibility ✓.
- Conversation history: message/tool call/tool result/decision records ✓,
  search ✓, service-restart recovery ✓, compaction ✓.
- Context: intent relevance ✓, project isolation ✓, user isolation vs
  spoofed identity ✓, conflicts surfaced ✓, budget enforcement ✓,
  provenance + assessment ✓.
- Validation: DISABLED skip ✓, WARN/STRICT mode switching ✓, STRICT blocks ✓,
  persistence (history + config tables) ✓, security-fixed invariants ✓.
- MCP: authentication ✓, authorization ✓, correct error codes ✓, cognitive
  tools registered and executable ✓.

## 2. Regression anchors

| Suite | Result |
|---|---|
| UserMemorySubsystemTest | 4/4 PASS |
| MemoryMenuIntegrationTest | 1/1 PASS |
| ContextBuilderTest | 3/3 PASS |
| AttachmentInjectionTest | 6/6 PASS |
| ConversationOutputControllerTest | 6/6 PASS |
| CheckpointDeterminismTest | 1/1 PASS |
| DreamExperienceSelectionAndContextTest | 3/5 FAIL — **pre-existing** (listed red in PHASE-6-TEST-REPORT.md §unit-level red tests; unrelated to cognitive changes) |
| ContextualHelpServiceTest / CanonicalEvoRepositoryPreflightTest | fail — **pre-existing** (documented in PHASE-7-WP-7.10-REGRESSION-REPORT.md §3 as pre-existing/environment-dependent) |

## 3. Build verification

| Project | Command | Result |
|---|---|---|
| EVO servers/controller/enr/view (+deps) | `./mvnw -o -pl ... -am package` | PASS |
| EVO full reactor (21 modules) | `./mvnw -o -DskipTests package` | PASS for all code bundles; `eu.kalafatic.evolution.repository` product materialization fails for the win32 product only — offline p2 repository lacks Windows-specific artifacts (`httpclient5-win`, `ecf...win32`); unrelated to source changes |
| DCode (persistence/server/harness) | `mvn -o -pl ... -am package` (Maven 3.9.12) | PASS |
| TRYST (evo.integration, domain) | `mvn -o -pl ... -am package` | PASS |
| DStudio (full reactor) | `mvn -o -DskipTests package` | PASS |

## 4. NOT yet executed (honest status)

- Live runtime verification (EVO MCP on 28280 with curl/JSON-RPC): not run —
  EVO runtime startup and the running open-webui (port 28080) conflict are
  environment conditions handled outside this session; the dispatcher path is
  covered by in-process tests.
- TRYST/DStudio/DCode live E2E against a running EVO: not run (EVO not
  running in this session); client code is compile-verified with graceful
  degradation built in.
- Dedicated unit tests for MemoryConsolidationService,
  KnowledgeExtractionService, PersonalizationService precedence matrix and
  ProjectKnowledgeService: pending (see KNOWN-LIMITATIONS.md).
