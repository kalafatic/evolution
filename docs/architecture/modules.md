# Logical Module Structure

**Document Identifier**: `docs/architecture/modules.md`
**Date**: September 9, 2026

---

## 1. Top-Level Module Map

The EVO logical architecture organizes system responsibilities into cohesive tiers:

```text
evo
│
├── core                 # Domain abstractions, orchestration lifecycle, events, configuration
│   ├── eu.kalafatic.evolution.model
│   ├── eu.kalafatic.evolution.controller
│   └── eu.kalafatic.utils
│
├── intelligence         # Multi-agent reasoning, intent classification, LLM routing
│   ├── eu.kalafatic.evolution.forge.agent
│   └── eu.kalafatic.evolution.forge.agent.api
│
├── evolution            # Darwin trajectory evaluation, variant search, fitness scoring
│   ├── eu.kalafatic.evolution.controller (ADarwinEngine, IterationManager)
│   └── eu.kalafatic.evolution.forge.observability
│
├── forge                # Source analysis, dataset preparation, neural model training
│   ├── eu.kalafatic.evolution.forge.data
│   ├── eu.kalafatic.evolution.forge.model
│   ├── eu.kalafatic.evolution.forge.trainer
│   ├── eu.kalafatic.evolution.forge.controller
│   └── eu.kalafatic.evolution.forge.tokenizer
│
├── inference            # Pure JVM zero-dependency native inference engine
│   └── eu.kalafatic.evolution.forge.model.inference
│
├── selfdev              # Self-development supervisor, build runner, runtime verification
│   ├── eu.kalafatic.evolution.supervisor
│   ├── eu.kalafatic.evolution.selfdev.genome
│   └── eu.kalafatic.evolution.servers
│
├── git                  # Model versioning, workspace isolation, diff/commit tracking
│   └── eu.kalafatic.evolution.controller.git
│
└── ui                   # Presentation layer: Eclipse RCP views, Web UI bridges
    └── eu.kalafatic.evolution.view
```

---

## 2. Package Responsibility Table

| Module / Package | Primary Responsibility | Key Classes |
| :--- | :--- | :--- |
| `eu.kalafatic.evolution.model` | Canonical EMF model entities & domain interfaces | `ChatMessage`, `TaskContext`, `Session` |
| `eu.kalafatic.evolution.controller` | Core orchestration pipelines & Darwin engine | `OrchestratorServiceImpl`, `ADarwinEngine` |
| `eu.kalafatic.evolution.forge.data` | Dataset acquisition, cleaning, deduplication | `DatasetAcquisitionEngine`, `EvoDatasetArtifact` |
| `eu.kalafatic.evolution.forge.model` | Causal transformer math & `.evo` protocol | `EvoLlmModel`, `EvoModelArtifact` |
| `eu.kalafatic.evolution.forge.trainer` | Native Java backpropagation & AdamW optimizer | `EvoLlmTrainer` |
| `eu.kalafatic.evolution.forge.model.inference` | Zero-dependency KV-Cache JVM runtime | `ReferenceEvoInferenceEngine` |
| `eu.kalafatic.evolution.forge.agent.api` | Exporters, GGUF binary serialization & validation | `OllamaExporter`, `GGUFValidator` |
| `eu.kalafatic.evolution.supervisor` | Out-of-process product build & deployment supervisor | `SelfDevSupervisor`, `ProcessRunner` |
| `eu.kalafatic.evolution.view` | Presentation views, HTML/JS Web UI bridges | `AiChatPage`, `DatasetEditorGroup` |
