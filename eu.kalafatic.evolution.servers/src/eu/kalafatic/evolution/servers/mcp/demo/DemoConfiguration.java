package eu.kalafatic.evolution.servers.mcp.demo;

public class DemoConfiguration {
    private String host = "localhost";
    private int port = 38080;
    private String docsFolder = "docs";
    private boolean enabled = true;

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getDocsFolder() { return docsFolder; }
    public void setDocsFolder(String docsFolder) { this.docsFolder = docsFolder; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
