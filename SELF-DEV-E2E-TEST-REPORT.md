# SELF-DEV E2E TEST REPORT (WP12)

Status: EXECUTED. 8/8 scenario/fault tests passing. Zero real Git mutations. Zero leaked child processes.
Date: 2026-11-18.

## 1. Test strategy

The end-to-end scenarios use a **real child process stand-in**: a small Java HTTP stub (compiled on the fly with the test JDK) wrapped as the product executable inside a layout-valid product ZIP. This exercises the REAL machinery end-to-end — artifact manifest, checksum verification, deployment (unzip + layout validation), process launch, persistent ownership records, HTTP/OSGi readiness verification, identity-verified STOP, rollback and leak detection — without a full Tycho product build.

- Git-related steps are simulation-only (WP9 boundary); every test asserts the repository HEAD and working-tree status are unchanged (read-only verification).
- Real children are tracked by PID and terminated by the tests/tearDown; post-run verification shows zero `SelfDevStubServer` processes.

## 2. Scenarios

| Scenario | Result | Evidence |
|---|---|---|
| A — Successful lifecycle | PASS | manifest record → deploy → START_EVO SUCCESS with readiness ≥ APPLICATION_INITIALIZED → ownership record (run-scoped, identity-verified) → identity-verified STOP → exit status persisted; parent JVM alive |
| B — Build failure | PASS | simulated BUILD_EVO failure → EXPORT_EVO BLOCKED with dependency message; START_EVO never executed |
| C — Invalid artifact | PASS | missing manifest → FAILED "missing required EVO RCP build artifact"; tampered artifact → FAILED "Checksum mismatch"; deleted artifact → FAILED |
| D — Child startup failure | PASS | exit-immediately product → START_EVO FAILED; ownership record carries exit status; parent JVM alive |
| E — Git failure (simulation) | PASS | pre-existing changes excluded from iteration attribution; commit without evidence blocked; push PENDING_AUTHORIZATION; repository verified unchanged |
| F — Repeated iterations / leak detection | PASS | 2 full iterations; exactly one child per active run; after stops zero live children; per-run ownership records correct |
| Fault: corrupted archive deployment | PASS | FAILED honestly |
| Fault: cross-process HTTP probe | PASS | child stub reachable from the parent JVM (code 200, RUNNING body) |

## 3. Fault-injection coverage mapping

The remaining fault injections from the WP12 specification are covered by the WP-specific test classes:
- port occupied (`SelfDevReadinessTest.testPortAvailabilityDetection`, `EvoRcpRuntime.isPortAvailable`)
- missing supervisor artifact (`SelfDevPipelineHardeningTest.testMissingLauncherPreconditionFailsStartupAndBlocksLoop`)
- invalid ownership record / PID reuse (`SelfDevProcessOwnershipTest`)
- HTTP unavailable / OSGi unresolved / app-init timeout (`SelfDevReadinessTest`)
- cancellation (`SelfDevTaskGraphGateTest`)
- missing configuration / invalid resource path (`SelfDevResourceTruthTest`, `CanonicalEvoRepositoryPreflightTest`)
- archive traversal (`SelfDevResourceTruthTest.testZipSlipRejectedByDeployer`)
- missing manifest / checksum mismatch (`SelfDevArtifactManifestTest`, Scenario C)
- recovery failure (`SelfDevRollbackTest.testRollbackFailsHonestlyWithoutKnownGood`)

## 4. Test execution

```
mvn -o -pl eu.kalafatic.evolution.controller.tests -am -Dtest=SelfDevE2ELifecycleTest verify
Tests run: 8, Failures: 0, Errors: 0, Skipped: 0  -> BUILD SUCCESS (15.0 s)
```

Post-run process check: zero `SelfDevStubServer` processes. Git check: HEAD `cbbfa121...` unchanged.

## 5. Not executed in this environment (documented)

- Scenario A with a REAL Tycho product build: a full reactor build (~250 s measured separately) plus a real EVO child launch requires the operator's controlled environment (the real child would bind the allocated EVO server ports). The build path itself is exercised by `SelfDevTaskArchitectureTest`/`SelfDevBuildPolicyTest` and the real-build regression performed during WP1/WP3.
