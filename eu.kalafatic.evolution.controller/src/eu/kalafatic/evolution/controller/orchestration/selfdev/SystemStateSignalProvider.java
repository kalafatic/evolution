package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;

/**
 * Dynamically discovers the system state and provides it as a signal for the DarwinEngine.
 * This ensures the platform remains general-purpose and can target any codebase.
 */
public class SystemStateSignalProvider {
    private final File projectRoot;
    private final TaskContext context;

    public SystemStateSignalProvider(File projectRoot, TaskContext context) {
        this.projectRoot = projectRoot;
        this.context = context;
    }

    /**
     * @return A string representation of the discovered system state.
     */
    public String getSystemStateSignal() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n--- SYSTEM STATE (DYNAMIC DISCOVERY) ---\n");

        // 1. Detect Build System
        String buildSystem = detectBuildSystem();
        sb.append("Build System: ").append(buildSystem).append("\n");

        // 2. Discover Modules
        List<String> modules = discoverModules();
        if (!modules.isEmpty()) {
            sb.append("Modules/Components:\n");
            for (String module : modules) {
                sb.append("- ").append(module).append("\n");
            }
        } else {
            sb.append("Project Structure: Flat/Single-module\n");
        }

        // 3. Environment Info
        sb.append("Target Environment: Java ").append(System.getProperty("java.version"));
        String os = System.getProperty("os.name");
        sb.append(" on ").append(os).append("\n");

        return sb.toString();
    }

    private String detectBuildSystem() {
        if (new File(projectRoot, "pom.xml").exists()) return "Maven (Tycho/Standard)";
        if (new File(projectRoot, "build.gradle").exists() || new File(projectRoot, "build.gradle.kts").exists()) return "Gradle";
        if (new File(projectRoot, "Makefile").exists()) return "Make";
        if (new File(projectRoot, "CMakeLists.txt").exists()) return "CMake";
        if (new File(projectRoot, "package.json").exists()) return "NPM/Node.js";
        return "Unknown/Generic";
    }

    private List<String> discoverModules() {
        List<String> modules = new ArrayList<>();
        File[] files = projectRoot.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory() && !f.getName().startsWith(".") && !f.getName().equals("target") && !f.getName().equals("bin")) {
                    // Check if it looks like a module (has a build file or src directory)
                    if (isModule(f)) {
                        modules.add(f.getName());
                    }
                }
            }
        }
        return modules;
    }

    private boolean isModule(File dir) {
        return new File(dir, "pom.xml").exists() ||
               new File(dir, "build.gradle").exists() ||
               new File(dir, "src").exists() ||
               new File(dir, "package.json").exists();
    }
}
