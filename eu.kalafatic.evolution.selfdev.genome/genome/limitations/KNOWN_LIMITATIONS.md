# KNOWN LIMITATIONS

## 1. Polling Latency
Current inter-process communication (between Kernel and Supervisor) and UI updates rely on file-system or HTTP polling. This introduces a ~2s delay in every transition, significantly slowing down the evolution loop.

## 2. Simulated Training (Forge)
The Forge subsystem provides the design and visualization for model training, but the actual tensor math is currently mocked. It cannot yet produce functional trained neural networks beyond the demo models.

## 3. Maven/Tycho Build Heavyweight
Running a full Maven build for every Darwin variant is extremely slow in large projects. The platform lacks an incremental, "Hot-Reload" verification system for Java.

## 4. Context Window Token Pressure
Long-running evolution sessions (10+ iterations) accumulate history that eventually exceeds the LLM's context window. Current pruning strategies are basic and may lose important "Lessons Learned" from early iterations.

## 5. Mono-Agent Bottleneck
While EVO has many specialized agents, they are mostly coordinated in a serial fashion by the `IterationManager`. Parallel multi-agent consensus (where multiple agents debate a solution) is not yet fully implemented.
