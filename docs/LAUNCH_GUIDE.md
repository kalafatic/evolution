# EVO — Launch Guide

Authoritative launch definitions live in `.launches/` (Eclipse PDE launch configurations) and the product definitions.

## Eclipse launch configurations (.launches/)

| Config | Type | Purpose |
|---|---|---|
| `EVO_RCP_Application.launch` | PDE Runtime Workbench | **Development launch** of the RCP GUI (application `eu.kalafatic.evolution.view.application.Application`, workspace bundles, target platform) |
| `EVO_RCP_Server_Mode.launch` | PDE Runtime Workbench | GUI with server mode |
| `EVO_Headless_Server.launch` | PDE Runtime Workbench / headless | Headless server start |
| `EVO_Maven_Build.launch` | Maven run config | Build from Eclipse |
| `EVO_Model_Tests.launch`, `EVO_Controller_Tests.launch`, `EVO_Quality_Tests.launch`, `EVO_Genome_Tests.launch` | PDE JUnit | Test launches |

In Eclipse: *Run > Run Configurations* → pick `EVO_RCP_Application` (Development) or duplicate for Debug (*Debug > Debug Configurations*). The configurations use the workspace bundles + the target platform from `eu.kalafatic.evolution.module/platform.target`.

## Product launches (built)

Build first (`./mvnw -B -DskipTests install -pl eu.kalafatic.evolution.repository -am`), then:

| Product | Launcher | Mode |
|---|---|---|
| evolution (GUI RCP) | `eu.kalafatic.evolution.repository/target/products/evolution/linux/gtk/x86_64/evolution` | GUI (needs DISPLAY) |
| evolution (win32) | `.../products/evolution/win32/win32/x86_64/evolution.exe` | GUI |
| evo | `.../products/evo/<os>/.../evolution` | GUI variant |
| headless | `.../products/headless/...` | Server/headless |

GUI products require a display; headless mode can be started via `scripts/start-headless.sh`.

## Runtime verification
- GUI launches are **not verified headlessly** on this host (no display); development launch is verified in the Eclipse IDE by the user.
- UPM launch-target resolution verified: product launchers detected under `target/products/…` and listed as `eclipse-product` launch configs.
