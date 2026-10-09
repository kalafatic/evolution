package eu.kalafatic.evolution.controller.manager.removal;

public enum ModelRemovalStatus {
    SUCCESS("Model was completely removed."),
    PARTIAL_SUCCESS("Model was partially removed (e.g. runtime unregistered, but local file cleanup failed)."),
    FAILED("Model removal failed."),
    UNSUPPORTED("Model removal operation is unsupported for this provider/model type.");

    private final String description;

    ModelRemovalStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
