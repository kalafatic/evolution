# Hybrid Mode: Intelligent Context & Reasoning Architecture

## 1. The Challenge of Small Local Models
Large Language Models (LLMs) running locally (e.g., via Ollama) are excellent for privacy and cost-efficiency. However, models that fit on consumer hardware (7B to 14B parameters) often struggle with:
*   **Context Window Pressure**: Processing thousands of lines of code accurately.
*   **Complex Reasoning**: Implementing multi-file refactorings or deep architectural changes.
*   **Precision**: Following strict machine-readable formats (JSON) without hallucination.

## 2. The Hybrid Solution: Split Intelligence
The Evolution Platform implements a **Hybrid Mode** that decouples "Context Gathering" from "Deep Reasoning."

### Phase 1: Local Context Building (The Librarian)
*   **Agent**: `LlmRouter` + `ContextBuilder`
*   **Provider**: Local Ollama (Small Model)
*   **Process**:
    1.  The local model performs a deterministic scan of the project structure.
    2.  It selects only the most relevant files, classes, and method signatures related to the current task.
    3.  It builds a structured `ContextPackage` (JSON) containing minimal but high-density information.
*   **Benefit**: This ensures that no sensitive data (beyond what is strictly necessary) leaves the local environment, and it reduces the token count for the next phase.

### Phase 2: Cloud Reasoning (The Architect)
*   **Agent**: `JavaDevAgent`, `ArchitectAgent`, etc.
*   **Provider**: Remote LLM (e.g., DeepSeek, GPT-4, Gemini)
*   **Process**:
    1.  The structured `ContextPackage` is sent to a powerful remote model.
    2.  The remote model, unburdened by irrelevant noise, performs high-precision reasoning and generates the implementation or plan.
*   **Benefit**: Leverages the "Big Brain" of massive models for the actual implementation work, where precision is non-negotiable.

### Phase 3: Local Verification (The Guard)
*   **Agent**: `ReviewerAgent` / `ConstraintAgent`
*   **Provider**: Local Ollama
*   **Process**:
    1.  The result from the cloud is returned to the local environment.
    2.  The local model performs a quick safety and format check (Verification).
    3.  It ensures the response aligns with local architectural guardrails before application.

## 3. Why This Works
By using the local model as a "Librarian" to filter and structure information, we feed the cloud model a "Technical Brief" rather than a "Dump of Files." This hybrid synergy allows the system to achieve results that neither a small local model nor a "blind" cloud model could achieve alone.

## 4. Resilience & Fallback
The `LlmRouter` is designed with **Automatic Resilient Fallback**. If Phase 2 (Cloud Reasoning) fails due to network issues or API limits, the system automatically attempts to complete the task using the local model in `LOCAL` mode, ensuring that the development workflow is never fully blocked.
