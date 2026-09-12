package eu.kalafatic.evolution.controller.orchestration.design;

/**
 * @evo:19:A reason=dynamic-design-model
 */
public class ComponentRecord {
    private String id;
    private String name;
    private String type;
    private String description;
    private String path;
    private double importanceScore;
    private int x;
    private int y;
    private String parentId;
    private int level = 2;
    private String qualifiedName;
    private String superClass;
    private boolean expanded = false;
    private int incomingCount = 0;
    private int outgoingCount = 0;
    private java.util.List<String> interfaces = new java.util.ArrayList<>();
    private java.util.List<String> fields = new java.util.ArrayList<>();
    private java.util.List<String> properties = new java.util.ArrayList<>();
    private java.util.List<String> methods = new java.util.ArrayList<>();
    private java.util.List<String> keyClasses = new java.util.ArrayList<>();
    private java.util.List<String> useCases = new java.util.ArrayList<>();
    private java.util.List<String> dependencies = new java.util.ArrayList<>();
    private java.util.List<ComponentRecord> childNodes = new java.util.ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public double getImportanceScore() { return importanceScore; }
    public void setImportanceScore(double importanceScore) { this.importanceScore = importanceScore; }

    public int getX() { return x; }
    public void setX(int x) { this.x = x; }

    public int getY() { return y; }
    public void setY(int y) { this.y = y; }

    public java.util.List<String> getProperties() { return properties; }
    public void setProperties(java.util.List<String> properties) { this.properties = properties; }

    public java.util.List<String> getMethods() { return methods; }
    public void setMethods(java.util.List<String> methods) { this.methods = methods; }

    public java.util.List<String> getKeyClasses() { return keyClasses; }
    public void setKeyClasses(java.util.List<String> keyClasses) { this.keyClasses = keyClasses; }

    public java.util.List<String> getUseCases() { return useCases; }
    public void setUseCases(java.util.List<String> useCases) { this.useCases = useCases; }

    public java.util.List<String> getDependencies() { return dependencies; }
    public void setDependencies(java.util.List<String> dependencies) { this.dependencies = dependencies; }

    public java.util.List<ComponentRecord> getChildNodes() { return childNodes; }
    public void setChildNodes(java.util.List<ComponentRecord> childNodes) { this.childNodes = childNodes; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public String getQualifiedName() { return qualifiedName; }
    public void setQualifiedName(String qualifiedName) { this.qualifiedName = qualifiedName; }

    public String getSuperClass() { return superClass; }
    public void setSuperClass(String superClass) { this.superClass = superClass; }

    public boolean isExpanded() { return expanded; }
    public void setExpanded(boolean expanded) { this.expanded = expanded; }

    public int getIncomingCount() { return incomingCount; }
    public void setIncomingCount(int incomingCount) { this.incomingCount = incomingCount; }

    public int getOutgoingCount() { return outgoingCount; }
    public void setOutgoingCount(int outgoingCount) { this.outgoingCount = outgoingCount; }

    public java.util.List<String> getInterfaces() { return interfaces; }
    public void setInterfaces(java.util.List<String> interfaces) { this.interfaces = interfaces; }

    public java.util.List<String> getFields() { return fields; }
    public void setFields(java.util.List<String> fields) { this.fields = fields; }
}
