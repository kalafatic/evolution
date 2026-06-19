# PROJECT MAP: BUNDLE STRUCTURE

## Core Platform Bundles

### eu.kalafatic.evolution.controller
The platform's heart. Contains the `orchestration` package (kernel), `kernel` logic, and `selfdev` evolution engine.

### eu.kalafatic.evolution.model
The structural definition. Contains the EMF `.ecore` models for tasks, agents, iterations, and sessions.

### eu.kalafatic.evolution.servers
The communication layer. Contains the `NanoHTTPD` server, REST endpoints, and SQLite database logic.

### eu.kalafatic.evolution.view
The user interface. Contains Eclipse RCP views, perspective definitions, and SWT-based dashboards.

### eu.kalafatic.evolution.selfdev.genome
The evolutionary memory. Manages genome artifacts and patterns.

### eu.kalafatic.evolution.creatic
The UX/Guidance bundle. Contains the guidance engine and theme assets.

### eu.kalafatic.utils
Cross-cutting utilities for logging, semantic annotations, and file manipulation.

## Forge (Neural Laboratory) Bundles
- `eu.kalafatic.evolution.forge.controller`: Coordination of training loops.
- `eu.kalafatic.evolution.forge.model`: Forge-specific data structures.
- `eu.kalafatic.evolution.forge.tokenizer`: Source code tokenization.
- `eu.kalafatic.evolution.forge.trainer`: (Mocked) tensor training logic.

## Infrastructure
- `orchestrator/`: Workspace for temporary Darwin variants.
- `self-dev-run/`: Runtime directory for EVO self-evolution sessions.
- `data/`: SQLite databases and persistence storage.
