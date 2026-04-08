package eu.kalafatic.evolution.controller.orchestration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import eu.kalafatic.evolution.model.orchestration.Maven;

/**
 * Tool for executing Maven commands.
 */
public class MavenTool implements ITool {
    @Override
    public String getName() {
        return "MavenTool";
    }

    @Override
    public String execute(String command, File workingDir, TaskContext context) throws Exception {
        context.log("Tool [MavenTool]: Executing " + command);
        List<String> mavenArgs = new ArrayList<>();

        String os = System.getProperty("os.name").toLowerCase();
        // Default to 'mvn' (Linux/Ubuntu) unless Windows is explicitly detected
        String mavenCmd = os.contains("win") ? "mvn.cmd" : "mvn";
        mavenArgs.add(mavenCmd);

        Maven mavenSettings = context.getOrchestrator().getMaven();
        if (mavenSettings != null) {
            if (!mavenSettings.getGoals().isEmpty()) {
                mavenArgs.addAll(mavenSettings.getGoals());
            } else {
                mavenArgs.add("clean");
                mavenArgs.add("install");
            }
            if (!mavenSettings.getProfiles().isEmpty()) {
                mavenArgs.add("-P" + String.join(",", mavenSettings.getProfiles()));
            }
        } else {
            mavenArgs.add("clean");
            mavenArgs.add("install");
        }

        ShellTool shell = new ShellTool();
        String cmdStr = String.join(" ", mavenArgs);
        context.log("Tool [MavenTool]: Launching " + cmdStr);
        String result = shell.execute(cmdStr, workingDir, context);
        context.log("Tool [MavenTool]: Execution finished. Output length: " + result.length());
        return result;
    }
}
