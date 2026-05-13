# Context Curation

Context Curation is the process of selecting a minimal, high-signal set of artifacts to provide to an external LLM.

## Selection Strategy

- **Anchors:** Key architectural files like `pom.xml` or `package.json`.
- **Entry Points:** Files containing `main` methods or core controllers.
- **Structural Models:** Important interfaces and base classes.
- **Semantic Clusters:** Files with high density of technical markers.

## Constraints

- Avoid token floods by limiting the number of selected files.
- Prioritize architectural relevance over raw size.
