package eu.kalafatic.evolution.controller.memory;

/**
 * Defines the priority/importance level of a memory entry.
 */
public enum MemoryImportance {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);

    private final int level;

    MemoryImportance(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}
