# Mediated Mode Analysis

## Current Architecture

Mediated Mode is currently integrated into the Darwin evolutionary engine. When the `AiMode` is set to `MEDIATED`, the `ModeRouter` selects the `DarwinFlow`.

### Workflow
1. **Detection**: `ModeRouter` detects `MEDIATED` mode from user intent or orchestrator state.
2. **Initialization**: `IterationManager` initializes semantic repository snapshots and metadata.
3. **Evolution Loop**:
   - `DarwinFlow` coordinates the evolutionary cycles.
   - `DarwinEngine` generates mediation-specific blueprints (Architecture Mapping, Dependency Audit, Hotspot Analysis, Context Distillation).
   - `DarwinVariantSpawner` materializes these blueprints into `BranchVariant` objects using an LLM.
   - `DarwinVariantValidator` ensures the variants adhere to the expected schema.
4. **Selection**: Variants are evaluated and selected (either automatically or via user selection in `IterationManager.handleVariantSelection`).
5. **Convergence**: Once the evolution stabilizes or the user forces a solution, `IterationManager.performMediatedExportConvergence` is called.
6. **Export**: `MediatedExportManager` packages the "final" understanding into a ZIP archive.

## Identified Issues

1. **Duplicated/Conflicting Flows**: `performMediatedExportConvergence` re-synthesizes the prompt using `PromptSynthesizer` and performs its own context curation if none is found in metadata. This bypasses the evolutionary refinements made during the Darwin rounds.
2. **Lack of Formal Candidate Model**: There is no explicit `MediationCandidate` class. Evolved artifacts (prompt, architecture summary, selected files) are stored loosely in `BranchVariant` fields or `OrchestrationState` metadata.
3. **Incomplete Export Package**: The current ZIP export only includes `PROMPT.md`, `METADATA.json`, `EVOLUTION_ANALYSIS.md`, and a `context/` folder. The requirement is for a richer package including `architecture.md`, `dependencies.md`, `execution-instructions.md`, etc.
4. **Context Selection Inconsistency**: File selection is evolved as a list of strings (`selected_files`), but the actual content inclusion is handled at the very end, sometimes falling back to static curation.
5. **Prompt Evolution**: While blueprints exist for different strategies, the actual prompt text is often synthesized at the end rather than being the primary unit of evolution.

## Proposed Refactoring

- **Formalize `MediationCandidate`**: Create a dedicated model to encapsulate all mediation artifacts.
- **Unit of Evolution**: Make `MediationCandidate` the primary object of evolution within `BranchVariant` during mediated mode.
- **Direct Export**: Ensure the final ZIP is assembled directly from the winning `MediationCandidate` artifacts without re-synthesis.
- **Rich Packaging**: Update `MediatedExportManager` to support the full set of required mediation files.
