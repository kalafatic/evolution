package eu.kalafatic.evolution.controller.memory;

/**
 * Identifies the origin or provenance of a memory entry.
 */
public enum MemorySource {
    USER,
    SESSION,
    INFERENCE,
    TOOL,
    GIT,
    BUILD,
    TEST,
    LOG,
    DOCUMENT,
    TRAINING,
    SYSTEM
}
