package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.json.JSONObject;

/**
 * Controller for bootstrapping the self-development flow.
 * Triggered by the RCP application to start an external Supervisor.
 */
public class SelfDevBootstrapController {

    private final File projectRoot;
    private final File runDir;

    public SelfDevBootstrapController(File projectRoot) {
        this.projectRoot = projectRoot;
        this.runDir = new File(projectRoot, "self-dev-run");
        if (!runDir.exists()) {
            runDir.mkdirs();
        }
    }

    /**
     * Writes bootstrap.json and triggers the external Supervisor.
     */
    public void startBootstrap() throws IOException {
        JSONObject bootstrap = new JSONObject();
        bootstrap.put("sourcePath", projectRoot.getAbsolutePath());
        bootstrap.put("targetPath", new File(runDir, "workspace").getAbsolutePath());
        bootstrap.put("action", "BUILD_AND_START");

        File bootstrapFile = new File(runDir, "bootstrap.json");
        Files.write(bootstrapFile.toPath(), bootstrap.toString(4).getBytes());

        // Trigger Supervisor
        triggerSupervisor();
    }

    private void triggerSupervisor() {
        new Thread(() -> {
            try {
                // Find supervisor jar
                File supervisorDir = new File(projectRoot, "eu.kalafatic.evolution.supervisor/target");
                File[] jars = supervisorDir.listFiles((dir, name) -> name.endsWith("-shaded.jar") || (name.endsWith(".jar") && !name.contains("sources")));

                String jarPath = null;
                if (jars != null && jars.length > 0) {
                    jarPath = jars[0].getAbsolutePath();
                } else {
                    jarPath = new File(projectRoot, "eu.kalafatic.evolution.supervisor/target/eu.kalafatic.evolution.supervisor-1.0.0-SNAPSHOT.jar").getAbsolutePath();
                }

                ProcessBuilder pb = new ProcessBuilder("java", "-jar", jarPath, projectRoot.getAbsolutePath());
                pb.directory(projectRoot);
                pb.inheritIO();
                pb.start();
            } catch (IOException e) {
                System.err.println("Failed to trigger Supervisor: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Reads the current status from status.json.
     */
    public JSONObject getStatus() {
        File statusFile = new File(runDir, "status.json");
        if (statusFile.exists()) {
            try {
                String content = new String(Files.readAllBytes(statusFile.toPath()));
                return new JSONObject(content);
            } catch (Exception e) {
                // Return null if file is being written or invalid
            }
        }
        return null;
    }
}
