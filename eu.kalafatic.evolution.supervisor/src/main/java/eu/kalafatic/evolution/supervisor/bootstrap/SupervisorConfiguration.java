package eu.kalafatic.evolution.supervisor.bootstrap;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class SupervisorConfiguration {
    private final File supervisorJar;
    private final File projectRoot;
    private final Map<String, String> jvmSystemProperties = new HashMap<>();
    private final Map<String, String> environmentVariables = new HashMap<>();

    public SupervisorConfiguration(File supervisorJar, File projectRoot) {
        this.supervisorJar = supervisorJar;
        this.projectRoot = projectRoot;
    }

    public File getSupervisorJar() {
        return supervisorJar;
    }

    public File getProjectRoot() {
        return projectRoot;
    }

    public Map<String, String> getJvmSystemProperties() {
        return jvmSystemProperties;
    }

    public void addSystemProperty(String key, String value) {
        this.jvmSystemProperties.put(key, value);
    }

    public Map<String, String> getEnvironmentVariables() {
        return environmentVariables;
    }

    public void addEnvironmentVariable(String key, String value) {
        this.environmentVariables.put(key, value);
    }
}
