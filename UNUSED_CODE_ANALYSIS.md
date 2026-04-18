# Unused Code Analysis Report (eu.kalafatic.utils bundle)

After a major upgrade and refactoring of the Evolution project, several components in the `eu.kalafatic.utils` bundle have been identified as potentially unused or legacy. These components appear to be holdovers from the "Gemini" project or experimental features not integrated into the current AI Orchestration workflow.

## 1. Identified Unused Classes

| Class | Category | WHY it is unused | Proposition |
| :--- | :--- | :--- | :--- |
| `eu.kalafatic.utils.protocols.x` | Placeholder | Empty class with a non-descriptive name. | **Delete** |
| `eu.kalafatic.utils.ui.Snippet111` | Example | Eclipse SWT snippet for TreeEditor. | **Delete** or move to a dedicated examples folder/bundle. |
| `eu.kalafatic.utils.ui.Snippet264` | Example | Eclipse SWT snippet for StyledText. | **Delete** or move to a dedicated examples folder/bundle. |
| `eu.kalafatic.utils.ui.TypeOne` | Placeholder | Model object for an unused Form Editor example. | **Delete** |
| `eu.kalafatic.utils.ui.TypeTwo` | Placeholder | Model object for an unused Form Editor example. | **Delete** |
| `eu.kalafatic.utils.ui.TypeOneDetailsPage` | Example | UI component for an unused Form Editor example. | **Delete** |
| `eu.kalafatic.utils.ui.TypeTwoDetailsPage` | Example | UI component for an unused Form Editor example. | **Delete** |
| `eu.kalafatic.utils.ui.Excel` | Experimental | Experimental View for Excel OLE integration. Not used in AI workflow. | **Delete** |
| `eu.kalafatic.utils.ui.SendEmail` | Legacy | Handler for sending emails. No registration in plugin.xml. | **Delete** |
| `eu.kalafatic.utils.factories.DrawExample` | Example | Graphics drawing example. | **Delete** |
| `eu.kalafatic.utils.model.PolygonObject` | Legacy | Geometric model object. No references in Evolution core logic. | **Delete** |

## 2. Legacy Model Components (Gemini Project leftovers)

The following classes are used by legacy preference pages or internal utility maps but are not part of the active Evolution AI features:

- **`eu.kalafatic.utils.model.SharedModel`**: Used by `AppData.getSharedModels()`. No active logic in Evolution uses this shared model system.
  - *Proposition*: Deprecate and remove once `AppData` is cleaned up.
- **`eu.kalafatic.utils.model.Association`**: Used for Windows file associations in `OSIntegrationPreferencePage`.
  - *Proposition*: Keep only if OS integration features (like file associations) are desired for the final product; otherwise, remove.

## 3. Observations on Project Structure

- The `eu.kalafatic.utils` bundle lacks a `plugin.xml`. This confirms that any UI components (Views, Handlers) contained within it are either:
  1. Contributed by other bundles (e.g., `eu.kalafatic.evolution.view`) via code or fragment.
  2. Currently inactive and not part of the Eclipse RCP application's visible surface.
- Many classes in `eu.kalafatic.utils.ui` seem to be tutorials or templates for building Eclipse Forms.

## 4. General Recommendations

1. **Workspace Cleanup**: Delete the "Snippet" and "Type*" classes immediately to reduce noise.
2. **Bundle Refactoring**: Consider splitting `eu.kalafatic.utils` into `eu.kalafatic.utils.core` (pure Java/OSGi utils) and `eu.kalafatic.utils.ui.legacy` if some legacy UI components must be kept for reference.
3. **Model Convergence**: The recent refactoring has moved core data classes to the Ecore model. Any remaining data classes in `utils.model` should either be moved to Ecore or deleted if confirmed unused.
