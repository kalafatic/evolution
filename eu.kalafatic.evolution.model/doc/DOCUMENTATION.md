# AI Evolution: Project Wizard and Use Case Documentation

## Overview
This document provides a comprehensive guide to the **New Evo Project Wizard** and the core **Use Cases** of the AI Evolution platform. It is designed to be both human-readable and structured enough to serve as input for other AI tools.

---

## 1. New Evo Project Wizard Scenarios

The `NewEvoProjectWizard` provides a flexible way to bootstrap an AI Evolution project. Depending on the user's needs, various integration points can be configured or skipped.

### Scenario A: Full Lifecycle Integration
- **Goal**: Establish a complete automated environment.
- **Components**: Git (Source), Maven (Build), LLM (Cloud AI), Ollama (Local AI), Agents (Logic).
- **Result**: A project ready for autonomous feature development and validation.

### Scenario B: Local-First Privacy Focus
- **Goal**: Keep all AI processing and source code on-premises.
- **Components**: Git (Local Repo), Ollama (Local LLM).
- **Result**: A secure, offline-capable environment using local models.

### Scenario C: Cloud AI Chat Hub
- **Goal**: Rapid prototyping via interactive chat.
- **Components**: LLM and AiChat (configured with high-capacity cloud providers).
- **Result**: Focused on the AI Output view and direct interaction.

### Scenario D: Neural Network Specialist
- **Goal**: Tackle complex technical tasks using non-transformer architectures.
- **Components**: Neuron AI (MLP, CNN, RNN, etc.).
- **Result**: Specialized agents capable of leveraging the `NeuronEngine`.

### Scenario E: Minimalist Starting Point
- **Goal**: Create the project structure and Evolution Nature without immediate config.
- **Components**: All optional pages skipped.
- **Result**: A blank canvas for manual orchestration definition.

---

## 2. Core Use Cases

### Autonomous Agentic Workflow
Agents like `coder` and `critic` collaborate on tasks. The `Orchestrator` manages their lifecycles and the flow of information between them using a structured `Task` hierarchy.

### End-to-End CI/CD Automation
The platform automatically pulls code from Git, assigns tasks to agents for implementation or review, verifies the changes using Maven, and commits the results.

### Specialized Neuron Processing
For tasks where traditional LLMs might struggle (e.g., specific signal analysis), agents delegate to Neuron AI models. The `NeuronEngine` provides local simulated implementations for these architectures.

### Hybrid Inference
Optimization of cost and privacy by routing simple tasks to a local Ollama instance while utilizing remote LLMs for complex reasoning or orchestration.

---

## 3. Structural Metadata
For machine consumption, refer to the following JSON files in this directory:
- `wizard_scenarios.json`: Detailed branching logic for project setup.
- `use_cases.json`: Functional workflows and component mappings.
- `evolution_api_metadata.json`: Technical mapping of the Ecore model and plugin extensions.
