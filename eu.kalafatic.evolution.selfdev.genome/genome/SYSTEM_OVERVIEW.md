# SYSTEM OVERVIEW

## EVO: Autonomous Evolutionary Development

EVO is an OS-like kernel for software evolution. It treats code not as a static artifact, but as a living organism that evolves through mutation (variant generation) and selection (fitness evaluation).

## Key Characteristics

### 1. Multi-Trajectory Reasoning
Unlike traditional AI assistants that propose a single solution, EVO's Darwin Engine spawns multiple divergent strategies (e.g., "Performance-Focused" vs. "Maintainability-Focused") and tests them against real-world metrics.

### 2. Physical Verification Loop
EVO doesn't just "guess" if code works. It uses Git worktrees to isolate variants, runs Maven builds to check compilation, and executes unit tests to verify behavior. Only successful variants survive.

### 3. Intelligence Isolation
The kernel is decoupled from the intelligence source. It can use local models (Ollama), remote APIs (OpenAI), or act as a "Mediated Architect" for external humans/LLMs.

### 4. Recursive Refinement
The system iterates through 5 phases (Intent -> Architecture -> Refinement -> Planning -> Synthesis), refining its understanding and its solution at every step.

## Domain Model
- **Orchestration**: The control plane (IterationManager).
- **Evolution**: The cognitive heart (DarwinEngine).
- **Mediation**: The semantic bridge (ContextCurator).
- **Infrastructure**: The physical hands (GitTool, MavenTool).
- **Persistence**: The evolutionary memory (Genome, EvolutionTree).
