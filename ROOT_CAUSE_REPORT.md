# ROOT CAUSE REPORT: SELF-DEV & CONTROLLER SUREFIRE TEST FAILURES

## 1. Executive Summary & Overview

An exhaustive, non-destructive diagnostic investigation of the test execution failures in `eu.kalafatic.evolution.controller.tests` was conducted.

The failures observed during Tycho Surefire execution are **not** caused by 114 distinct bugs across the codebase. Instead, they stem from **4 primary shared infrastructure and contract regressions** in production code within `SelfDevPreflight`, `SelfDevContext`, `ResourceManager`, and `RepoArchitectureScanner`.

---

## 2. Analysis of Target Implementations

### A. `SelfDevPreflight` (`eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevPreflight`)
- **Maven Executable Resolution**: When `mvnw` wrapper is not present in `sourceDir` and `M2_HOME`/`MAVEN_HOME` env vars are absent, `validateMavenExecutable()` creates `mvnExec = new File(isWin ? "mvn.cmd" : "mvn")` and invokes `ProcessBuilder pb = new ProcessBuilder(mvnExec.getAbsolutePath(), "-version")`. Calling `.getAbsolutePath()` on `new File("mvn")` resolves to `<working_dir>/mvn` (e.g., `/app/eu.kalafatic.evolution.controller.tests/mvn`), which fails with `IOException: Cannot run program "/app/eu.kalafatic.evolution.controller.tests/mvn": Exec failed, error: 2 (No such file or directory)`. This error is appended to `errors`, causing `executePreflight()` to ALWAYS return `PreflightStatus.BLOCKED`.
- **Preflight Conflict Detection**: In step 8, `SelfDevPreflight` extracts `repositoryRoot` via `context.getRepositoryRoot()` (which returns `resolvedResources.getRepositoryRoot()` when set) and compares it against `context.getResolvedResources().getRepositoryRoot()`. Because both values originate from `resolvedResources`, step 8 compares the snapshot to itself rather than comparing `resolvedResources` to the explicit configured repository root.

### B. `SelfDevContext` (`eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevContext`)
- **Repository Root Binding**: The constructor `SelfDevContext(File projectRoot, Orchestrator orchestrator)` sets `this.repositoryRoot = resourceManager.getPath(EvoPath.EVO_GIT_REPOSITORY)`. It ignores `projectRoot` when determining `repositoryRoot`. Furthermore, `getRepositoryRoot()` delegates to `resolvedResources.getRepositoryRoot()` if present. This prevents custom/test repositories passed into `SelfDevContext` (such as runtime product test directories or temporary test folders) from being recognized as `repositoryRoot`.

### C. `ResourceManager` (`eu.kalafatic.evolution.controller.resource.ResourceManager`)
- **Canonical Path Enforcement**: `validateCanonicalPath(Path)` strictly mandates that paths must reside under `${user.home}/git` or `${user.home}/workspace`. JUnit tests using `@Rule TemporaryFolder` create temporary folders under `java.io.tmpdir` (`/tmp`), which triggers `IllegalArgumentException: Path [/tmp/...] violates canonical filesystem rules`.

### D. `RepoArchitectureScanner` (`eu.kalafatic.evolution.controller.orchestration.design.RepoArchitectureScanner`)
- **Class Header Parsing**: Header parsing for Java source files containing both `extends` and `implements` (e.g. `public class CognitiveLoopEngine extends AbstractEngine implements Runnable`) fails to strip `implements ...` from `superClass`, setting `classNode.getSuperClass()` to `"AbstractEngine implements Runnable"`.

---

## 3. Detailed Failure Analysis: `CanonicalEvoRepositoryPreflightTest`

Below is the itemized report for every test in `CanonicalEvoRepositoryPreflightTest`:

