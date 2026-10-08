# EVO — Canonical Technical & Architectural Map

Welcome to the canonical documentation for **EVO** (Evolution Platform). This repository contains the complete technical architecture, subsystem specifications, execution flow maps, data models, build guidelines, and operational manuals reverse-engineered directly from the production source code.

---

## 🚀 AI Agent & Developer Quick Start Map

If you are an AI assistant or software developer working on EVO, use this table to locate the single source of truth for each area:

| Area / Question | Primary Reference File | Key Topics Covered |
| :--- | :--- | :--- |
| **Project Overview & System Architecture** | [`architecture/overview.md`](architecture/overview.md) | High-level topology, core design principles, layer hierarchy, system boundaries, and Genome module integration. |
| **Module Inventory & Bundles** | [`architecture/modules.md`](architecture/modules.md) | OSGi bundle taxonomy, responsibilities, public APIs, and cross-bundle mappings across 29 modules. |
| **Module Dependencies & Layering** | [`architecture/dependencies.md`](architecture/dependencies.md) | Package import/export graphs, coupling analysis, circular dependency tracking. |
| **Runtime Topology & OSGi** | [`architecture/runtime.md`](architecture/runtime.md) | OSGi Equinox container, process models, embedded HTTP servers, multi-process modes. |
| **Threading & Concurrency** | [`architecture/threading.md`](architecture/threading.md) | SWT UI thread boundaries, Eclipse Jobs, background execution, synchronization safeguards. |
| **Configuration Architecture** | [`architecture/configuration.md`](architecture/configuration.md) | EMF configuration authority, `ResourceManager`, resolution rules, defaults vs overrides. |
| **Architectural Problem Register** | [`architecture/problems.md`](architecture/problems.md) | Audit findings, severity levels, architectural anti-patterns, recommended refactorings. |
| **Darwin Subsystem (Agentic Loop)** | [`DARWIN.MD`](DARWIN.MD) | Agentic iteration loop, intent analysis, strategy discovery, candidate generation, blueprint pre-spawning. |
| **AI Kernel Architecture** | [`AI_KERNEL_ARCHITECTURE.md`](AI_KERNEL_ARCHITECTURE.md) | Capabilities, contracts, signals, decision authority, and backpressure scheduling. |
| **Semantic Workspace & Memory** | [`SEMANTIC_WORKSPACE_ARCHITECTURE.md`](SEMANTIC_WORKSPACE_ARCHITECTURE.md) | Trajectory memory, semantic artifacts, context curation, knowledge decay. |
| **Self-Dev Pipeline Analysis** | [`SELF_DEV_ANALYSIS.md`](SELF_DEV_ANALYSIS.md) | Automated pipeline (Check -> Copy -> Build -> Export -> Run -> Verify), workspace model. |
| **EVO Inference Engine** | [`EVO_INFERENCE_ENGINE.md`](EVO_INFERENCE_ENGINE.md) | Native Java LLM inference runtime and GGUF bridge. |
| **Package Context Inventory** | [`PACKAGE_CONTEXT.md`](PACKAGE_CONTEXT.md) | Comprehensive directory and component taxonomy across the `docs/` hierarchy. |

---

## 🏛️ System Topology Snapshot

EVO is an OSGi-based native AI development environment built on top of the Eclipse Rich Client Platform (RCP) and Tycho/Maven infrastructure. It integrates local/remote LLM inference, autonomous dataset compilation, native LLM training, agentic repository mutation (Darwin), self-guided workspace evolution (Self-Dev), and structural evolution mapping (Genome).

```
                      +-----------------------------------------------+
                      |           Eclipse RCP UI / Workbench          |
                      |  (MultiPageEditor, DevelopmentPage, Pages)   |
                      +-----------------------+-----------------------+
                                              |
                                              v
                      +-----------------------------------------------+
                      |         Controller & Service Engines          |
                      |  (CognitiveLoopEngine, LLMDarwinEngine)       |
                      +-----------+-----------------------+-----------+
                                  |                       |
                                  v                       v
      +-----------------------------------+       +-----------------------------------+
      |        Forge LLM Subsystem        |       |    Self-Dev & Genome Subsystems   |
      | (Acquisition, Trainer, Tokenizer) |       |  (Task Engine, SelfDevGenomeHub)  |
      +-----------------------------------+       +-----------------------------------+
                                  |                       |
                                  v                       v
      +-------------------------------------------------------------------------------+
      |                   Persistence, Artifacts & External Interfaces            |
      |   (EMF Model, .evodata, .evo, .gguf, GenomeArtifact, Git, Hugging Face, MCP)  |
      +-------------------------------------------------------------------------------+
```

---

## 📊 Documentation Confidence & Coverage Matrix

All statements in this canonical documentation suite are derived from direct analysis of source code, configuration files, POMs, and plugin manifests.

| Section | Verification Status | Verification Evidence / Source References |
| :--- | :--- | :--- |
| **Architecture & Topology** | **Verified** | Inspected `plugin.xml`, `MANIFEST.MF`, `pom.xml`, `evolution.product`, `evo.product`. |
| **Core & Resources** | **Verified** | `ResourceManager.java`, `ResolvedSelfDevResources.java`, `RuntimeEventBus.java`. |
| **UI Architecture** | **Verified** | `MultiPageEditor.java`, `DevelopmentPage.java`, `EvoGlobalContextMenuManager.java`. |
| **Forge Subsystem** | **Verified** | `ForgeOrchestratorImpl.java`, `EvoLlmTrainer.java`, `TrainingDataAcquisitionService.java`. |
| **Darwin Subsystem** | **Verified** | `LLMDarwinEngine.java`, `CognitiveLoopEngine.java`, `IterationManager.java`. |
| **Self-Dev Subsystem** | **Verified** | `SelfDevOrchestrator.java`, `AbstractSelfDevTask.java`, `TychoEvoRcpBuilder.java`. |
| **Genome Subsystem** | **Verified** | `SelfDevGenomeHub.java`, `LocalGenomeRepository.java`, `SecondhandUpgradeEngine.java`. |
| **Supervisor Subsystem** | **Verified** | `SupervisorMain.java`, `EVOSupervisorServer.java`, `SupervisorRuntime.java`. |
| **Build & Tycho** | **Verified** | Root `pom.xml`, child `pom.xml` descriptors, Tycho 4.0.5 target platform settings. |
| **Data Models** | **Verified** | `evolution.ecore`, `EvoDatasetArtifact.java`, `EvoModelArtifact.java`, `GenomeArtifact.java`. |
| **External Integrations**| **Verified** | `HuggingFaceDatasetSource.java`, `OllamaProvider.java`, `McpServersGroup.java`. |

---

## 🛠️ Essential Maintenance Rules for Documentation

1. **Source of Truth**: The Java source code, EMF models, and Genome artifacts remain the absolute source of truth.
2. **Fact vs Interpretation**: Statements marked as **FACT** are directly grounded in code; statements marked as **ARCHITECTURAL INTERPRETATION** or **RECOMMENDATION** reflect analytical synthesis.
3. **Traceability**: All major architectural assertions include explicit class and method references (`eu.kalafatic.evolution...`).
4. **No Speculation**: Features not present in code or POM configurations are categorized under "Unverified / Not Implemented".
