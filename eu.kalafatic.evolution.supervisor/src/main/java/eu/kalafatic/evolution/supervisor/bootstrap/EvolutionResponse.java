package eu.kalafatic.evolution.supervisor.bootstrap;

public class EvolutionResponse {
    private final boolean acknowledged;
    private final String message;
    private String evolutionId;

    public EvolutionResponse(boolean acknowledged, String message) {
        this.acknowledged = acknowledged;
        this.message = message;
    }

    public boolean isAcknowledged() {
        return acknowledged;
    }

    public String getMessage() {
        return message;
    }

    public String getEvolutionId() {
        return evolutionId;
    }

    public void setEvolutionId(String evolutionId) {
        this.evolutionId = evolutionId;
    }
}
