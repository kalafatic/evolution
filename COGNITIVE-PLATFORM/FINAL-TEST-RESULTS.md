# FINAL TEST RESULTS
All suites executed during the final integration and verification phase.

## 1. EVO (eu.kalafatic.evolution.controller.tests) — Java 21 / Tycho, executed

| Suite | Tests | Result |
|---|---|---|
| CognitiveMemoryServiceTest | 9 | 9/9 PASS |
| ConversationHistoryServiceTest | 4 | 4/4 PASS |
| SessionContinuityServiceTest | 6 | 6/6 PASS |
| ContextReconstructionTest | 7 | 7/7 PASS |
| CognitiveSecurityTest | 8 | 8/8 PASS |
| CognitiveMcpSecurityTest | 6 | 6/6 PASS |
| DarwinCognitiveIntegrationTest | 8 | 8/8 PASS |
| CognitiveRuntimeDefectRegressionTest | 4 | 4/4 PASS |
| CognitivePlatformServicesTest | 9 | 9/9 PASS |
| EnrHttpServerTest | 12 | 12/12 PASS (incl. new metadata wire assertions) |
| EnrRuntimeTest | 9 | 9/9 PASS |
| ContextBuilderTest (regression) | 3 | 3/3 PASS |
| UserMemorySubsystemTest (regression) | 4 | 4/4 PASS |
| MemoryMenuIntegrationTest (regression) | 1 | 1/1 PASS |
| ConversationOutputControllerTest (regression) | 6 | 6/6 PASS |
| CheckpointDeterminismTest (regression) | 1 | 1/1 PASS |
| AttachmentInjectionTest (regression) | 6 | 6/6 PASS |
| **Total** | **103** | **103/103 PASS** |

## 2. Live runtime verification (running EVO product)

- MCP runtime matrix: 41/41 PASS (protocol, auth matrix, 24 tools executed,
  resources, error envelopes, audit).
- Cross-application E2E scenario: 20/20 PASS.
- Persistence-after-restart: memory/decisions/preferences/sessions re-read
  after a clean kill + restart.
- Latency measurements: see PERFORMANCE-AND-RELIABILITY.md.

## 3. Sibling repositories

| Repository | Suite | Result |
|---|---|---|
| DCode | reactor tests (persistence/server/harness/tests in build) | 128 tests, 0 failures, 0 errors, 3 skipped (live-LLM) |
| DCode | PlatformContextIngestPersistenceTest (new) | 2/2 PASS |
| TRYST | full reactor verify | 354 tests, 0 failures, 0 errors |
| TRYST | EvoMcpClientLiveIntegrationTest (new, live EVO) | 4/4 PASS |
| DStudio | full reactor verify | 101/102 PASS; 1 environment-dependent failure (live OpenCode E2E without a working LLM edit) — unrelated to cognitive changes |
| DStudio | EvoMcpClientLiveIntegrationTest (new, live EVO) | 3/3 PASS |

## 4. Builds

- EVO: full reactor build PASS (all code bundles, Java 21/Tycho); product
  materialization PASS online (linux + win32 + headless products built).
- DCode, TRYST, DStudio: PASS (Maven 3.9.12, offline).

## 5. Pre-existing failures (unchanged, unrelated)

- EVO: DreamExperienceSelectionAndContextTest, ContextualHelpServiceTest,
  CanonicalEvoRepositoryPreflightTest — documented as pre-existing in
  PHASE-6/PHASE-7 reports; unchanged.
- DStudio: TeipOpenCodeEndToEndTest — live-environment dependent (see above).

## 6. Test fixes made in this phase (test code only)

- EnrHttpServerTest FakeEngine updated to the extended ENR records
  (contextLength/promptTokens).
- DStudio ConfigurationServiceTest / TeipApiEndpointTest /
  TeipOpenCodeEndToEndTest updated to the extended DStudioConfiguration
  constructor.
