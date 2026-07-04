# Evo Forge Agent

Autonomous agent for creating project-specific LLMs named **"evo"**.

## Architecture

1.  **Smart Scanner**: Recursively scans the codebase while respecting `.gitignore`.
2.  **Data Forge Engine**: Converts raw project data into JSONL datasets for pre-training and instruction tuning.
3.  **AI Data Enhancer**: Uses a base LLM (via Ollama) to generate high-quality synthetic instructions based on the project code.
4.  **Training Orchestrator**: Manages the end-to-end pipeline and generates configurations for Unsloth/Axolotl.
5.  **Model Exporter**: Merges adapters, converts to GGUF, and creates Ollama manifests.

## Prerequisites

-   Java 21
-   Ollama
-   Python 3.10+ (for fine-tuning)

## Usage

### 1. Build the Agent
```bash
mvn clean package -pl eu.kalafatic.evolution.forge.agent -am
```

### 2. Run the Forge Pipeline
Use the one-click command to start the data scanning and enhancement process:
```bash
./forge-evo --project-path .
```
Access the progress dashboard at `http://localhost:58081`.

### 3. Fine-Tune (Python)
Once data is prepared in `./evo-forge-data`, run the training script:
```bash
pip install -r eu.kalafatic.evolution.forge.agent/src/main/resources/requirements.txt
python3 eu.kalafatic.evolution.forge.agent/src/main/resources/train.py
```

### 4. Export to Ollama
```bash
./evo-forge-data/export_model.sh
```

## Configuration
Training parameters can be adjusted in `axolotl_config.yaml` located in the agent's resources or generated output directory.
