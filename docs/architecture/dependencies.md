# Module Dependencies & Layering Analysis

## Dependency Flow & Layering Rules

The EVO platform enforces strict unidirectional dependency flow across its OSGi bundles to preserve modularity, testability, and clean separation of concerns.

```
+-------------------------------------------------------------------+
|                        Product & Feature Layer                    |
|             (eu.kalafatic.evolution.repository, feature)          |
+-------------------------------------------------------------------+
                                  |
                                  v
+-------------------------------------------------------------------+
|                           UI Layer                                |
|                 (eu.kalafatic.evolution.view)                     |
+-------------------------------------------------------------------+
                                  |
                                  v
+-------------------------------------------------------------------+
|                    Controller & Server Layer                      |
|      (eu.kalafatic.evolution.controller, servers, supervisor)     |
+-------------------------------------------------------------------+
                                  |
                                  v
+-------------------------------------------------------------------+
|                    Engine & Subsystem Layer                       |
|   (eu.kalafatic.evolution.forge.*, selfdev.genome, creatic)      |
+-------------------------------------------------------------------+
                                  |
                                  v
+-------------------------------------------------------------------+
|                    Core Model & Utility Layer                     |
|           (eu.kalafatic.evolution.model, utils, media)            |
+-------------------------------------------------------------------+
```

---

## Strict OSGi Import / Export Invariants

1. **Core Utilities Base**: `eu.kalafatic.utils` exports basic utilities and UI constants without depending on any other EVO bundle.
2. **EMF Model Authority**: `eu.kalafatic.evolution.model` depends only on `eu.kalafatic.utils` and EMF runtime bundles (`org.eclipse.emf.ecore`). It exports generated model interfaces (`eu.kalafatic.evolution.model.orchestration`).
3. **Engine Modularity**: Subsystems under `eu.kalafatic.evolution.forge.*` depend on `eu.kalafatic.evolution.model` for configuration structures but DO NOT depend on `eu.kalafatic.evolution.view` or `eu.kalafatic.evolution.controller`.
4. **Controller Integration**: `eu.kalafatic.evolution.controller` acts as the central coordinator, importing `model`, `utils`, `forge.*`, and `selfdev.genome`.
5. **UI Layer Isolation**: `eu.kalafatic.evolution.view` imports `controller`, `model`, `servers`, `utils`, and SWT/JFace/E4 RCP dependencies. Lower-level engines never import `view`.

---

## Dependency Graph & Package Export Mappings

| OSGi Bundle | Requires Bundles | Exported Packages |
| :--- | :--- | :--- |
| `eu.kalafatic.utils` | `org.eclipse.core.runtime` | `eu.kalafatic.utils`, `eu.kalafatic.utils.constants` |
| `eu.kalafatic.evolution.model` | `eu.kalafatic.utils`, `org.eclipse.emf.ecore` | `eu.kalafatic.evolution.model.orchestration`, `eu.kalafatic.evolution.model.orchestration.impl`, `eu.kalafatic.evolution.model.orchestration.util` |
| `eu.kalafatic.evolution.controller` | `eu.kalafatic.evolution.model`, `eu.kalafatic.utils`, `eu.kalafatic.evolution.forge.*` | `eu.kalafatic.evolution.controller.resource`, `eu.kalafatic.evolution.controller.orchestration.selfdev`, `eu.kalafatic.evolution.controller.orchestration.cognitive.loop`, `eu.kalafatic.evolution.controller.ui` |
| `eu.kalafatic.evolution.servers` | `eu.kalafatic.evolution.controller`, `eu.kalafatic.evolution.model` | `eu.kalafatic.evolution.servers`, `eu.kalafatic.evolution.servers.mcp` |
| `eu.kalafatic.evolution.view` | `eu.kalafatic.evolution.controller`, `eu.kalafatic.evolution.servers`, `org.eclipse.ui` | `eu.kalafatic.evolution.view.editors`, `eu.kalafatic.evolution.view.editors.pages`, `eu.kalafatic.evolution.view.menu`, `eu.kalafatic.evolution.view.dialogs` |
| `eu.kalafatic.evolution.supervisor` | `eu.kalafatic.evolution.controller`, `eu.kalafatic.utils` | `eu.kalafatic.evolution.supervisor`, `eu.kalafatic.evolution.supervisor.ui` |

---

## Coupling & Architectural Observations

1. **Clean Decoupling of Forge Engines**: The Forge neural network engine (`forge.trainer`, `forge.tokenizer`, `forge.math`, `forge.data`) is fully decoupled from the Eclipse RCP UI and can execute headlessly in pure Java or Supervisor CLI environments.
2. **Controlled UI Invocation**: `DevelopmentPage` and `DatasetEditorGroup` invoke services via `ResourceManager` or background Eclipse `Job` instances, preventing business logic leaking directly into SWT UI event handlers.
