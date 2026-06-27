package eu.kalafatic.evolution.controller.tools;

import java.io.File;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.Eclipse;

/**
 * Tool for handling Eclipse-specific tasks like building plugins, features, products, and update sites.
 * It primarily delegates to Maven/Tycho but provides specialized commands for Eclipse development.
 */
public class EclipseTool implements ITool {
    @Override
    public String getName() {
        return "EclipseTool";
    }

    @Override
    public String execute(String command, File workingDir, TaskContext context) throws Exception {
        context.log("EclipseTool: Executing " + command);

        ShellTool shell = new ShellTool();
        String os = System.getProperty("os.name").toLowerCase();
        // Default to 'mvn' (Linux/Ubuntu) unless Windows is explicitly detected
        String mvnCmd = os.contains("win") ? "mvn.cmd" : "mvn";

        switch (command.toUpperCase()) {
            case "BUILD_PLUGINS":
                // Standard Tycho build for plugins
                return shell.execute(mvnCmd + " clean install -DskipTests", workingDir, context);
            case "BUILD_FEATURES":
                // Standard Tycho build for features
                return shell.execute(mvnCmd + " clean install -DskipTests", workingDir, context);
            case "BUILD_PRODUCT":
                // Standard Tycho build for products
                return shell.execute(mvnCmd + " clean install -DskipTests", workingDir, context);
            case "BUILD_UPDATE_SITE":
                // Standard Tycho build for update sites (repository projects)
                return shell.execute(mvnCmd + " clean install -DskipTests", workingDir, context);
            case "TEST_CONNECTION":
                return testEnvironment(workingDir, context);
            default:
                if (command.toUpperCase().startsWith("MVN ")) {
                     return shell.execute(command, workingDir, context);
                }
                return "Unknown EclipseTool command: " + command + ". Use BUILD_PLUGINS, BUILD_FEATURES, BUILD_PRODUCT, BUILD_UPDATE_SITE or a direct mvn command.";
        }
    }

    private String testEnvironment(File workingDir, TaskContext context) {
        StringBuilder sb = new StringBuilder();
        Eclipse eclipse = context.getOrchestrator().getEclipse();
        if (eclipse != null) {
            sb.append("Eclipse Workspace: ").append(eclipse.getWorkspace() != null ? eclipse.getWorkspace() : "Not set").append("\n");
            sb.append("Eclipse Installation: ").append(eclipse.getInstallation() != null ? eclipse.getInstallation() : "Not set").append("\n");
            sb.append("Target Platform: ").append(eclipse.getTargetPlatform() != null ? eclipse.getTargetPlatform() : "Not set").append("\n");
        } else {
            sb.append("No Eclipse settings configured in the orchestrator model.\n");
        }

        try {
            ShellTool shell = new ShellTool();
            String mvnVer = shell.execute(System.getProperty("os.name").toLowerCase().contains("win") ? "mvn.cmd -version" : "mvn -version", workingDir, context);
            sb.append("Environment check: SUCCESS\n").append(mvnVer);
        } catch (Exception e) {
            sb.append("Environment check: FAILED\n").append(e.getMessage());
        }

        return sb.toString();
    }
}
