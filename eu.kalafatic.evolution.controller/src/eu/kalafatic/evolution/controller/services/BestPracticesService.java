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

        String[] roles = {"architect", "planner", "agent", "tools"};
        for (String role : roles) {
            File roleDir = new File(baseDir, role);
            if (!roleDir.exists()) {
                roleDir.mkdirs();
            }
            File guidelines = new File(roleDir, "guidelines.md");
            if (!guidelines.exists()) {
                try {
                    Files.write(guidelines.toPath(), ("# " + role.toUpperCase() + " Best Practices\n\n- Add your specific instructions here.").getBytes(StandardCharsets.UTF_8));
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
