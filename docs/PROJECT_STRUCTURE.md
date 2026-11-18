# EVO — Project Structure

Authoritative parent/aggregator: **`pom.xml` (repository root)** — `eu.kalafatic.evolution.aggregator` 2.6.11-SNAPSHOT

| Layer | Location | Notes |
|---|---|---|
| Repository root | `/home/petr/git/evolution-121126` | Git root; parent POM at root |
| Build system | Maven + Tycho 4.0.5, `./mvnw` (Maven 3.9.12) | Java 21 |
| Target platform | Eclipse 2025-12 SimRel + Orbit + GEF Classic | `eu.kalafatic.evolution.module/platform.target` (2026-09 + GEF Classic units for the IDE) |
| Feature | `eu.kalafatic.evolution.feature/feature.xml` | Includes all 26 application plugins |
| Products | `eu.kalafatic.evolution.view/evolution.product`, `evo.product` (RCP GUI) | `eu.kalafatic.evolution.repository/{evolution,evo,headless}.product` (assembly) |
| Repository/assembly | `eu.kalafatic.evolution.repository` | `tycho-p2-director` materializes products (linux + win32) |
| Launch configs | `.launches/` (8 files) | RCP Application, RCP Server Mode, Headless Server, Maven Build, Model/Controller/Quality/Genome Tests |

## Reactor modules (30)

| Module | Role |
|---|---|
| eu.kalafatic.utils | Utility library (SWT/EMF helpers, ZoomManager hack) |
| eu.kalafatic.evolution.model | EMF model (orchestration) |
| eu.kalafatic.evolution.model.edit / .editor | EMF edit + editor support |
| eu.kalafatic.evolution.controller | Core Darwin engine/orchestration (KernelFacade, IterationManager, ADarwinEngine) |
| eu.kalafatic.evolution.view | RCP UI, application entry point, Zest/Draw2d graph views |
| eu.kalafatic.evolution.media | Media rendering |
| eu.kalafatic.evolution.servers | HTTP/MCP servers, DB, embedded jackson/nanohttpd/sqlite (exports jackson packages) |
| eu.kalafatic.evolution.enr | ENR integration bridge |
| eu.kalafatic.evolution.creatic | Creative modes |
| eu.kalafatic.evolution.forge.* (12) | Forge: math, model, tokenizer, data, runtime, trainer, agent.api, observability, controller, agent |
| eu.kalafatic.evolution.selfdev.genome | Self-development genome |
| eu.kalafatic.evolution.supervisor | Supervisor (plain JAR, shaded) |
| eu.kalafatic.evolution.module | Target-platform project (pom packaging, platform.target) |
| eu.kalafatic.evolution.feature | Feature definition |
| eu.kalafatic.evolution.repository | p2 repository + product materialization |
| *tests (model.tests, controller.tests, quality.tests, selfdev.genome.tests, tests) | Test bundles |
| eu.kalafatic.evolution.gpu | Standalone GPU diagnostic module (not in reactor; builds independently) |

## Non-reactor projects
- `eu.kalafatic.evolution.gpu` — standalone Maven JAR (GpuDiagnosticMain), Java 21 + preview.
- `docs/` — documentation POM (not in reactor).
