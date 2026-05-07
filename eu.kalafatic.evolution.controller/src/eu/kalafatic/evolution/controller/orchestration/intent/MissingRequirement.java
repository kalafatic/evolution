package eu.kalafatic.evolution.controller.orchestration.intent;

/**
 * Represents a missing piece of information in the user request.
 */
public class MissingRequirement {
    private final String field;
    private final String description;

    public MissingRequirement(String field, String description) {
        this.field = field;
        this.description = description;
    }

    public String getField() {
        return field;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return field + ": " + description;
    }
}
