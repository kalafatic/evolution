# Architectural Proposal: Evo Self-Development Mode

## 1. Overview & Vision
Self-Development Mode is an autonomous "closed-loop" evolution system where the Evo Platform improves its own source code. The vision is to enable **safe, repeatable, cross-project evolutionary improvement** by orchestrating three core pillars:
*   **SELFDEV-GENOME**: The intelligence and planning layer. It acts as an "Evolution Compiler" that understands architectural intent.
*   **SUPERVISOR**: The execution and safety layer. It manages the lifecycle, isolation, and rollbacks.
*   **DARWIN**: The selection pressure layer. It generates and evaluates variants to ensure only the fittest changes survive.

## 2. System Architecture: The Evolutionary Pipeline
The self-development process follows a strictly controlled pipeline:

1.  **SUPERVISOR (External)**: Initiates the cycle, prepares the environment, and monitors health.
2.  **PROJECT SNAPSHOT**: Captures the current state of the code and configuration.
3.  **SELFDEV-GENOME (Evolution Compiler)**: Analyzes the snapshot against the platform's "DNA" to identify gaps and planning opportunities.
4.  **UPGRADE PLAN**: Generates a deterministic set of proposed changes (Architectural & File-level).
5.  **DARWIN EVALUATION**: Spawns competitive variants based on the plan and evaluates them under pressure.
6.  **APPLY / REJECT / ROLLBACK**: The Supervisor commits successful changes or rolls back failures.

## 3. Initiation Flow (From Debug-RCP)
The process begins within the user's active Eclipse development environment (the "Debug-RCP").

### 3.1 Pre-Flight Checks
The `SelfDevBootstrapController` performs the following validations:
*   **Git**: Ensure a clean working directory, preferably on a dedicated `evo/self-dev` branch.
*   **Maven**: Verify build tool availability and valid project structure.
*   **LLM**: Ensure the model has sufficient context capacity and is reachable.
*   **Genome Integrity**: Verify `eu.kalafatic.evolution.selfdev.genome` artifacts are present.
*   **Permissions**: Verify write access to `self-dev-run/`.

### 3.2 Sandbox & Export
1.  **Target Folder Creation**: Initialize `self-dev-run/` as the sandbox.
2.  **Headless Export**: The Debug-RCP triggers a headless Maven build (`mvn clean package -DskipTests`) to export the worker artifact to `self-dev-run/target/`.
3.  **Bootstrap Generation**: Create `bootstrap.json` defining source and target paths.

## 4. Metadata-Driven Evolution
A key innovation in this architecture is the use of structured **Knowledge Metadata** to drive the evolutionary engine. This metadata (modeled in `KnowledgeMetadata.java`) enables:

*   **Rapid Context Selection**: Tags, keywords, and module associations allow the system to quickly pull relevant source files without scanning the entire repo.
*   **Semantic Search & Indexing**: Metadata provides the foundation for future vector indexing of the platform's capabilities.
*   **AI Context Compression**: By using summary levels and importance ratings, the system can condense vast architectural information into high-density prompts for the LLM.
*   **Mediated Mode Filtering**: Ensures that changes in one subsystem do not violate constraints defined in the "Forbidden Regions" of the metadata.
*   **Cross-Project Intelligence**: Allows the platform to "learn" patterns from one repository and apply them as upgrades to another via the `SecondhandUpgradeEngine`.

## 5. The Autonomous Loop (Supervisor & Worker)
### 5.1 Supervisor (The Guardian)
The external Supervisor monitors the Worker RCP via the file-based protocol:
*   **State Monitoring**: Reads `state.json` for progress.
*   **Safety Interlocks**: Triggers `git rollback` if the Worker crashes or the build fails.
*   **Handover Execution**: Applies `patch.json` and manages restarts.

### 5.2 Worker (The Mutator)
The Worker RCP uses the `SelfDevDarwinEngine` to:
1.  **Analyze**: Consults the `SelfDevGenomeHub` to align with architectural milestones.
2.  **Plan**: Uses the `SecondhandUpgradeEngine` to compile an `UpgradePlan`.
3.  **Execute**: Generates Darwinian variants to find the optimal implementation of the plan.

## 6. Communication Protocol (`self-dev-run/`)
*   **`bootstrap.json`**: Configuration and initial actions.
*   **`state.json`**: Real-time status (Phase, Progress).
*   **`command.json`**: Control signals (`BUILD_AND_RUN`, `RESTART`, `STOP`).
*   **`patch.json`**: The encoded code mutation.
*   **`control.json`**: User-driven overrides from the UI.

## 7. Monitoring & Visualization
The Debug-RCP provides a live view of the **Evolution Tree** and the **Genome Dashboard**. This allows developers to observe the platform's self-improvement, analyze fitness gains, and inspect the metadata-driven decisions made during each iteration.
