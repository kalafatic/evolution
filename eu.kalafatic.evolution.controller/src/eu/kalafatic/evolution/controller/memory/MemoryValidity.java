package eu.kalafatic.evolution.controller.memory;

/**
 * Validity state of a memory entry. Retraction and supersession are explicit
 * persisted states; they never delete the underlying record.
 */
public enum MemoryValidity {
    VALID,
    OUTDATED,
    SUPERSEDED,
    RETRACTED;

    public static MemoryValidity fromString(String value) {
        if (value == null) return VALID;
        try {
            return MemoryValidity.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return VALID;
        }
    }
}
