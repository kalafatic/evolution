package eu.kalafatic.evolution.forge.data.api.preference;

/**
 * Classification level for user training preferences.
 */
public enum RequirementLevel {
    /**
     * Hard requirement that must be satisfied for dataset acquisition to succeed.
     */
    HARD,

    /**
     * Soft preference influencing source ranking and composition balance.
     */
    SOFT,

    /**
     * Optional objective optimized if resources allow.
     */
    OPTIONAL
}
