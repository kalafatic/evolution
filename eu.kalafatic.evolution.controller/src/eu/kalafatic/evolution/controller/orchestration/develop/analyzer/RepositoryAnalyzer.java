package eu.kalafatic.evolution.controller.orchestration.develop.analyzer;

import java.io.File;
import java.util.List;

/**
 * Repository analysis interface for build system and project structure discovery.
 */
public interface RepositoryAnalyzer {

    ProjectAnalysis analyze(File repositoryDir);

    class ProjectAnalysis {
        private String projectType = "UNKNOWN";
        private String buildSystem = "UNKNOWN";
        private List<String> languages;
        private List<String> buildFiles;
        private boolean hasTests;
        private String summary;

        public String getProjectType() {
            return projectType;
        }

        public void setProjectType(String projectType) {
            this.projectType = projectType;
        }

        public String getBuildSystem() {
            return buildSystem;
        }

        public void setBuildSystem(String buildSystem) {
            this.buildSystem = buildSystem;
        }

        public List<String> getLanguages() {
            return languages;
        }

        public void setLanguages(List<String> languages) {
            this.languages = languages;
        }

        public List<String> getBuildFiles() {
            return buildFiles;
        }

        public void setBuildFiles(List<String> buildFiles) {
            this.buildFiles = buildFiles;
        }

        public boolean isHasTests() {
            return hasTests;
        }

        public void setHasTests(boolean hasTests) {
            this.hasTests = hasTests;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }
    }
}
