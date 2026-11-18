package eu.kalafatic.evolution.controller.memory;

/**
 * Distinguishes the epistemic status of a memory entry. Historical
 * information must never be presented as the current state.
 */
public enum MemoryInformationStatus {
    /** Verified or user-confirmed current information. */
    CONFIRMED,
    /** True at the time of recording but possibly no longer current. */
    HISTORICAL,
    /** Derived from observed behavior; never binding on its own. */
    INFERRED,
    /** Unverified hypothesis or model-generated assumption. */
    ASSUMPTION,
    /** Valid only within a limited session or time window. */
    TEMPORARY;

    public static MemoryInformationStatus fromString(String value) {
        if (value == null) return CONFIRMED;
        try {
            return MemoryInformationStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return CONFIRMED;
        }
    }
}
