# Dependencies and Architectural Layering

**Document Identifier**: `docs/architecture/dependencies.md`
**Date**: September 9, 2026

---

## 1. Architectural Layering Model

The target EVO dependency flow strictly enforces unidirectional coupling:

```text
       ┌────────────────────────┐
       │     UI / RCP Presentation     │
       └───────────┬────────────┘
                   │
                   ▼
       ┌────────────────────────┐
       │   Application Services  │
       └───────────┬────────────┘
                   │
                   ▼
       ┌────────────────────────┐
       │   Domain Orchestration │
       └───────────┬────────────┘
                   │
                   ▼
       ┌────────────────────────┐
       │     Core Domain        │
       └───────────┬────────────┘
                   │
                   ▼
       ┌────────────────────────┐
       │ Infrastructure / Adapters │
       └────────────────────────┘
```

---

## 2. Identified Dependency Violations & Leaks

During the architectural audit, the following coupling issues and layer leaks were cataloged:

### 2.1 UI → Domain Internal Leakage
* **`AiChatPage.java` / `DatasetEditorGroup.java`**: Direct calls into low-level dataset preparation and native model file path resolvers bypassing application service boundaries.
* **`ForgeSettingsDialog.java`**: Direct manipulation of `LLMDarwinEngine` internals and `ForgeSession` model state.

### 2.2 Subsystem Interdependencies
* **`OllamaProvider` → `ReferenceEvoInferenceEngine`**: Direct instantiation of native inference classes from LLM provider adapters.
* **`SelfEvoForgingServiceImpl` → Exporters**: Exporter invocation embedded directly inside self-evo forging service routines rather than orchestrator pipeline steps.

### 2.3 Cyclic Bundle References
* **OSGi Dependencies**: High fan-in on `eu.kalafatic.evolution.model` and `eu.kalafatic.utils`. All core bundles depend on EMF model contracts.

---

## 3. High Fan-In / Fan-Out God Classes

1. **`OrchestratorServiceImpl`**: Fan-in from UI, Server, and Agents; Fan-out to Darwin, Forge, Ollama, Git, and File tools.
2. **`OllamaProvider`**: Serves as generic provider, native artifact resolver, GGUF interceptor, and fallback router.
3. **`SelfDevSupervisor`**: Handles HTTP server, process management, build execution, and product artifact extraction.
