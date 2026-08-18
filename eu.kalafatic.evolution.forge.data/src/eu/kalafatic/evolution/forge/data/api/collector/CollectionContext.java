package eu.kalafatic.evolution.forge.data.api.collector;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Context provided to data collectors during collection.
 */
public class CollectionContext {

    private File projectDirectory;
    private List<File> diskPaths;
    private List<String> searchTopics;
    private List<String> mcpToolsOrResources;
    private List<String> exclusions;
    private long maxFileSize; // in bytes, e.g. 10MB
    private Map<String, Object> properties;

    public CollectionContext() {
        this.diskPaths = new ArrayList<>();
        this.searchTopics = new ArrayList<>();
        this.mcpToolsOrResources = new ArrayList<>();
        this.exclusions = new ArrayList<>();
        this.maxFileSize = 10 * 1024 * 1024L; // Default 10MB
        this.properties = new HashMap<>();
    }

    public File getProjectDirectory() {
        return projectDirectory;
    }

    public void setProjectDirectory(File projectDirectory) {
        this.projectDirectory = projectDirectory;
    }

    public List<File> getDiskPaths() {
        return diskPaths;
    }

    public void setDiskPaths(List<File> diskPaths) {
        this.diskPaths = diskPaths != null ? diskPaths : new ArrayList<>();
    }

    public void addDiskPath(File path) {
        if (this.diskPaths == null) {
            this.diskPaths = new ArrayList<>();
        }
        this.diskPaths.add(path);
    }

    public List<String> getSearchTopics() {
        return searchTopics;
    }

    public void setSearchTopics(List<String> searchTopics) {
        this.searchTopics = searchTopics != null ? searchTopics : new ArrayList<>();
    }

    public void addSearchTopic(String topic) {
        if (this.searchTopics == null) {
            this.searchTopics = new ArrayList<>();
        }
        this.searchTopics.add(topic);
    }

    public List<String> getMcpToolsOrResources() {
        return mcpToolsOrResources;
    }

    public void setMcpToolsOrResources(List<String> mcpToolsOrResources) {
        this.mcpToolsOrResources = mcpToolsOrResources != null ? mcpToolsOrResources : new ArrayList<>();
    }

    public List<String> getExclusions() {
        return exclusions;
    }

    public void setExclusions(List<String> exclusions) {
        this.exclusions = exclusions != null ? exclusions : new ArrayList<>();
    }

    public long getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties != null ? properties : new HashMap<>();
    }

    public Object getProperty(String key) {
        return this.properties.get(key);
    }

    public void setProperty(String key, Object value) {
        this.properties.put(key, value);
    }
}
