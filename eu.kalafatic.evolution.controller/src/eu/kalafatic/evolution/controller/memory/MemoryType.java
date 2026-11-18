package eu.kalafatic.evolution.controller.memory;

/**
 * Represents the semantic type of stored knowledge.
 * Legacy values are preserved for compatibility; the unified cognitive model
 * adds the six canonical types below.
 */
public enum MemoryType {
    FACT,
    PREFERENCE,
    DECISION,
    RULE,
    CONSTRAINT,
    GOAL,
    PROJECT_CONTEXT,
    OBSERVATION,
    EXPERIENCE,
    SOLUTION,
    // Unified cognitive memory types
    WORKING,
    EPISODIC,
    SEMANTIC,
    PROCEDURAL,
    PROJECT,
    RELATIONAL;

    /** True for the six canonical cognitive types of the unified memory model. */
    public boolean isCognitiveType() {
        return this == WORKING || this == EPISODIC || this == SEMANTIC
                || this == PROCEDURAL || this == PROJECT || this == RELATIONAL;
    }
}
