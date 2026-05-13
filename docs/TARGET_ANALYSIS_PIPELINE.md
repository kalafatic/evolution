# Target Analysis Pipeline

The mediated analysis pipeline consists of several iterative passes to evolve the kernel's understanding of a target.

## Pipeline Steps

1.  **Target Selection:** User selects a directory, file, or mixed workspace.
2.  **Surface Scanning:** Recursive scan to build a file model and detect technologies.
3.  **Semantic Extraction:** Lightweight analysis to identify entry points, interfaces, and components.
4.  **Architecture Inference:** Higher-level reasoning about the project's structure and patterns.
5.  **Refinement:** (Optional) User refinement of the discovered metadata.

## Observability

Each pass is recorded as a task in the orchestration model, allowing the user to observe the evolution of understanding in real-time.
