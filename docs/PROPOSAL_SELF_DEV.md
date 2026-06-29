# Architectural Proposal: Evo Self-Development Mode

## 1. Overview
Self-Development Mode is an autonomous "closed-loop" evolution system where the Evo Platform improves its own source code. To ensure safety and stability, this process is managed by an external **Supervisor** process that oversees a **Worker RCP** instance. The system leverages the **Evolution Genome** to maintain architectural consistency and self-awareness.

## 2. Initiation Flow (From Debug-RCP)
The process begins within the user's active Eclipse development environment (the "Debug-RCP").

### 2.1 Pre-Flight Checks
Before initiation, the `SelfDevBootstrapController` performs the following validations:
*   **Git**: Ensure the project is a Git repository, preferably on a dedicated `evo/self-dev` branch, with a clean working directory.
*   **Maven**: Verify `mvn` is accessible and the project structure is valid for building.
*   **LLM**: Check that the configured LLM (Ollama/OpenAI/etc.) is reachable and has sufficient context capacity for the platform's core source files.
*   **Genome Check**: Verify the presence and integrity of the `eu.kalafatic.evolution.selfdev.genome` artifacts.
*   **Permissions**: Verify write access to the `self-dev-run/` target directory.

### 2.2 Sandbox & Export
1.  **Target Folder Creation**: Initialize `self-dev-run/` as the sandbox for the autonomous loop.
2.  **Headless Export**: The Debug-RCP triggers a headless Maven build (`mvn clean package -DskipTests`) to export the current source into a runnable worker artifact (JAR) located in `self-dev-run/target/`.
3.  **Bootstrap Generation**: Create `bootstrap.json` in the run directory, defining the `sourcePath` (actual source) and `targetPath` (the sandbox workspace).

## 3. External Supervisor Initialization
The Debug-RCP launches the `eu.kalafatic.evolution.supervisor` as an independent JVM process.
*   Once launched, the Debug-RCP enters a **Monitoring State**.
*   The Supervisor takes ownership of the execution loop.

## 4. The Autonomous Evolutionary Loop
The Supervisor manages the lifecycle of the Worker RCP through a file-based protocol.

### 4.1 Step 1: Start Worker
Supervisor launches the Worker RCP using the exported artifact with the following flags:
*   `--mode=SELF_DEV`
*   `--state=self-dev-run/state.json`

### 4.2 Step 2: Evolution (Worker Internal)
The Worker RCP, using `SelfDevDarwinEngine`, performs:
1.  **Genome Analysis**: Utilizes `SelfDevGenomeHub` to retrieve the current architectural blueprints (`genome/` directory) and identify "Knowledge Gaps" or alignment issues.
2.  **Mutation**: Generates a code change (diff) guided by the `SecondhandUpgradeEngine` to ensure architectural integrity.
3.  **Proposal**: Writes a `patch.json` and sets `action: RESTART` in `command.json`.
4.  **Exit**: The Worker RCP shuts down voluntarily to allow the Supervisor to apply changes.

### 4.3 Step 3: Mutation & Rebuild (Supervisor)
1.  **Patch**: Supervisor applies the diff using `git apply`.
2.  **Build**: Supervisor runs `mvn clean package`.
3.  **Stability Check**: If the build fails, the Supervisor performs a `git rollback` and logs the failure.

### 4.4 Step 4: Verification & Genome Update (Worker vNext)
1.  **Restart**: Supervisor restarts the Worker RCP (now running the mutated code).
2.  **Self-Verification**: The Worker RCP runs internal integration tests.
3.  **Genome Update**: On success, the `MilestoneGenerator` is triggered to update the `genome/` repository with the new architectural state.
4.  **Commit**: The Worker signals `SUCCESS` via the protocol, and the Supervisor performs a Git commit of the changes.

## 5. Communication Protocol (`self-dev-run/`)
*   **`bootstrap.json`**: Initial configuration (paths, actions).
*   **`state.json`**: Real-time status (Phase, Iteration, Progress).
*   **`command.json`**: Handover signals (`BUILD_AND_RUN`, `RESTART`, `STOP`).
*   **`patch.json`**: The code mutation diff.
*   **`control.json`**: Manual overrides from the Debug-RCP UI (e.g., Force Stop).

## 6. Monitoring and UI
The original Debug-RCP monitors the `self-dev-run/` folder and updates the AI Chat and Evolution Tree views in real-time. It provides a specialized view for the **Genome Dashboard**, showing how the platform's "DNA" is evolving over time.
