package eu.kalafatic.evolution.forge.data.api;

/**
 * Enumeration of supported internal training sample types.
 */
public enum TrainingSampleType {
    TEXT,
    INSTRUCTION,
    CHAT,
    QA,
    CODE,
    REASONING,
    CLASSIFICATION;

    public static TrainingSampleType fromString(String typeStr) {
        if (typeStr == null || typeStr.trim().isEmpty()) {
            return TEXT;
        }
        try {
            return TrainingSampleType.valueOf(typeStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return TEXT;
        }
    }
}
