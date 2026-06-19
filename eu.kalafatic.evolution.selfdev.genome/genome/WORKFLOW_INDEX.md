# WORKFLOW INDEX

## Core Evolution Workflow (Darwin Cycle)
The primary loop used to resolve engineering goals.

1.  **Entry Point**: `IterationManager.handle(TaskRequest)` receives the user prompt.
2.  **Intent Grounding**: `IntentExpansionEngine` resolves ambiguity and discovers semantic dimensions.
3.  **Trajectory Spawning**: `DarwinEngine` generates divergent blueprints.
4.  **Parallel Evaluation**: `DarwinFlow` tests variants in isolated Git worktrees.
5.  **Winner Selection**: `AuthorityEngine` selects the best variant based on fitness or user choice.
6.  **Implementation**: `TaskExecutor` applies the winning changes and verifies them.
7.  **lineage Persistence**: `IterationMemoryService` saves the result to the `EvolutionTree`.

## Mediated Export Workflow
Used when EVO acts as an architect for an external model.

1.  **Request Analysis**: `SelfDevRequestAnalyzer` identifies the goal.
2.  **Reality Discovery**: `TargetScanner` and `SemanticExtractor` map the project.
3.  **Context Curation**: `ContextCurator` selects the top 16 files.
4.  **Prompt Optimization**: `PromptSynthesizer` creates instructions.
5.  **Packaging**: `MediatedExportManager` creates a ZIP bundle.

## Self-Development Workflow
Used when EVO evolves its own source code.

1.  **Supervisor Start**: `SelfDevSupervisor` monitors the main server.
2.  **Autonomous Task**: Darwin proposes a change to an EVO bundle.
3.  **Self-Mutation**: The change is applied and the Maven build is triggered.
4.  **Restart**: `RestartManager` reloads the OSGi container to apply the new version.
5.  **Rollback**: If the server fails to boot, the Supervisor rolls back the Git state.

## Model Training Workflow (Forge)
1.  **Design**: User selects model type and layers in `forge.html`.
2.  **Grounding**: `DatasetController` indexes source code.
3.  **Training**: `NeuronEngine` simulates (soon: performs) tensor optimization.
4.  **Export**: `ForgeSessionManager` generates a GGUF artifact.
