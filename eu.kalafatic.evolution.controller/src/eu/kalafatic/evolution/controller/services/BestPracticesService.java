package eu.kalafatic.evolution.controller.services;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.utils.log.Log;

public class BestPracticesService {

    private final Orchestrator orchestrator;
    private final File projectRoot;

    public BestPracticesService(Orchestrator orchestrator, File projectRoot) {
        this.orchestrator = orchestrator;
        this.projectRoot = projectRoot;
        initializeDefaults();
    }

    public String getInstructionsPath() {
        return new File(projectRoot, "orchestrator/best_practices").getAbsolutePath();
    }

    private void initializeDefaults() {
        File baseDir = new File(getInstructionsPath());
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }

        Map<String, String> roleDefaults = new HashMap<>();
        roleDefaults.put("architect", "# ARCHITECT Best Practices\n\n" +
                "- Design for modularity and high cohesion.\n" +
                "- Use EMF-based modeling for core domain entities.\n" +
                "- Follow the 'Separation of Concerns' principle between UI and logic.\n" +
                "- Prioritize maintainability and extensibility in all design decisions.");

        roleDefaults.put("planner", "# PLANNER Best Practices\n\n" +
                "- Decompose user requests into atomic, actionable tasks.\n" +
                "- Handle ambiguity by generating a clarification task (the 'Jules' way).\n" +
                "- Assign specific agent roles (JavaDev, Tester, etc.) to tasks for better accuracy.\n" +
                "- Use 'loopToTaskId' for iterative fix-test-improve cycles.");

        roleDefaults.put("agent", "# AGENT Best Practices\n\n" +
                "- Be concise and professional in all communications.\n" +
                "- Prioritize using available tools over general reasoning for technical tasks.\n" +
                "- Report errors clearly and suggest potential fixes or workarounds.\n" +
                "- Always verify the outcome of tool execution.");

        roleDefaults.put("tools", "# TOOLS Best Practices\n\n" +
                "- Ensure all file paths are normalized and relative to the project root.\n" +
                "- Request explicit user approval for high-risk actions (e.g., DELETE, SHELL).\n" +
                "- Log tool execution results, including partial successes or informative failures.\n" +
                "- Clean up temporary resources or side effects after execution.");

        for (Map.Entry<String, String> entry : roleDefaults.entrySet()) {
            String role = entry.getKey();
            File roleDir = new File(baseDir, role);
            if (!roleDir.exists()) {
                roleDir.mkdirs();
            }
            File guidelines = new File(roleDir, "guidelines.md");
            if (!guidelines.exists()) {
                try {
                    Files.write(guidelines.toPath(), entry.getValue().getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    Log.log("BestPractices: Could not initialize default guidelines for " + role + ": " + e.getMessage());
                }
            }
        }
    }

    public String getPracticesForRole(String role) {
        if (role == null) return "";
        role = role.toLowerCase();

        // Map common agent types to best practice roles
        if (role.contains("planner")) role = "planner";
        else if (role.contains("architect")) role = "architect";
        else if (role.contains("tool")) role = "tools";
        else role = "agent";

        File roleDir = new File(getInstructionsPath(), role);
        if (roleDir.exists() && roleDir.isDirectory()) {
            StringBuilder content = new StringBuilder();
            File[] mdFiles = roleDir.listFiles((dir, name) -> name.endsWith(".md"));
            if (mdFiles != null) {
                for (File mdFile : mdFiles) {
                    try {
                        content.append(new String(Files.readAllBytes(mdFile.toPath()), StandardCharsets.UTF_8)).append("\n\n");
                    } catch (IOException e) {
                        Log.log("BestPractices: Error reading " + mdFile.getAbsolutePath() + ": " + e.getMessage());
                    }
                }
            }
            return content.toString();
        }
        return "";
    }

    public String getCombinedPractices() {
        StringBuilder sb = new StringBuilder();
        sb.append("### BEST PRACTICES & GUIDELINES\n");
        String[] roles = {"architect", "planner", "agent", "tools"};
        boolean found = false;
        for (String role : roles) {
            String practices = getPracticesForRole(role);
            if (!practices.isEmpty()) {
                sb.append("#### ROLE: ").append(role.toUpperCase()).append("\n");
                sb.append(practices).append("\n");
                found = true;
            }
        }
        return found ? sb.toString() : "";
    }
}
