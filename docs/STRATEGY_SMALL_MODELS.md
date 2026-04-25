# Strategy: High-Performance AI Systems with Small Local Models

## Executive Summary
Building powerful AI systems—such as coding assistants or autonomous development platforms—does not require massive, multi-billion parameter models if the underlying system architecture is robust. By treating the Large Language Model (LLM) as a specialized execution component rather than a "brain," and by shifting the burden of logic to a structured orchestration layer, developers can achieve complex, high-reliability results using small, resource-constrained local models.

## Key Principles
*   **Architecture over Intelligence:** The system's strength comes from its design, not the raw reasoning power of the model.
*   **Separation of Concerns:** Explicitly decouple intent detection (routing) from task execution (implementation).
*   **The "Business Analyst" Model:** Treat the LLM as a requirements analyst that must validate inputs before acting.
*   **Mandatory Clarification:** Force models to **ASK** for missing information instead of hallucinating missing data or guessing user intent.
*   **Structural Constraint:** Use strict, machine-readable output formats (e.g., JSON) to minimize parsing fragility and enforce logic flow.
*   **Atomic Modularity:** Break complex requests into discrete, manageable phases (Clarify → Design → Generate).

## System Architecture Overview

### 1. The Multi-Mode Router
Small models struggle with abstract, multi-purpose prompts. The architecture must implement a specialized router to detect the operational mode (e.g., Chat, Coding, Darwin/Iterative, Self-Development) and dispatch the request to a domain-specific agent with a narrowed scope.

### 2. Intent Detection & Validation (The "Gate")
Before any execution occurs, a specialized "Intent Gate" analyzes the request against a structured requirement schema.
*   **Input:** User prompt + System Context (Shared Memory).
*   **Process:** The model checks for critical parameters (e.g., file paths, language, specific commands).
*   **Output:** A JSON object with two primary keys: `ASK` (for missing details) or `GENERATE` (for actionable plans).

### 3. Phased Execution Pipeline
Complexity is managed by dividing the workflow into distinct stages:
1.  **Clarification Phase:** Ensures the project scope is fully defined.
2.  **Design Phase:** Generates a blueprint or a list of atomic tasks (The Plan).
3.  **Generation Phase:** Executes individual tasks (File writes, Git commits, Maven builds) using specialized, narrow-context agents.

### 4. General Engine, Specific Schemas
Maintain a single, general-purpose orchestration engine that handles task lifecycle, state, and tool integration (Git, Shell, etc.). Use task-specific schemas to define how different types of work (e.g., "Refactor Code" vs. "Write Documentation") should be processed by the model.

## Practical Implementation Tips

### Use Strict Output Schemas
Instead of asking for "a plan," provide a JSON schema. Small models perform significantly better when filling in a template than when generating free-form text.
```json
{
  "status": "CLARIFICATION_REQUIRED",
  "questions": ["Which directory should the tests be created in?"],
  "tentative_plan": []
}
```

### Implement Hybrid Optimization
When local models are too weak for a specific reasoning task, use a "Hybrid" approach:
1.  **Local Refinement:** Use a small model to clean up and structure the user's prompt.
2.  **Remote Execution:** Send the structured prompt to a larger model for the heavy lifting.
3.  **Local Simplification:** Use the small model to translate the complex response back into a user-friendly format.

### Force Iterative Refinement
Design the system to present the generated plan to the user for approval before execution. This "Human-in-the-loop" step mitigates the risks associated with small model errors and allows for iterative refinement of the requirements.

### Scalability Beyond Coding
The requirement-analysis approach is domain-agnostic. By changing the schemas and toolsets (e.g., replacing Maven with a Research API), the same architecture can power AI assistants for legal, medical, or administrative domains.
