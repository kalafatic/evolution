# EVO — Comprehensive Architecture Audit & Consolidation Report

**Date**: September 2026
**System**: EVO General Experimental AI Platform

---

## Executive Summary

This audit evaluates the architectural landscape of EVO across its primary OSGi modules, domain services, orchestration engines, and user interfaces.

EVO is an experimental AI operating platform designed to execute arbitrary AI-related workflows:
- Conversation & Inference
- Reasoning / RLLM
- Software Engineering & Code Analysis
- Dataset Acquisition (`.evodata`) & Fine-Tuning
- LLM Forging & Native Model Training (`.evo`)
- Evolutionary Darwin Optimization
- Self-Development & Mutation

Rather than redesigning EVO from scratch or creating parallel architectures, this audit identifies the existing **canonical abstractions** and maps the consolidation strategy to align all components with a unified, extensible architecture.

---

## 1. Canonical Existing Abstractions

The following core interfaces and authority classes form the foundational architecture of EVO:

| Layer | Canonical Abstraction | Package Location | Primary Responsibility |
| :--- | :--- | :--- | :--- |
| **Task / Context** | `TaskContext`, `SessionContainer` | `eu.kalafatic.evolution.controller` | Single context boundary holding session state, task intent, execution metrics, and logs. |
| **Capabilities** | `ICapability`, `CapabilityRegistry` | `eu.kalafatic.evolution.controller.orchestration.capability` | Decoupled contracts representing extensible AI operational capabilities. |
| **Inference & RLLM**| `ILlmProvider`, `OllamaProvider`, `LlmResponse` | `eu.kalafatic.evolution.controller.orchestration.llm` | Uniform LLM/RLLM interface enforcing strict separation between reasoning/thinking and final answer. |
| **Native Inference** | `ReferenceEvoInferenceEngine`, `EvoInferenceEngine` | `eu.kalafatic.evolution.forge.model.inference` | Zero-dependency, KV-Cache accelerated JVM native inference runtime for `.evo` model artifacts. |
| **Evolution Core** | `ADarwinEngine`, `IterationManager` | `eu.kalafatic.evolution.controller`, `eu.kalafatic.evolution.supervisor` | Evolutionary optimization core driving candidate generation, fitness evaluation, and winner selection. |
| **Forge Orchestrator**| `ForgeOrchestrator`, `ForgeOrchestratorImpl` | `eu.kalafatic.evolution.forge.controller` | Pipeline authority for dataset composition, model preflight validation, training, and artifact export. |
| **Data Acquisition** | `TrainingDataAcquisitionService`, `EvoDataWriter` | `eu.kalafatic.evolution.forge.data` | Multi-source dataset acquisition engine delivering structured `.evodata` archives. |
| **Model Trainer** | `EvoLlmTrainer`, `EvoLlmModel`, `EvoModelArtifact` | `eu.kalafatic.evolution.forge.trainer`, `eu.kalafatic.evolution.forge.model` | Pure Java backpropagation engine with AdamW optimizer, producing canonical `.evo` native packages. |
| **Self-Development**| `SelfDevOrchestrator`, `DevelopAgent`, `SelfDevSupervisor` | `eu.kalafatic.evolution.controller.orchestration.selfdev`, `eu.kalafatic.evolution.supervisor` | Out-of-process build, deploy, test, and rollback lifecycle for autonomous self-improvement. |

---

## 2. Identified Consolidation & Unification Targets

1. **Inference & Reasoning Protocol Uniformity**:
   - Standardize `LlmResponse` usage across `LlmRouter` and `OllamaProvider` so that thinking/reasoning tags are consistently isolated into the dedicated `reasoning` field rather than mixing with user-facing final content.
2. **Forge Training Pipeline Delegation**:
   - Ensure evolutionary model creation in `LLMDarwinEngine` delegates directly to `ForgeOrchestratorImpl` and `EvoLlmTrainer`, eliminating parallel training loop variations.
3. **Session Context Hardening**:
   - Guarantee request-scoped isolation across async worker threads and eliminate any static mutable state across session boundaries.
4. **UI Decoupling**:
   - Enforce that Eclipse RCP views (`AiChatPage`, `ForgeSettingsDialog`, `DatasetEditorGroup`) act strictly as UI observers and clients submitting tasks to orchestrators, rather than executing low-level domain logic directly.

---

## 3. Extensibility Architecture

EVO's execution pipeline follows a single unified flow:
```text
REQUEST ──► INTENT / PLAN ──► CAPABILITY SELECTION ──► EXECUTION ──► EVALUATION / DARWIN ──► ARTIFACT / RESULT
```

New platform capabilities (such as future multimodal models, RAG, web tools, or distributed inference) can be integrated by implementing `ICapability` and registering with `CapabilityRegistry` without modifying core orchestration or session management.
