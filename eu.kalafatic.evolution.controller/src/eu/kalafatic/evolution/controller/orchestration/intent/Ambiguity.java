package eu.kalafatic.evolution.controller.orchestration.intent;

/**
 * Represents an ambiguity in the user request.
 */
public class Ambiguity {
    private final String part;
    private final String reason;

    public Ambiguity(String part, String reason) {
        this.part = part;
        this.reason = reason;
    }

    public String getPart() {
        return part;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return "Part: '" + part + "', Reason: " + reason;
    }
}