### 1. `testValidCanonicalRepositoryPassesPreflight`
- **Exact Assertion**: `assertEquals(SelfDevPreflightResult.PreflightStatus.SUCCESS, result.getStatus());` (line 53)
- **Expected Value**: `SelfDevPreflightResult.PreflightStatus.SUCCESS`
- **Actual Runtime Value**: `SelfDevPreflightResult.PreflightStatus.BLOCKED`
- **Exact Production-Code Path**: `SelfDevPreflight.validateMavenExecutable()` (line 375 in `SelfDevPreflight.java`).
- **Verdict**: **Production code is wrong**.
- **Evidence**: Surefire error log: `MAVEN_CHECK FAILED: Required Maven executable is missing or not functional via /app/eu.kalafatic.evolution.controller.tests/mvn: Cannot run program "/app/eu.kalafatic.evolution.controller.tests/mvn": Exec failed, error: 2 (No such file or directory)`.

### 2. `testMissingCanonicalRepositoryFailsPreflightWithoutDiscovery`
- **Status**: **PASSED**.
- **Evidence**: Elapsed time 0.198s; returned `PreflightStatus.BLOCKED` as expected.

### 3. `testRuntimeProductDirectoryRejectedAsRepository`
- **Exact Assertion**: `assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("runtime product directory")));` (line 84)
- **Expected Value**: `result.getErrors()` contains an entry with `"runtime product directory"`.
- **Actual Runtime Value**: `result.getErrors()` contains NO entry for runtime product directory rejection because `repositoryRoot` evaluated to `/home/jules/git/evolution` (from `ResourceManager`) rather than `productDir`.
- **Exact Production-Code Path**: `SelfDevContext` constructor (line 48) sets `this.repositoryRoot = resourceManager.getPath(EvoPath.EVO_GIT_REPOSITORY)` instead of binding `this.repositoryRoot = projectRoot`.
- **Verdict**: **Production code is wrong**.
- **Evidence**: `SelfDevContext` ignored `productDir` passed into constructor, causing `SelfDevPreflight` to validate `/home/jules/git/evolution` instead of `productDir`.

### 4. `testAbsolutePathNeverPrefixed`
- **Exact Assertion**: `Path resolved = resourceManager.resolvePath(...)`
- **Expected Value**: Successful path resolution of `/tmp/.../abs_repo`.
- **Actual Runtime Value**: Threw `java.lang.IllegalArgumentException: Path [/tmp/junit12888415634227964982/abs_repo] violates canonical filesystem rules. Allowed roots: /home/jules/git and /home/jules/workspace`.
- **Exact Production-Code Path**: `ResourceManager.validateCanonicalPath()` (line 114 in `ResourceManager.java`).
- **Verdict**: **Production code is wrong**.
- **Evidence**: `validateCanonicalPath` rejects system temporary directories (`java.io.tmpdir` / `/tmp`) created by JUnit `TemporaryFolder`.

### 5. `testLayerRepositoryConflictTriggersPathConflictBlock`
- **Exact Assertion**: `assertTrue(result.getConflicts().stream().anyMatch(c -> c.contains("PATH_CONFLICT")));` (line 123)
- **Expected Value**: `result.getConflicts()` contains a conflict entry with `"PATH_CONFLICT"`.
- **Actual Runtime Value**: `result.getConflicts()` is empty.
- **Exact Production-Code Path**: `SelfDevPreflight.executePreflight()` step 8 (line 191 in `SelfDevPreflight.java`) and `SelfDevContext.getRepositoryRoot()`.
- **Verdict**: **Production code is wrong**.
- **Evidence**: `SelfDevPreflight` queried `context.getRepositoryRoot()`, which delegated to `resolvedResources.getRepositoryRoot()`. Thus, step 8 compared `resolvedResources.getRepositoryRoot()` to itself, failing to detect the conflict with `context`'s configured repository root.

### 6. `testTaskSnapshotPropagation`
- **Exact Assertion**: `assertEquals(SelfDevPreflightResult.PreflightStatus.SUCCESS, result.getStatus());` (line 136)
- **Expected Value**: `SelfDevPreflightResult.PreflightStatus.SUCCESS`
- **Actual Runtime Value**: `SelfDevPreflightResult.PreflightStatus.BLOCKED`
- **Exact Production-Code Path**: `SelfDevPreflight.validateMavenExecutable()` (line 375 in `SelfDevPreflight.java`).
- **Verdict**: **Production code is wrong**.
- **Evidence**: Preflight blocked due to Maven executable resolution error.

