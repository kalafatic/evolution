package eu.kalafatic.evolution.servers.mcp.model;

import java.util.Map;

public class Implementation {
    private String name;
    private String version;

    public Implementation() {}
    public Implementation(String name, String version) {
        this.name = name;
        this.version = version;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
}
