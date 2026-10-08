# Timestamped Architecture Inventory Report — 08102026

## OSGi Bundle Taxonomy & Modules

| Bundle Symbolic Name | Type | Purpose & Responsibility |
| :--- | :--- | :--- |
| `eu.kalafatic.utils` | Plugin | Core utility methods, FUIConstants, string helpers. |
| `eu.kalafatic.evolution.model` | Plugin | EMF domain model (`evolution.ecore`). |
| `eu.kalafatic.evolution.model.edit` | Plugin | EMF Edit item providers for RCP UI. |
| `eu.kalafatic.evolution.model.editor` | Plugin | EMF generated editor components. |
| `eu.kalafatic.evolution.model.tests` | Test | JUnit 3 tests for EMF generated classes. |
| `eu.kalafatic.evolution.controller` | Plugin | Orchestration, CognitiveLoopEngine, LLMDarwinEngine, SelfDevOrchestrator, ResourceManager. |
| `eu.kalafatic.evolution.controller.tests` | Test | Integration and unit tests for controller and tasks. |
| `eu.kalafatic.evolution.servers` | Plugin | EvolutionServer (NanoHTTPD), REST endpoints, MCP server. |
| `eu.kalafatic.evolution.view` | Plugin | Eclipse RCP Workbench, MultiPageEditor, DevelopmentPage, DatasetEditorGroup, context menus. |
| `eu.kalafatic.evolution.supervisor` | Plugin | Standalone supervisor server, ProcessRunner, REST control plane. |
| `eu.kalafatic.evolution.selfdev.genome` | Plugin | MilestoneGenerator, GenomeRepository, SecondhandUpgradeEngine. |
| `eu.kalafatic.evolution.selfdev.genome.tests` | Test | Tests for selfdev.genome. |
| `eu.kalafatic.evolution.forge.tokenizer` | Plugin | SimpleBPETokenizer. |
| `eu.kalafatic.evolution.forge.math` | Plugin | Tensor math, matrix operations, activations. |
| `eu.kalafatic.evolution.forge.model` | Plugin | EvoLlmArchitecture, EvoModelArtifact, model size presets. |
| `eu.kalafatic.evolution.forge.observability` | Plugin | Loss tracking, progress reporting, metrics. |
| `eu.kalafatic.evolution.forge.agent.api` | Plugin | Agent interfaces for Forge source analysis. |
| `eu.kalafatic.evolution.forge.runtime` | Plugin | Native Java LLM inference runtime. |
| `eu.kalafatic.evolution.forge.data` | Plugin | TrainingDataAcquisitionService, HuggingFaceDatasetSource, EvoDataWriter. |
| `eu.kalafatic.evolution.forge.trainer` | Plugin | EvoLlmTrainer, AdamW optimizer, loss calculation. |
| `eu.kalafatic.evolution.forge.controller` | Plugin | ForgeOrchestratorImpl, ForgeJob lifecycle manager. |
| `eu.kalafatic.evolution.forge-lab` | Plugin | Experimental neural architectures and playground. |
| `eu.kalafatic.evolution.creatic` | Plugin | Code creative generators and prompt templates. |
| `eu.kalafatic.evolution.media` | Plugin | Flag assets, graphics, UI icons. |
| `eu.kalafatic.evolution.tests` | Test | Platform level integration test suite. |
| `eu.kalafatic.evolution.quality.tests` | Test | Code quality and static compliance tests. |
| `eu.kalafatic.evolution.feature` | Feature | OSGi feature packaging for Tycho build. |
| `eu.kalafatic.evolution.repository` | Product | Tycho product definition (`evolution.product`, `evo.product`). |
| `eu.kalafatic.evolution.module` | POM | Maven module aggregator pom. |
