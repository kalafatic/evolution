# Package Context: UI and View Layer
## Domain: Presentation

This package contains the Eclipse-based SWT/JFace UI components for the Evolution platform.

### Core Components
- **MultiPageEditor**: The main orchestration interface.
- **AiChatPage**: Conversational interface for interacting with the Kernel.
- **ArchitecturePage**: Visual representation of the project design and metadata.
- **MediatedTargetDialog**: Configuration for external analysis.

### Invariants
- **SWT UI Stabilization**: Use setTextSafe, setSelectionSafe, etc., from base classes to prevent flickering.
- UI elements must be created via GUIFactory.
