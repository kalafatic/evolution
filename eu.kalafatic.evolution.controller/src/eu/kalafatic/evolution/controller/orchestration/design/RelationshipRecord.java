package eu.kalafatic.evolution.controller.orchestration.design;

/**
 * @evo:19:A reason=dynamic-design-model
 */
public class RelationshipRecord {
    private String from;
    private String to;
    private String type;
    private int count = 1;
    private java.util.List<String> details = new java.util.ArrayList<>();

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }
    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }

    public java.util.List<String> getDetails() { return details; }
    public void setDetails(java.util.List<String> details) { this.details = details; }
}
