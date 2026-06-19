# Discovery & Mediation

## Overview
The Discovery and Mediation subsystems bridge the EVO kernel with its target project and external LLMs. Discovery builds the "Ground Truth" model of a codebase, while Mediation optimizes the communication with external intelligence.

## Discovery Subsystem
Discovery is responsible for "Recursive Reconstruction" of a project's architecture.

### Key Components
- **TargetScanner**: Performs physical scans of the filesystem, detecting technologies and file types.
- **SemanticExtractor**: Analyzes code files (imports, classes, interfaces) to build a semantic graph.
- **RealityDiscoveryAgent**: An AI agent that interprets the results of the scan to infer subsystems, responsibilities, and hotspots.
- **TargetRealityModel**: The formal JSON-LD representation of the discovered architecture.

### Discovery Workflow
1.  **Structural Scan**: Map folders and files.
2.  **Heuristic Selection**: Pick high-signal files (entry points, configs).
3.  **Semantic Analysis**: Extract interfaces and relationships.
4.  **Target Reality Formalization**: Create the `TargetRealityModel`.
5.  **Recursive Refinement**: Use knowledge gaps to trigger deeper scans of specific areas.

## Mediation Subsystem
Mediation enables "Mediated Development Mode," where EVO acts as a senior architect preparing context for an external frontier model (e.g., Claude 3.5).

### Key Components
- **ContextCurator**: The "Semantic Sieve." It uses graph centrality and knowledge gaps to select the 4-16 most significant files for the context window.
- **PromptSynthesizer**: Combines the original user request with evolved understanding and architectural metadata to produce an "Optimized Prompt" (Genome A).
- **MediatedExportManager**: Packages the curated files and optimized prompt into a portable ZIP/Markdown bundle.

## Success Criteria
A successful mediation package should allow a downstream LLM to understand and extend the target system with minimal additional context.

## Related
- [ARCHITECTURE.md](../architecture/ARCHITECTURE.md)
- [MediatedExportManager.java](../../../eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/workflow/MediatedExportManager.java)
