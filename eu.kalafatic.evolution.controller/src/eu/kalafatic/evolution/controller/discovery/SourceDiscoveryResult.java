package eu.kalafatic.evolution.controller.discovery;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SourceDiscoveryResult {
    private File workspaceRoot;
    private Set<File> gitRepositories = new HashSet<>();
    private Set<File> projectRoots = new HashSet<>();
    private List<SourceFileDescriptor> files = new ArrayList<>();
    private Map<String, Integer> statistics = new HashMap<>();
    private double confidence = 0.0;
    private File primaryRepository;

    public File getWorkspaceRoot() { return workspaceRoot; }
    public void setWorkspaceRoot(File workspaceRoot) { this.workspaceRoot = workspaceRoot; }

    public Set<File> getGitRepositories() { return gitRepositories; }
    public void setGitRepositories(Set<File> gitRepositories) { this.gitRepositories = gitRepositories; }

    public Set<File> getProjectRoots() { return projectRoots; }
    public void setProjectRoots(Set<File> projectRoots) { this.projectRoots = projectRoots; }

    public List<SourceFileDescriptor> getFiles() { return files; }
    public void setFiles(List<SourceFileDescriptor> files) { this.files = files; }

    public Map<String, Integer> getStatistics() { return statistics; }
    public void setStatistics(Map<String, Integer> statistics) { this.statistics = statistics; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public File getPrimaryRepository() { return primaryRepository; }
    public void setPrimaryRepository(File primaryRepository) { this.primaryRepository = primaryRepository; }
}
