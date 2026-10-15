package eu.kalafatic.evolution.controller.discovery;

import java.util.regex.Pattern;

public class SourceDiscoveryRequest {
    private Pattern includeExtensions;
    private Pattern pathFilter;
    private int maxFiles = 50000;
    private boolean fastMode = false;

    public Pattern getIncludeExtensions() { return includeExtensions; }
    public void setIncludeExtensions(Pattern includeExtensions) { this.includeExtensions = includeExtensions; }

    public Pattern getPathFilter() { return pathFilter; }
    public void setPathFilter(Pattern pathFilter) { this.pathFilter = pathFilter; }

    public int getMaxFiles() { return maxFiles; }
    public void setMaxFiles(int maxFiles) { this.maxFiles = maxFiles; }

    public boolean isFastMode() { return fastMode; }
    public void setFastMode(boolean fastMode) { this.fastMode = fastMode; }
}
