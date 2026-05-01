package eu.kalafatic.evolution.supervisor;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Supervisor for autonomous self-development sessions.
 * Acts as the executor: builds, starts, and stops the Worker (RCP).
 * Follows a file-based protocol via command.json and state.json.
 */
public class SelfDevSupervisor {
    private final File baseDir;
    private final ProcessRunner runner = new ProcessRunner();
    private final SelfDevProtocol protocol;
    private final ResultReader reader = new ResultReader();
    private final EvoValidator validator = new EvoValidator();
    private final IterationManager iterationManager;

    public SelfDevSupervisor(File baseDir) {
        this.baseDir = baseDir;
        this.protocol = new SelfDevProtocol(baseDir);
        this.iterationManager = new IterationManager(baseDir);
    }

    public void run() {
        System.out.println("[SUPERVISOR] Starting monitoring loop...");
        try {
            while (true) {
                // 1. Check for legacy/bootstrap triggers (Adaptation)
                checkLegacyTriggers();

                // 2. Check for new Protocol Commands (Primary)
                SelfDevProtocol.Command command = protocol.readCommand();
                if (command != null) {
                    System.out.println("[SUPERVISOR] Command received: " + command.action + " (iter: " + command.iteration + ")");
                    handleCommand(command);
                    protocol.clearCommand();
                }

                // 3. Check for Control overrides
                SelfDevProtocol.Control control = protocol.readControl();
                if (control != null && "STOP".equals(control.forceAction)) {
                    System.out.println("[SUPERVISOR] Stop command received. Exiting.");
                    runner.stopRCP();
                    break;
                }

                Thread.sleep(2000);
            }
        } catch (Exception e) {
            System.err.println("[CRITICAL] Supervisor loop failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void checkLegacyTriggers() {
        File bootstrapFile = new File(baseDir, "self-dev-run/bootstrap.json");
        if (bootstrapFile.exists()) {
            try {
                Bootstrap bootstrap = reader.readBootstrap(bootstrapFile);
                if ("BUILD_AND_START".equals(bootstrap.getAction())) {
                    System.out.println("[SUPERVISOR] Legacy bootstrap detected. Converting to protocol command.");
                    handleCommand(newProtocolCommand("BUILD_AND_RUN", 0));
                    bootstrapFile.delete();
                }
            } catch (IOException e) {
                System.err.println("[SUPERVISOR] Failed to read bootstrap: " + e.getMessage());
            }
        }
    }

    private SelfDevProtocol.Command newProtocolCommand(String action, int iteration) {
        SelfDevProtocol.Command cmd = new SelfDevProtocol.Command();
        cmd.action = action;
        cmd.iteration = iteration;
        return cmd;
    }

    private void handleCommand(SelfDevProtocol.Command command) {
        if ("BUILD_AND_RUN".equals(command.action)) {
            buildAndRun(command.iteration);
        } else if ("RESTART".equals(command.action)) {
            restart(command.iteration);
        } else if ("NONE".equals(command.action)) {
            System.out.println("[SUPERVISOR] NONE action received. Doing nothing.");
        }
    }

    private void buildAndRun(int iteration) {
        protocol.updateState(iteration, "BUILDING", "RUNNING", "Building project", 0.1);
        if (runner.runBuild(baseDir)) {
            protocol.updateState(iteration, "STARTING", "RUNNING", "Starting RCP", 0.5);
            String jarName = findJar(baseDir);
            if (jarName != null) {
                File stateFile = new File(baseDir, "self-dev-run/state.json");
                runner.runRCP(baseDir, jarName, stateFile.getAbsolutePath());
            } else {
                protocol.updateState(iteration, "ERROR", "FAILED", "No JAR found after build", 0.0);
            }
        } else {
            protocol.updateState(iteration, "ERROR", "FAILED", "Build failed", 0.0);
        }
    }

    private void restart(int iteration) {
        System.out.println("[SUPERVISOR] Restarting RCP for iteration " + iteration);

        // 0. Stop current RCP
        runner.stopRCP();

        // 1. Apply patch
        SelfDevProtocol.Patch patch = protocol.readPatch();
        if (patch != null && patch.diff != null && !patch.diff.isEmpty()) {
            System.out.println("[SUPERVISOR] Applying patch with " + patch.files.size() + " files");
            protocol.updateState(iteration, "APPLYING_PATCH", "RUNNING", "Applying patch", 0.05);
            if (!runner.applyPatch(baseDir, patch.diff)) {
                protocol.updateState(iteration, "ERROR", "FAILED", "Failed to apply patch", 0.0);
                return;
            }
        }

        // 2. Build and Run
        buildAndRun(iteration);
    }

    private String findJar(File variantDir) {
        File targetDir = new File(variantDir, "target");
        if (targetDir.exists()) {
            File[] files = targetDir.listFiles((dir, name) -> name.endsWith(".jar") && !name.contains("sources"));
            if (files != null && files.length > 0) {
                return files[0].getAbsolutePath();
            }
        }
        // Fallback for current project structure if not in target
        return null;
    }

    // Keep legacy evaluation logic if needed by other components, though Worker now owns this.
    public String evaluate(Map<String, Result> results) {
        String bestVariant = null;
        double maxScore = 0.7; // Threshold
        for (Map.Entry<String, Result> entry : results.entrySet()) {
            Result r = entry.getValue();
            if ("OK".equalsIgnoreCase(r.getStatus()) && r.getScore() > maxScore) {
                maxScore = r.getScore();
                bestVariant = entry.getKey();
            }
        }
        return bestVariant;
    }
}