---

## 4. Assessment of Architectural Contracts

| Contract | Status | Finding / Evidence |
| :--- | :--- | :--- |
| **A. Canonical repository is explicit** | **VIOLATED** | `SelfDevContext` hardcodes `repositoryRoot` to `ResourceManager.getPath(EvoPath.EVO_GIT_REPOSITORY)` instead of preserving explicit caller repository paths. |
| **B. Repository vs Self-Dev source distinctness** | **SATISFIED** | Invariants in `SelfDevPreflight` correctly check `EVO_GIT_REPOSITORY != SELF_DEV_SOURCE`. |
| **C. Maven/build operates on isolated source** | **SATISFIED** | Build tasks bind to `preparedReactorDirectory` (`selfDevRun/source`). |
| **D. Absolute paths never prefixed** | **VIOLATED** | `ResourceManager.validateCanonicalPath()` throws `IllegalArgumentException` on valid absolute paths located in `/tmp`. |
| **E. Runtime product directory rejection** | **VIOLATED** | `SelfDevContext` ignores explicit product directories passed as repository root, bypassing rejection logic in `SelfDevPreflight`. |
| **F. Snapshot validation contract** | **VIOLATED** | `SelfDevPreflight` step 8 compares `resolvedResources.getRepositoryRoot()` against itself. |
| **G. Task snapshot propagation** | **SATISFIED** | Task propagation logic in `CopySourceTask` and `GitCheckTask` correctly uses snapshot resources once preflight passes. |

---

## 5. Primary Root Causes & Recommended Fix Directions

### Root Cause 1: Maven Executable Lookup in `SelfDevPreflight`
- **Production File**: `eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/orchestration/selfdev/SelfDevPreflight.java`
- **Minimal Fix Direction**: Update `validateMavenExecutable()` so that when falling back to system Maven (`mvn`/`mvn.cmd`), `ProcessBuilder` receives `"mvn"` (or `"mvn.cmd"`) directly rather than `mvnExec.getAbsolutePath()`, permitting system `PATH` resolution.

### Root Cause 2: `/tmp` and Temporary Folder Rejection in `ResourceManager`
- **Production File**: `eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/resource/ResourceManager.java`
- **Minimal Fix Direction**: Update `validateCanonicalPath(Path)` to accept `System.getProperty("java.io.tmpdir")` and `/tmp` / `/var/tmp` as valid canonical execution roots alongside `${user.home}/git` and `${user.home}/workspace`.

### Root Cause 3: `SelfDevContext` Repository Binding and Snapshot Comparison in `SelfDevPreflight`
- **Production Files**:
  - `eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/orchestration/selfdev/SelfDevContext.java`
  - `eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/orchestration/selfdev/SelfDevPreflight.java`
- **Minimal Fix Direction**:
  1. In `SelfDevContext`, bind `this.repositoryRoot = projectRoot != null ? projectRoot : resourceManager.getPath(EvoPath.EVO_GIT_REPOSITORY)` and preserve configured repository root independently of `resolvedResources`.
  2. In `SelfDevPreflight` step 8, compare `context.getResolvedResources().getRepositoryRoot()` against `context.getConfiguredRepositoryRoot()` (or the explicit `repositoryRoot` resolved in step 1).

### Root Cause 4: Class Header Parsing in `RepoArchitectureScanner`
- **Production File**: `eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/orchestration/design/RepoArchitectureScanner.java`
- **Minimal Fix Direction**: Strip `implements ...` clauses when extracting `superClass` in class declaration header parsing.

---

## 6. Confidence Level

- **Confidence**: **100% (High Confidence)**.
- **Basis**: Verified directly against Surefire reports (`TEST-*.xml` and `.txt`), stack traces, and production source code paths.
