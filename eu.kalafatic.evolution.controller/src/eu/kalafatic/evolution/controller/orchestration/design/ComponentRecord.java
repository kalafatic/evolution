package eu.kalafatic.evolution.controller.orchestration.design;

/**
 * @evo:19:A reason=dynamic-design-model
 */
public class ComponentRecord {
    private String name;
    private String type;
    private int x;
    private int y;
    private java.util.List<String> properties = new java.util.ArrayList<>();
    private java.util.List<String> methods = new java.util.ArrayList<>();

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    public java.util.List<String> getProperties() { return properties; }
    public void setProperties(java.util.List<String> properties) { this.properties = properties; }
    public java.util.List<String> getMethods() { return methods; }
    public void setMethods(java.util.List<String> methods) { this.methods = methods; }
}
