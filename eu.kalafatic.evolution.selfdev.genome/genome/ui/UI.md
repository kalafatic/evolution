# UI & Visualization

## Overview
EVO features a rich, multi-layered user interface integrated into the Eclipse IDE. It combines traditional development views (Task Tree, Command Stack) with high-performance SVG visualizations of the evolutionary process.

## Core Views

### AI Chat View
The primary interaction point. It supports streaming responses, markdown rendering, and interactive buttons for "Force Solution" or "Execute Winner."
- **Factory**: `AiChatPageFactory`
- **Location**: `chat.html` (WebView)

### Orchestration Zest View
A graphical representation of the evolution lineage, showing successful and rejected branches.
- **Class**: `OrchestrationZestView`

### Task Tree View
A hierarchical view of planned and executing agent tasks.
- **Class**: `TaskTreeView`

### Forge Page
A dedicated dashboard for model architecture design and training progress visualization.
- **Location**: `forge.html`

## Aesthetic & Theme
The platform utilizes a "Dark-Slate" aesthetic with "Neon-Cyber" accents, consistent across both SWT widgets and embedded HTML pages.

## UI Communication
The UI synchronizes with the kernel via:
1. **HTTP Polling**: Periodic status updates (planned for WebSocket migration).
2. **SWT Browser Injection**: Direct execution of JavaScript functions from Java.
3. **Runtime Event Bus**: Listening for UI-triggering events (e.g., `DECISION_UPDATED`).

## Related
- [ARCHITECTURE.md](../architecture/ARCHITECTURE.md)
- [SERVER.md](../server/SERVER.md)
