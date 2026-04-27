package eu.kalafatic.evolution.controller.orchestration.design;

/**
 * @evo:19:A reason=dynamic-design-model
 */
public class RelationshipRecord {
    private String from;
    private String to;
    private String type;

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }
    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
