package eu.kalafatic.evolution.creatic.model;

public class GuidanceAction {
    private String label;
    private String actionId;
    private String description;

    public GuidanceAction(String label, String actionId, String description) {
        this.label = label;
        this.actionId = actionId;
        this.description = description;
    }

    public String getLabel() { return label; }
    public String getActionId() { return actionId; }
    public String getDescription() { return description; }
}
