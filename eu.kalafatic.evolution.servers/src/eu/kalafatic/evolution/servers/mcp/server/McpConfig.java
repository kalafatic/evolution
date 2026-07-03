package eu.kalafatic.evolution.servers.mcp.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class McpConfig {
    private int port = 68080;
    private boolean enableLogging = true;
    private List<String> enabledConnectors;

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public boolean isEnableLogging() { return enableLogging; }
    public void setEnableLogging(boolean enableLogging) { this.enableLogging = enableLogging; }
    public List<String> getEnabledConnectors() { return enabledConnectors; }
    public void setEnabledConnectors(List<String> enabledConnectors) { this.enabledConnectors = enabledConnectors; }

    public static McpConfig load() {
        File configFile = new File("application.json");
        if (configFile.exists()) {
            try {
                return new ObjectMapper().readValue(configFile, McpConfig.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new McpConfig();
    }
}
