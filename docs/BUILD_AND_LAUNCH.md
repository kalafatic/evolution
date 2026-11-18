# EVO Build and Launch Guide

## Verified toolchain

- JDK 21 (`/usr/lib/jvm/java-21-openjdk-amd64`)
- Maven 3.9.12 through `./mvnw` (Tycho 4.0.5)
- Linux GTK x86_64

The machine-wide `java` defaults to 1.8 and `mvn` to 3.8.7. Always select JDK 21 for EVO:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
```

## Build

```bash
./mvnw -B -DskipTests -Plinux clean verify
```

Verified result: `BUILD SUCCESS` (31 modules). The non-fatal SWT POM warning about
`org.eclipse.swt.${osgi.platform}` is emitted by the Tycho dependency resolution and does
not fail the build.

Products are materialized under:

| Product | Directory | Launcher |
|---|---|---|
| AI Evolution (full RCP) | `eu.kalafatic.evolution.repository/target/products/evolution/linux/gtk/x86_64/evolution` | `evo` |
| AI Evolution (`evo` uid) | `eu.kalafatic.evolution.repository/target/products/evo/linux/gtk/x86_64/evolution` | `evo` |
| Evolution Headless | `eu.kalafatic.evolution.repository/target/products/headless/linux/gtk/x86_64/headless` | `eclipse` |

`build-product` and `scripts/build.sh` wrap the same command. `scripts/build.sh` selects JDK 21
and the `linux` profile automatically.

## Launch configurations

Launch files live in `.launches/`. They are Eclipse PDE/Maven launch configurations and are
visible after importing the repository root project (`eu.kalafatic.evolution.aggregator`).
The launches explicitly select this repository's bundles, so they stay isolated when
other projects or repository variants are imported into the same workspace.

| Launch | Purpose |
|---|---|
| `EVO_RCP_Application.launch` | Full AI Evolution RCP product (`eu.kalafatic.evolution.view.product`) |
| `EVO_RCP_Server_Mode.launch` | Full product in `--server --port 28080` mode (no UI) |
| `EVO_Headless_Server.launch` | Headless product, HTTP port 28080 |
| `EVO_Controller_Tests.launch` | PDE JUnit 4 run of `eu.kalafatic.evolution.controller.tests` |
| `EVO_Model_Tests.launch` | PDE JUnit 4 run of `eu.kalafatic.evolution.model.tests` |
| `EVO_Quality_Tests.launch` | PDE JUnit 4 run of `eu.kalafatic.evolution.quality.tests` |
| `EVO_Genome_Tests.launch` | PDE JUnit 4 run of `eu.kalafatic.evolution.selfdev.genome.tests` |
| `EVO_Maven_Build.launch` | `clean verify -DskipTests` with the `linux` profile and JDK 21 |

The RCP launch uses `eu.kalafatic.evolution.view/evolution.product`. The `evo.product` and
`evolution.product` files now both declare the only registered product extension,
`eu.kalafatic.evolution.view.product`.

## Headless runtime

```bash
scripts/build.sh
scripts/start-headless.sh 28080
```

Or directly:

```bash
cd eu.kalafatic.evolution.repository/target/products/headless/linux/gtk/x86_64/headless
./eclipse -nosplash -consoleLog --port 28080
```

Verified: the server starts, registers the MCP tools, and binds `127.0.0.1:28080`
(tested on a free port). The process runs until stopped.

The full RCP product also supports server-only mode without opening a workbench:

```bash
cd eu.kalafatic.evolution.repository/target/products/evolution/linux/gtk/x86_64/evolution
./evo -nosplash -consoleLog --server --port 28080
```

Verified: prints `Starting Evolution in SERVER-ONLY mode`, binds the port, and keeps running.

## Configuration fixes applied

1. `evo.product` (view and repository copies) declared the non-existent product id
   `eu.kalafatic.evolution.view.evo`. It now uses the registered product id
   `eu.kalafatic.evolution.view.product`. The generated `evo` product `config.ini` was
   previously invalid; it is correct after the fix.
2. `headless.product` did not auto-start the SCR extender or `org.eclipse.core.contenttype`.
   Starting it produced `Platform.getContentTypeManager()` NPEs and prevented workspace
   discovery. Both bundles are now auto-started at appropriate start levels.

The duplicated files under `eu.kalafatic.evolution.repository/bin/` were synchronized with
the repository-level product definitions.

## Test status

| Test bundle | Result |
|---|---|
| `eu.kalafatic.evolution.quality.tests` | 5 tests, all pass |
| `eu.kalafatic.evolution.model.tests` | 0 tests executed (suite empty) |
| `eu.kalafatic.evolution.selfdev.genome.tests` | 9 tests, 1 failure (`GenomeHubTest.testUpdateGenomeLifecycleWithProgressListener`) |
| `eu.kalafatic.evolution.controller.tests` | 1207 tests, 70 failures, 79 errors |

The controller and genome failures are pre-existing functional test failures, not build
configuration failures. The full `verify` stops at `controller.tests`, so the other test
bundles must be run individually (as done for the results above).

## Target platform notes

- The Tycho target platform is defined inline in the root `pom.xml` (Eclipse 2025-12 and
  Orbit p2 repositories).
- `eu.kalafatic.evolution.module/platform.target` is not wired into the reactor and
  references a Windows-only 2026-09 target. It is not used by the build.
- `eu.kalafatic.evolution.supervisor` is a standalone non-Tycho jar module with its own
  versioning and is not part of the feature/product export.
