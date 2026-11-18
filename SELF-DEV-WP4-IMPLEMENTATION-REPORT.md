# SELF-DEV WP4 IMPLEMENTATION REPORT — Artifact Manifest and Provenance

Status: IMPLEMENTED + UNIT TESTED.
Date: 2026-11-18.

## 1. Changes implemented

### 1.1 Provenance model
- `BuildArtifact` extended with: runId, buildId, size, SHA-256, buildResult, testResult, exportResult (+ productId/os/arch via metadata). Existing constructor preserved for compatibility.
- New `ArtifactManifestStore` — the authoritative per-run artifact resolver, persisted at `<run>/export/artifact-manifest.json`:
  - `record(type, path, revision, buildId, buildResult, testResult, exportResult, metadata)` — computes size + SHA-256, assigns identity, persists JSON.
  - `resolve(type)` — returns the recorded artifact; throws when the file is stale or the checksum mismatches.
  - `verifyIntegrity(artifact)` — checksum verification for promoted/restored artifacts.
  - `rehome(source, newPath, exportResult)` — promotion preserves build/run identity + revision + metadata and re-verifies the copy.
  - `SelfDevContext.getArtifactManifestStore()` exposes the per-run store.

### 1.2 Producers wired
- `TychoEvoRcpBuilder.build` records the produced artifact with build result + test-policy marker.
- `TychoEvoRcpBuilder.exportProduct` records the exported artifact preserving the BUILD identity (buildId) where available.
- `MavenSupervisorBuilder.build` (both branches) records the supervisor jar through the store.
- `ExportSupervisorTask` (D-14) now materializes the supervisor jar into the export directory and records a real manifest entry (`exportResult=SUCCESS`, checksum); post-validation verifies the exported jar exists and is non-empty.

### 1.3 Consumers wired (D-09/D-10)
- `TychoEvoRcpBuilder.getArtifact` resolves through the manifest store first; falls back to the strict exact-product search (never first-match).
- `MavenSupervisorBuilder.getArtifact` resolves through the manifest store first; the deterministic fallback prefers the shaded versioned jar and REFUSES ambiguous candidates (no `jars[0]`).
- `EvoRcpRuntime.start` consumes only the manifest-recorded artifact; the export-dir `zips[0]` scan is removed; in-memory artifacts are accepted only when located inside the current run's export/runtime dirs; manifest validation failures abort startup.
- `StartEvoTask.findEvoRcpProductFallback` resolves through the manifest store; the `archives[0]` scan and the canonical-repo target lookup are removed.
- `EvoRcpDeployer.deploy` verifies the recorded SHA-256 before unzipping.
- `PromoteTask` copies the artifact and records the promoted identity via `rehome` (identity preserved, checksum re-verified).

## 2. Tests

New test class: `SelfDevArtifactManifestTest` (7 tests, all passing):
- record/resolve round trip (identity + persistence + reload from disk)
- checksum mismatch rejection
- stale manifest entry rejection
- rehome preserves identity + verifies copy (tamper detection)
- supervisor jar selection deterministic (versioned jar preferred; sources excluded; ambiguity refused)
- START_EVO does not scan the export dir for arbitrary ZIPs
- EXPORT_SUPERVISOR produces a real export record

## 3. Build and test execution

- Controller bundle compile: BUILD SUCCESS.
- `SelfDevArtifactManifestTest` 7/7 PASS; `SelfDevBuildPolicyTest` 7/7 PASS; `SelfDevResourceTruthTest` 6/6 PASS; `SelfDevTaskGraphGateTest` 10/10 PASS; `SelfDevTaskArchitectureTest` 7/7 PASS; `SelfDevResourceManagerIntegrationTest` 3/3 PASS.

## 4. Remaining environment-dependent failures (unchanged from WP1/WP2 baseline)

- `SelfDevPipelineHardeningTest` (4F): pre-existing — supervisor EMF config, repo target jar, RollbackTask false-success (WP10), live-supervisor ping flakiness (WP5).
- `SelfDevStandalonePipelineTest` (3F): pre-existing — deploy fixture lacks configuration/config.ini, stale Git-root expectation, overridden-run artifact.

## 5. Defect register updates

- D-09 FIXED (artifact manifest with runId/buildId/size/SHA-256/revision/timestamp/product/os/arch + build/test/export results).
- D-10 FIXED (single authoritative resolver; first-match selection removed; checksum verification at deploy/launch).
- D-12 FIXED (BUILD and EXPORT record the same artifact chain through the manifest store; export preserves build identity).
- D-14 FIXED (EXPORT_SUPERVISOR produces a real export record).
- D-43 PARTIALLY_FIXED (promotion deterministic + integrity verified; rollback known-good selection completes in WP10).
