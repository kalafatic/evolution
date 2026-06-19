# EVO Platform Architecture

## Overview
EVO is an experimental evolutionary software development platform designed to enable autonomous self-evolution of software systems. It moves beyond simple code generation by implementing a recursive evolutionary loop based on Darwinian principles.

## Core Philosophy
- **Evolution over Generation**: Instead of single-shot generation, EVO uses iterative mutation and selection.
- **Architectural Authority**: Decisions are grounded in a deep understanding of the target system's architecture, not just syntax.
- **Runtime Isolation**: Each evolution session is strictly isolated to prevent side effects and ensure reproducibility.
- **Sovereign Potential**: The platform is designed to eventually manage its own development (Self-Dev Mode).

## Subsystems

### 1. Orchestration (Kernel)
The "Control Plane" of the platform. It manages session lifecycles, state transitions, and coordinates between agents and tools.
- **Key Class**: `IterationManager`
- **Related Docs**: [DARWIN.md](DARWIN.md), [WORKFLOW_INDEX.md](../WORKFLOW_INDEX.md)

### 2. Darwin Evolution Engine
Implements the multi-trajectory mutation model. It generates divergent architectural blueprints and materializes them into competing code variants.
- **Key Class**: `DarwinEngine`, `DarwinFlow`
- **Related Docs**: [DARWIN.md](DARWIN.md)

### 3. Mediation & Discovery
Bridges the gap between the platform and external LLMs. It curates high-signal context packages (Genome B) and optimized prompts (Genome A).
- **Key Class**: `RealityDiscoveryAgent`, `ContextCurator`
- **Related Docs**: [DISCOVERY.md](../modules/DISCOVERY.md)

### 4. Genome System
The persistent architectural memory. It encodes patterns, traits, and evolutionary lessons into portable artifacts.
- **Key Class**: `LocalGenomeRepository`, `GenomeArtifact`
- **Related Docs**: [GENOME.md](../modules/GENOME.md)

### 5. Forge (Neural Laboratory)
A subsystem for designing and training domain-specific AI models. (Currently features a reasoning skeleton with simulated training).
- **Key Class**: `ForgeSessionManager`
- **Related Docs**: [FORGE.md](../modules/FORGE.md)

## Communication Model
Subsystems communicate primarily through:
1. **Runtime Event Bus**: A synchronous/asynchronous signal system for platform-wide events.
2. **Signal Bus**: Specific for trajectory evaluation and fitness signals.
3. **Session Context**: Dependency injection container for session-scoped services.

## Technology Stack
- **Language**: Java 21
- **Framework**: Eclipse RCP / OSGi
- **Modeling**: Eclipse Modeling Framework (EMF)
- **VCS**: JGit (automated branch/worktree management)
- **Build**: Maven / Tycho
- **Communication**: NanoHTTPD (REST), WebSocket (planned)
- **Intelligence**: LLM Providers (OpenAI, Gemini, Ollama)

## Design Decisions
- [Why Event Bus exists](../decisions/DESIGN_DECISIONS.md#event-bus)
- [Why Genome is isolated](../decisions/DESIGN_DECISIONS.md#genome-isolation)

## Future Roadmap
- [Milestone 1: Real Training Backend](../roadmap/ROADMAP.md#milestone-1)
- [Milestone 2: WebSocket Control Plane](../roadmap/ROADMAP.md#milestone-2)
