package eu.kalafatic.evolution.supervisor;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Supervisor for autonomous self-development sessions.
 * Acts as the top-level orchestrator of atomic development tasks and validates results externally.
 */
public class SelfDevSupervisor {
    private final File baseDir;
    private final ProcessRunner runner = new ProcessRunner();
    private final SelfDevProtocol protocol;
    private final ResultReader reader = new ResultReader();
    private final EvoValidator validator = new EvoValidator();
    private final IterationManager iterationManager;

    private SupervisorState state;
    private final File supervisorStateFile;
    private final File activeTaskFile;

    public SelfDevSupervisor(File baseDir) {
        this.baseDir = baseDir;
        this.protocol = new SelfDevProtocol(baseDir);
        this.iterationManager = new IterationManager(baseDir);
        this.supervisorStateFile = new File(baseDir, "self-dev-run/supervisor_state.json");
        this.activeTaskFile = new File(baseDir, "self-dev-run/active_task.json");

        try {
            this.state = SupervisorState.load(supervisorStateFile);
        } catch (IOException e) {
            System.err.println("[SUPERVISOR] Failed to load supervisor_state.json: " + e.getMessage());
            this.state = new SupervisorState();
        }
    }

    public SupervisorState getState() {
        return state;
    }

    public ProcessRunner getRunner() {
        return runner;
    }

