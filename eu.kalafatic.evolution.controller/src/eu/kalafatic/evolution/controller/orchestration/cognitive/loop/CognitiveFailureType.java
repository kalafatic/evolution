package eu.kalafatic.evolution.controller.orchestration.cognitive.loop;

/**
 * Categorization of outcome failures in the cognitive layer for structured strategy reasoning.
 */
public enum CognitiveFailureType {
    TARGET_REACHED(false, false),
    SOURCE_EXHAUSTED(true, false),
    SOURCE_EMPTY(true, false),
    SOURCE_INACCESSIBLE(true, false),
    NETWORK_FAILURE(true, true),
    AUTHENTICATION_FAILURE(true, false),
    PARSER_FAILURE(true, false),
    FILTER_REJECTED_ALL(true, false),
    SIZE_LIMIT_REACHED(true, false),
    INVALID_CONFIGURATION(true, false),
    CAPABILITY_FAILURE(true, false),
    UNKNOWN_FAILURE(true, false);

    private final boolean failure;
    private final boolean transientFailure;

    CognitiveFailureType(boolean failure, boolean transientFailure) {
        this.failure = failure;
        this.transientFailure = transientFailure;
    }

    public boolean isFailure() {
        return failure;
    }

    public boolean isTransient() {
        return transientFailure;
    }

    public static CognitiveFailureType fromString(String name) {
        if (name == null || name.isBlank()) {
            return UNKNOWN_FAILURE;
        }
        for (CognitiveFailureType type : values()) {
            if (type.name().equalsIgnoreCase(name.trim())) {
                return type;
            }
        }
        if (name.contains("NETWORK") || name.contains("TIMEOUT") || name.contains("CONNECT")) {
            return NETWORK_FAILURE;
        }
        if (name.contains("EXHAUSTED")) {
            return SOURCE_EXHAUSTED;
        }
        if (name.contains("EMPTY")) {
            return SOURCE_EMPTY;
        }
        return UNKNOWN_FAILURE;
    }
}
