package eu.kalafatic.evolution.controller.discovery;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import java.util.regex.Pattern;

public class SourceDiscoveryRequest {
    private Pattern includeExtensions;
    private Pattern pathFilter;
    private int maxFiles = 50000;

    public Pattern getIncludeExtensions() { return includeExtensions; }
    public void setIncludeExtensions(Pattern includeExtensions) { this.includeExtensions = includeExtensions; }

    public Pattern getPathFilter() { return pathFilter; }
    public void setPathFilter(Pattern pathFilter) { this.pathFilter = pathFilter; }

    public int getMaxFiles() { return maxFiles; }
    public void setMaxFiles(int maxFiles) { this.maxFiles = maxFiles; }
}