    public void run() {
        System.out.println("[SUPERVISOR] Starting monitoring loop...");

        // Perform interrupted task recovery
        recoverInterruptedTask();

        try {
            while (true) {
                // Check for Control overrides
                SelfDevProtocol.Control control = protocol.readControl();
                if (control != null && "STOP".equals(control.forceAction)) {
                    System.out.println("[SUPERVISOR] Stop command received. Exiting.");
                    runner.stopRCP();
                    break;
                }

                // If we have an active task or tasks in queue, execute atomic task lifecycle
                if (state.getCurrentTask() != null || !state.getTaskQueue().isEmpty()) {
                    if (state.getCurrentTask() == null) {
                        DevelopmentTask next = state.getTaskQueue().remove(0);
                        next.setStatus(TaskStatus.READY);
                        state.setCurrentTask(next);
                        saveState();
                    }

                    DevelopmentTask task = state.getCurrentTask();
                    executeTaskLifecycle(task);
                    continue; // Skip legacy triggers when executing tasks
                }

                // Check for legacy/bootstrap triggers (Adaptation)
                checkLegacyTriggers();

                // Check for new Protocol Commands (Primary)
                SelfDevProtocol.Command command = protocol.readCommand();
                if (command != null) {
                    System.out.println("[SUPERVISOR] Command received: " + command.action + " (iter: " + command.iteration + ")");
                    handleCommand(command);
                    protocol.clearCommand();
                }

                Thread.sleep(2000);
            }
        } catch (Exception e) {
            System.err.println("[CRITICAL] Supervisor loop failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void recoverInterruptedTask() {
        DevelopmentTask current = state.getCurrentTask();
        if (current != null) {
            System.out.println("[SUPERVISOR] Interrupted task found during startup: " + current.getId() + " (status: " + current.getStatus() + ")");
            if (current.getStatus() == TaskStatus.EVO_RUNNING) {
                System.out.println("[SUPERVISOR] Reconnecting/terminating safely: Task was running EVO when interrupted. Setting status to READY to retry.");
                current.setStatus(TaskStatus.READY);
                saveState();
            } else if (current.getStatus() == TaskStatus.BUILDING ||
                       current.getStatus() == TaskStatus.TESTING ||
                       current.getStatus() == TaskStatus.RUNNING ||
                       current.getStatus() == TaskStatus.VERIFYING) {
                System.out.println("[SUPERVISOR] EVO completed but verification did not. Resuming verification from: " + current.getStatus());
                // Will continue verification in the main loop
            } else if (current.getStatus() == TaskStatus.READY || current.getStatus() == TaskStatus.CREATED) {
                // Safe, will start normally
            } else {
                System.out.println("[SUPERVISOR] Ambiguous state. Marking task as requiring recovery.");
                current.setStatus(TaskStatus.EVO_FAILED);
                current.setFailureReason("Interrupted in ambiguous status: " + current.getStatus());
                state.getTaskHistory().add(current);
                state.setCurrentTask(null);
                saveState();
            }
        }
    }

    protected void executeTaskLifecycle(DevelopmentTask task) {
        System.out.println("[SUPERVISOR] Processing task: " + task.getId() + " - " + task.getObjective() + " (status: " + task.getStatus() + ")");

        try {
            // Write task context
            ObjectMapper mapper = new ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(activeTaskFile, task);

            // 1. Prepare and Run EVO
            if (task.getStatus() == TaskStatus.READY || task.getStatus() == TaskStatus.EVO_RUNNING) {
                task.setAttempts(task.getAttempts() + 1);
                task.setStatus(TaskStatus.EVO_RUNNING);
                saveState();

                publishEvent("TASK_STARTED", "Starting task: " + task.getId() + " (attempt " + task.getAttempts() + ")");
                publishEvent("EVO_STARTED", "Starting EVO for task: " + task.getId());

                String evoTarget = findEvoTarget(baseDir);
                if (evoTarget == null) {
                    evoTarget = "mock-rcp.jar";
                }
                File rcpStateFile = new File(baseDir, "self-dev-run/state.json");
                boolean evoSuccess = runner.runRCP(baseDir, evoTarget, rcpStateFile.getAbsolutePath());

                if (evoSuccess) {
                    task.setStatus(TaskStatus.EVO_COMPLETED);
                    publishEvent("EVO_COMPLETED", "EVO successfully completed task: " + task.getId());
                } else {
                    task.setStatus(TaskStatus.EVO_FAILED);
                    publishEvent("EVO_FAILED", "EVO failed to complete task: " + task.getId());
                }
                saveState();
            }

            // 2. Shut down EVO
            if (task.getStatus() == TaskStatus.EVO_COMPLETED || task.getStatus() == TaskStatus.EVO_FAILED) {
                runner.stopRCP();
                publishEvent("EVO_SHUTDOWN", "EVO process cleanly shut down for task: " + task.getId());
            }

            // Handle EVO failure
            if (task.getStatus() == TaskStatus.EVO_FAILED) {
                handleTaskFailure(task, "EVO_FAILED", "EVO process returned non-zero or failed to complete");
                return;
            }

            // 3. Independent External Verification Cycle
            // A. BUILDING
            if (task.getStatus() == TaskStatus.EVO_COMPLETED || task.getStatus() == TaskStatus.BUILDING) {
                task.setStatus(TaskStatus.BUILDING);
                saveState();
                publishEvent("BUILD_STARTED", "Starting external build verification for: " + task.getId());

                EvoProductBuildService buildService = new EvoProductBuildService(baseDir, runner);
                ProductBuildResult buildResult = buildService.buildProduct();
                if (buildResult.isSuccessful()) {
                    publishEvent("BUILD_COMPLETED", "Build successful for: " + task.getId());
                    if (buildResult.getArtifactPath() != null) {
                        task.getMetadata().put("artifactPath", buildResult.getArtifactPath().toString());
                    }
                    saveState();
                } else {
                    publishEvent("BUILD_COMPLETED", "Build failed for: " + task.getId() + " - " + buildResult.getFailureSummary());
                    handleTaskFailure(task, "BUILD_FAILED", "Project or product build failed: " + buildResult.getFailureSummary());
                    return;
                }
            }

            // B. TESTING
            if (task.getStatus() == TaskStatus.BUILDING || task.getStatus() == TaskStatus.TESTING) {
                task.setStatus(TaskStatus.TESTING);
                saveState();
                publishEvent("TEST_STARTED", "Starting external test verification for: " + task.getId());

                boolean testsOk = runner.runTests(baseDir);
                if (testsOk) {
                    publishEvent("TEST_COMPLETED", "Tests completed successfully for: " + task.getId());
                } else {
                    publishEvent("TEST_COMPLETED", "Tests failed for: " + task.getId());
                    handleTaskFailure(task, "TEST_FAILED", "One or more tests failed post-compilation.");
                    return;
                }
            }

            // C. RUNNING
            if (task.getStatus() == TaskStatus.TESTING || task.getStatus() == TaskStatus.RUNNING) {
                task.setStatus(TaskStatus.RUNNING);
                saveState();
                publishEvent("RUN_STARTED", "Starting runtime execution check for: " + task.getId());

                boolean runOk = runner.runApplication(baseDir);
                if (runOk) {
                    publishEvent("RUN_COMPLETED", "Application launched successfully for: " + task.getId());
                } else {
                    publishEvent("RUN_COMPLETED", "Application runtime launch failed for: " + task.getId());
                    handleTaskFailure(task, "RUN_FAILED", "Application failed to launch or crashed at startup.");
                    return;
                }
            }

            // D. VERIFYING
            if (task.getStatus() == TaskStatus.RUNNING || task.getStatus() == TaskStatus.VERIFYING) {
                task.setStatus(TaskStatus.VERIFYING);
                saveState();
                publishEvent("VERIFICATION_COMPLETED", "Starting final health/acceptance checks for: " + task.getId());

                String pathStr = (String) task.getMetadata().get("artifactPath");
                java.nio.file.Path artifactPath = null;
                if (pathStr != null) {
                    artifactPath = java.nio.file.Paths.get(pathStr);
                } else {
                    // Fallback discovery
                    String expectedName = PlatformInfo.isWindows() ? "EVO-win-x64.zip" : "EVO-linux-x64.tar.gz";
                    artifactPath = baseDir.toPath().resolve("release").resolve(expectedName);
                }

                EvoProductValidator productValidator = new EvoProductValidator(runner);
                boolean verifyOk = productValidator.validateProduct(artifactPath);
                if (verifyOk) {
                    publishEvent("VERIFICATION_COMPLETED", "Final verification passed for: " + task.getId());
                } else {
                    publishEvent("VERIFICATION_COMPLETED", "Final verification failed for: " + task.getId());
                    handleTaskFailure(task, "VERIFICATION_FAILED", "Product file structure, launch, or readiness checks failed.");
                    return;
                }
            }

            // Success & Completion
            task.setStatus(TaskStatus.COMPLETED);
            publishEvent("TASK_COMPLETED", "Task successfully completed and verified: " + task.getId());
            state.setConsecutiveFailures(0);
            state.getTaskHistory().add(task);

            // Resolve recursive lineage completion
            resolveLineageCompletion(task);

            state.setCurrentTask(null);
            saveState();

        } catch (Exception e) {
            System.err.println("[SUPERVISOR] Exception during task lifecycle: " + e.getMessage());
            e.printStackTrace();
            handleTaskFailure(task, "VERIFICATION_FAILED", "Exception occurred: " + e.getMessage());
        }
    }

    private void handleTaskFailure(DevelopmentTask task, String failureType, String errorDetails) {
        task.setStatus(TaskStatus.valueOf(failureType));
        task.setFailureReason(errorDetails);
        saveState();

        System.out.println("[SUPERVISOR] Handling failure: " + failureType + " for task: " + task.getId());

        // Decision loop: retry or repair
        if (task.getAttempts() < state.getMaxTaskAttempts() && !"BUILD_FAILED".equals(failureType) && !"TEST_FAILED".equals(failureType) && !"RUN_FAILED".equals(failureType) && !"VERIFICATION_FAILED".equals(failureType)) {
            // Retry the same task
            System.out.println("[SUPERVISOR] Retrying task: " + task.getId() + " (attempt " + (task.getAttempts() + 1) + ")");
            task.setStatus(TaskStatus.READY);
            saveState();
        } else {
            // Create repair task
            if ("BUILD_FAILED".equals(failureType) || "TEST_FAILED".equals(failureType) || "RUN_FAILED".equals(failureType) || "VERIFICATION_FAILED".equals(failureType)) {
                createRepairTask(task, failureType, errorDetails);
            } else {
                // EVO Failed completely and exceeded maxTaskAttempts
                publishEvent("TASK_FAILED", "Task failed permanently after exceeding max attempts: " + task.getId());
                state.getTaskHistory().add(task);
                state.setCurrentTask(null);
                state.setConsecutiveFailures(state.getConsecutiveFailures() + 1);

                if (state.getConsecutiveFailures() >= state.getMaxConsecutiveFailures()) {
                    System.err.println("[CRITICAL] Max consecutive failures reached (" + state.getMaxConsecutiveFailures() + "). Stopping self-development process.");
                    state.getTaskQueue().clear(); // Stop everything
                }
                saveState();
            }
        }
    }

    private void createRepairTask(DevelopmentTask failedTask, String failureType, String errorDetails) {
        int repairDepth = getRepairChainDepth(failedTask);
        if (repairDepth >= state.getMaxRepairAttempts()) {
            System.out.println("[SUPERVISOR] Max repair attempts reached (" + state.getMaxRepairAttempts() + ") for root task. Permanently failing task chain.");
            failedTask.setFailureReason("Max repair attempts reached: " + repairDepth);
            state.getTaskHistory().add(failedTask);
            state.setCurrentTask(null);
            state.setConsecutiveFailures(state.getConsecutiveFailures() + 1);

            if (state.getConsecutiveFailures() >= state.getMaxConsecutiveFailures()) {
                System.err.println("[CRITICAL] Max consecutive failures reached after repair limit. Stopping process.");
                state.getTaskQueue().clear();
            }
            saveState();
            return;
        }

        String repairId = failedTask.getId() + "-repair-" + (repairDepth + 1);
        String objective = "Fix " + failureType.toLowerCase().replace("_", " ") + " introduced while implementing \"" + failedTask.getObjective() + "\".";
        DevelopmentTask repairTask = new DevelopmentTask(repairId, objective);
        repairTask.setParentTaskId(failedTask.getId());
        repairTask.setStatus(TaskStatus.READY);

        repairTask.getMetadata().put("originalTaskObjective", failedTask.getObjective());
        repairTask.getMetadata().put("failureType", failureType);
        repairTask.getMetadata().put("errorDetails", errorDetails);
        repairTask.getMetadata().put("failedCommand", getFailedCommandForType(failureType));

        // Insert repair task at the FRONT of the queue so it runs next
        state.getTaskQueue().add(0, repairTask);

        // Move parent to history/tracking
        state.getTaskHistory().add(failedTask);
        state.setCurrentTask(null);
        saveState();

        System.out.println("[SUPERVISOR] Created repair task: " + repairId + " targeting parent: " + failedTask.getId());
    }

    private String getFailedCommandForType(String failureType) {
        if ("BUILD_FAILED".equals(failureType)) return "mvn clean package -DskipTests";
        if ("TEST_FAILED".equals(failureType)) return "mvn test";
        if ("RUN_FAILED".equals(failureType)) return "java -jar rcp.jar";
        return "Verification check";
    }

    private int getRepairChainDepth(DevelopmentTask task) {
        int depth = 0;
        String currentParentId = task.getParentTaskId();
        while (currentParentId != null) {
            depth++;
            currentParentId = getParentTaskId(currentParentId);
        }
        return depth;
    }

    private String getParentTaskId(String taskId) {
        if (state.getCurrentTask() != null && taskId.equals(state.getCurrentTask().getId())) {
            return state.getCurrentTask().getParentTaskId();
        }
        for (DevelopmentTask t : state.getTaskQueue()) {
            if (taskId.equals(t.getId())) return t.getParentTaskId();
        }
        for (DevelopmentTask t : state.getTaskHistory()) {
            if (taskId.equals(t.getId())) return t.getParentTaskId();
        }
        return null;
    }

    private void resolveLineageCompletion(DevelopmentTask completedTask) {
        String parentId = completedTask.getParentTaskId();
        if (parentId == null) return;

        // Find parent in history and recursively mark as completed
        for (DevelopmentTask t : state.getTaskHistory()) {
            if (parentId.equals(t.getId())) {
                if (t.getStatus() != TaskStatus.COMPLETED) {
                    t.setStatus(TaskStatus.COMPLETED);
                    System.out.println("[SUPERVISOR] Lineage Resolution: Recursively completed parent task: " + t.getId());
                    resolveLineageCompletion(t);
                }
            }
        }
        for (DevelopmentTask t : state.getTaskQueue()) {
            if (parentId.equals(t.getId())) {
                t.setStatus(TaskStatus.COMPLETED);
                System.out.println("[SUPERVISOR] Lineage Resolution: Recursively completed parent task in queue: " + t.getId());
                resolveLineageCompletion(t);
            }
        }
    }

    private void publishEvent(String event, String message) {
        System.out.println("[EVENT] " + event + ": " + message);
        try {
            File eventLog = new File(baseDir, "self-dev-run/events.log");
            if (eventLog.getParentFile() != null && !eventLog.getParentFile().exists()) {
                eventLog.getParentFile().mkdirs();
            }
            java.nio.file.Files.write(eventLog.toPath(),
                (System.currentTimeMillis() + " [" + event + "] " + message + "\n").getBytes(),
                java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException ignored) {}

        protocol.updateState(0, event, "RUNNING", message, 0.5);
    }

    private void saveState() {
        try {
            state.save(supervisorStateFile);
        } catch (IOException e) {
            System.err.println("[SUPERVISOR] Failed to save supervisor state: " + e.getMessage());
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

    public String findEvoTarget(File baseDir) {
        if (baseDir == null || !baseDir.exists()) return null;

        // 1. Check exportDir and releaseDir for native product executable
        File exportDir = new File(baseDir, "export");
        if (!exportDir.exists() && baseDir.getName().equalsIgnoreCase("export")) {
            exportDir = baseDir;
        }

        File executable = findExecutableInDir(exportDir);
        if (executable != null) {
            System.out.println("[SUPERVISOR] Found native EVO product executable in export: " + executable.getAbsolutePath());
            return executable.getAbsolutePath();
        }

        File releaseDir = new File(baseDir, "release");
        executable = findExecutableInDir(releaseDir);
        if (executable != null) {
            System.out.println("[SUPERVISOR] Found native EVO product executable in release: " + executable.getAbsolutePath());
            return executable.getAbsolutePath();
        }

        // 2. Check for archives in export/release to auto-extract
        if (exportDir.exists()) {
            File[] archives = exportDir.listFiles((dir, name) -> name.endsWith(".zip") || name.endsWith(".tar.gz"));
            if (archives != null && archives.length > 0) {
                for (File archive : archives) {
                    try {
                        System.out.println("[SUPERVISOR] Auto-extracting product archive for task execution: " + archive.getName());
                        if (archive.getName().endsWith(".zip")) {
                            unzip(archive, exportDir);
                        } else if (archive.getName().endsWith(".tar.gz")) {
                            untar(archive, exportDir);
                        }
                    } catch (Exception ex) {
                        System.err.println("[SUPERVISOR] Error auto-extracting archive: " + ex.getMessage());
                    }
                }
                executable = findExecutableInDir(exportDir);
                if (executable != null) {
                    return executable.getAbsolutePath();
                }
            }
        }

        // 3. Fallback to runnable JAR search in export, builds, or target
        if (exportDir.exists()) {
            File[] exportJars = exportDir.listFiles((dir, name) -> name.endsWith(".jar") && !name.startsWith("original-") && !name.contains("supervisor"));
            if (exportJars != null && exportJars.length > 0) {
                return exportJars[0].getAbsolutePath();
            }
        }

        return findJar(baseDir);
    }

    private File findExecutableInDir(File dir) {
        if (dir == null || !dir.exists()) return null;
        boolean isWin = PlatformInfo.isWindows();
        String exeName = isWin ? "evo.exe" : "evo";
        String shName = "evo.sh";

        try (java.util.stream.Stream<java.nio.file.Path> stream = java.nio.file.Files.walk(dir.toPath())) {
            java.util.List<File> files = stream.filter(java.nio.file.Files::isRegularFile)
                .map(java.nio.file.Path::toFile)
                .toList();

            for (File f : files) {
                if (f.getName().equalsIgnoreCase(exeName) && PlatformInfo.isValidExecutable(f)) {
                    return f;
                }
            }
            if (!isWin) {
                for (File f : files) {
                    if (f.getName().equalsIgnoreCase(shName) && PlatformInfo.isValidExecutable(f)) {
                        return f;
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static void unzip(File zipFile, File destDir) throws IOException {
        try (java.util.zip.ZipInputStream zipIn = new java.util.zip.ZipInputStream(new java.io.FileInputStream(zipFile))) {
            java.util.zip.ZipEntry entry = zipIn.getNextEntry();
            while (entry != null) {
                File filePath = new File(destDir, entry.getName());
                if (!entry.isDirectory()) {
                    if (filePath.getParentFile() != null && !filePath.getParentFile().exists()) {
                        filePath.getParentFile().mkdirs();
                    }
                    try (java.io.FileOutputStream bos = new java.io.FileOutputStream(filePath)) {
                        byte[] bytesIn = new byte[4096];
                        int read;
                        while ((read = zipIn.read(bytesIn)) != -1) {
                            bos.write(bytesIn, 0, read);
                        }
                    }
                } else {
                    filePath.mkdirs();
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
        }
    }

    private static void untar(File tarFile, File destDir) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("tar", "-xzf", tarFile.getAbsolutePath(), "-C", destDir.getAbsolutePath());
        Process p = pb.start();
        int code = p.waitFor();
        if (code != 0) {
            throw new IOException("tar extraction failed with exit code: " + code);
        }
    }

    private String findJar(File variantDir) {
        File targetDir = new File(variantDir, "target");
        if (targetDir.exists()) {
            File[] files = targetDir.listFiles((dir, name) -> name.endsWith(".jar") && !name.contains("sources") && !name.contains("supervisor"));
            if (files != null && files.length > 0) {
                return files[0].getAbsolutePath();
            }
        }
        return null;
    }

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
