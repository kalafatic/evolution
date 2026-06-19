# Forge: Neural Laboratory

## Overview
The Forge is a specialized subsystem within EVO dedicated to designing, training, and exporting domain-specific AI models. It integrates model development directly into the software engineering workflow.

## Current Maturity
- **Maturity Level**: 1 (Simulated)
- **Status**: The UI and orchestration logic are fully implemented, but actual tensor calculations are currently mocked/simulated.

## Architectural Vision
Forge is designed to bridge the gap between "consuming AI" and "manufacturing AI." It allows developers to:
1.  **Define Model Architecture**: Select layers (MLP, CNN, Transformer), activation functions, and hyperparameters.
2.  **Import Datasets**: Automatically index source code or documentation as training data.
3.  **Train Locally**: Run training loops within the JVM using hardware acceleration (planned).
4.  **Export GGUF**: Package the trained model for immediate use by providers like Ollama.

## Key Components

### ForgeSessionManager
Coordinates the training lifecycle. It manages `ForgeSession` objects which track dataset bindings, hyperparameters, and progress.
- **Location**: `eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager`

### NeuronEngine
The tensor calculation core. Currently provides a deterministic simulation of model training outcomes.
- **Location**: `eu.kalafatic.evolution.forge.controller`

### DatasetController
Handles semantic indexing of training data.
- **Related Events**: `FORGE_DATASET_IMPORTED`

## E2E Demo Flow
The platform supports a simulated end-to-end demo via `runE2EDemo`.
1.  Initialize architecture.
2.  Import dataset.
3.  Simulate training progress (loss/perplexity curves).
4.  Export demo model to `./forge-lab/forge-model/src/main/resources/model/demo/`.

## Future Milestones
- **Milestone 1: The Realized Neuron**: JNI integration with a high-performance tensor backend (e.g., libtorch, OnnxRuntime).
- **NAS (Neural Architecture Search)**: Using the Darwin engine to evolve the *architecture* of forged models.

## Related
- [ARCHITECTURE.md](../architecture/ARCHITECTURE.md)
- [ROADMAP.md](../roadmap/ROADMAP.md)
