# DSTUDIO INTEGRATION VERIFICATION

## 1. Implemented integration

- `EvoMcpClient` (eu.kalafatic.dstudio.teip.client): Jackson-based JSON-RPC
  client; contextGet, memoryStore, sessionCreate, contextRecord,
  recordOutcome, sessionHistory; graceful degradation.
- Configuration: `DStudioConfiguration` gains `evoMcpEndpoint` and
  `evoMcpToken` (properties `evo.mcp.endpoint`/`evo.mcp.token`; sysprops
  `dstudio.evo.mcp.endpoint`/`dstudio.evo.mcp.token`; env equivalents).
- Context pre-step: `TeipExecutionManager.augmentInstruction` performs a
  bounded context_get (3500 tokens, project/memory/continuity/decisions)
  and correlates a cognitive session before harness dispatch. Unconfigured
  or unreachable EVO → instruction passes through unchanged.

## 2. Live verification (EvoMcpClientLiveIntegrationTest, 3/3 PASS)

Executed against the RUNNING EVO MCP server with the DSTUDIO application
token:

- `liveContextGetReturnsPackage`: context_get returns a package with
  schemaVersion + non-negative token estimate.
- `liveMemoryStoreAndSessionCreate`: memory_store and session_create
  succeed against live EVO.
- `invalidTokenDegradesGracefully`: invalid token yields null (no throw).

Skips silently when EVO is not running.

## 3. Test fixes

- `DStudioConfiguration` gained two fields; three existing test classes
  (ConfigurationServiceTest, TeipApiEndpointTest, TeipOpenCodeEndToEndTest)
  were updated to the extended constructor.

## 4. Regression

- DStudio full reactor verify: 101/102 PASS. The one failure
  (`TeipOpenCodeEndToEndTest.externalClientDrivesRealOpenCodeThroughTeip`)
  is a live-OpenCode environment-dependent test: the harness reported
  COMPLETED but the external OpenCode agent did not produce the marker file
  (no configured LLM completed the edit). Not related to cognitive changes
  (the pre-step is inactive when evoMcpEndpoint is empty; the test config
  is empty). Documented as environment-dependent.

## 5. Authorization boundaries

- Verified live via the shared E2E: the DStudio token cannot read TRYST
  sessions or conversations (-32002), cannot record decisions
  (decision.write missing), and its spoofed userId is ignored.
