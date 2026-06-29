package eu.kalafatic.evolution.supervisor.bootstrap;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BuildConfiguration {
    private final File workspacePath;
    private final List<String> goals = new ArrayList<>();
    private final List<String> profiles = new ArrayList<>();
    private boolean skipTests = true;

    public BuildConfiguration(File workspacePath) {
        this.workspacePath = workspacePath;
    }

    public File getWorkspacePath() {
        return workspacePath;
    }

    public List<String> getGoals() {
        return goals;
    }

    public void addGoal(String goal) {
        this.goals.add(goal);
    }

    public List<String> getProfiles() {
        return profiles;
    }

    public void addProfile(String profile) {
        this.profiles.add(profile);
    }

    public boolean isSkipTests() {
        return skipTests;
    }

    public void setSkipTests(boolean skipTests) {
        this.skipTests = skipTests;
    }
}
