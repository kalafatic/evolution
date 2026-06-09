package eu.kalafatic.evolution.selfdev.genome.core;

import java.util.Map;

public class ProjectSnapshot {
    private String projectName;
    private String rootPath;
    private Map<String, String> fileContents;

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public Map<String, String> getFileContents() {
        return fileContents;
    }

    public void setFileContents(Map<String, String> fileContents) {
        this.fileContents = fileContents;
    }
}
