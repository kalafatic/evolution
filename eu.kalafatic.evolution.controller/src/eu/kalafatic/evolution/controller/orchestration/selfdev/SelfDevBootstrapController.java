package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.kalafatic.evolution.controller.orchestration.ContextBuilder;
import eu.kalafatic.evolution.controller.orchestration.ContextPackage;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.Task;

public class SelfDevBootstrapController {

    private final File projectRoot;
    private final File runDir;
    private final Orchestrator orchestrator;
    private static volatile Process supervisorProcess;
    private boolean debugMode = false;

    public void setDebugMode(boolean debugMode) {
        System.out.println("[SelfDevBootstrapController] Debug mode set to: " + debugMode);
        this.debugMode = debugMode;
    }

    public boolean isDebugMode() {
        return this.debugMode;
    }

    public SelfDevBootstrapController(File projectRoot, Orchestrator orchestrator) {
        System.out.println("[SelfDevBootstrapController] Initializing. projectRoot: "
            + (projectRoot != null ? projectRoot.getAbsolutePath() : "null"));
        this.projectRoot = projectRoot;
        this.orchestrator = orchestrator;
        this.runDir = new File(projectRoot, "self-dev-run");
        if (!runDir.exists()) {
            boolean created = runDir.mkdirs();
            System.out.println("[SelfDevBootstrapController] Run directory created: " + created + " -> " + runDir.getAbsolutePath());
        } else {
            System.out.println("[SelfDevBootstrapController] Run directory already exists: " + runDir.getAbsolutePath());
        }
    }

    private void ensureSupervisorRunning() {
        System.out.println("[SelfDevBootstrapController] Checking if supervisor is alive...");
        if (isSupervisorAlive()) {
            System.out.println("[SelfDevBootstrapController] Supervisor is already running and responding.");
            return;
        }

        if (supervisorProcess != null) {
            System.out.println("[SelfDevBootstrapController] Supervisor process exists but is not responding to ping. Forcibly destroying existing process...");
            supervisorProcess.destroyForcibly();
            try {
                supervisorProcess.waitFor(2, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            supervisorProcess = null;
        }

        System.out.println("[SelfDevBootstrapController] Supervisor is not running. Starting new supervisor process...");
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add("java");
            if (debugMode) {
                cmd.add("-Devo.mode=debug");
                cmd.add("-Ddebug=true");
            }
            cmd.add("-jar");
            String supervisorJarPath = getSupervisorJarPath();
            cmd.add(supervisorJarPath);
            cmd.add(projectRoot.getAbsolutePath());
            if (debugMode) {
                cmd.add("--debug");
            }
            System.out.println("[SelfDevBootstrapController] Launch command: " + String.join(" ", cmd));
            ProcessBuilder pb = new ProcessBuilder(cmd);
            if (debugMode) {
                pb.environment().put("EVO_DEBUG", "true");
            }
            pb.directory(projectRoot);
            pb.redirectErrorStream(true);
            supervisorProcess = pb.start();
            System.out.println("[SelfDevBootstrapController] Supervisor process started successfully. PID details: " + supervisorProcess.toHandle().pid());
            
            new Thread(() -> {
                System.out.println("[SelfDevBootstrapController] Started thread to read Supervisor console stream.");
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(supervisorProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("[Supervisor] " + line);
                    }
                } catch (IOException e) {
                    System.err.println("[SelfDevBootstrapController] Exception while reading Supervisor output: " + e.getMessage());
                }
            }).start();

            waitUntilReady();
        } catch (IOException e) {
            System.err.println("[SelfDevBootstrapController] Failed to start Supervisor: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean isSupervisorAlive() {
        for (String host : new String[]{"127.0.0.1", "localhost"}) {
            try {
                URL url = new URL("http://" + host + ":8089/ping");
                System.out.println("[SelfDevBootstrapController] Pinging supervisor at: " + url);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(3000);
                int code = conn.getResponseCode();
                System.out.println("[SelfDevBootstrapController] Ping response code: " + code);
                if (code == 200) {
                    return true;
                }
            } catch (Exception e) {
                System.out.println("[SelfDevBootstrapController] Supervisor ping failed for " + host + ". Details: " + e.getMessage());
            }
        }
        return false;
    }

    private void waitUntilReady() {
        System.out.println("[SelfDevBootstrapController] Waiting for supervisor HTTP server to be ready (up to 10 seconds)...");
        for (int i = 0; i < 10; i++) {
            if (isSupervisorAlive()) {
                System.out.println("[SelfDevBootstrapController] Supervisor HTTP server is ready!");
                return;
            }
            try {
                System.out.println("[SelfDevBootstrapController] Sleeping 1 second (attempt " + (i + 1) + "/10)...");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                System.err.println("[SelfDevBootstrapController] Sleep interrupted during wait loop.");
                Thread.currentThread().interrupt();
            }
        }
        System.err.println("[SelfDevBootstrapController] Supervisor did not respond to ping within 10 seconds.");
    }

    private File findSupervisorDir() {
        File dir = projectRoot;
        File supervisorDir = null;

        // 0. Prioritize checking Git repository path first
        String gitPath = null;
        if (orchestrator != null && orchestrator.getGit() != null) {
            gitPath = orchestrator.getGit().getLocalPath();
        }
        if (gitPath == null || gitPath.isEmpty()) {
            gitPath = eu.kalafatic.evolution.controller.tools.EclipseGitEvoTool.getRepositoryPath(eu.kalafatic.evolution.controller.tools.EclipseGitEvoTool.REPO_EVOLUTION);
        }
        if (gitPath != null && !gitPath.isEmpty()) {
            File gitDir = new File(gitPath);
            File testDir = new File(gitDir, "eu.kalafatic.evolution.supervisor");
            System.out.println("[SelfDevBootstrapController] [SUPERVISOR_FIND] Checking Git repository path first: " + testDir.getAbsolutePath());
            if (testDir.exists() && new File(testDir, "pom.xml").exists()) {
                supervisorDir = testDir;
                System.out.println("[SelfDevBootstrapController] [SUPERVISOR_FIND] Found supervisor dir via Git repository scan: " + supervisorDir.getAbsolutePath());
                return supervisorDir;
            }
        }

        // 1. Scan upwards from projectRoot
        while (dir != null) {
            File testDir = new File(dir, "eu.kalafatic.evolution.supervisor");
            System.out.println("[SelfDevBootstrapController] [SUPERVISOR_FIND] Scanning parent path: " + dir.getAbsolutePath() + " for: " + testDir.getName());
            if (testDir.exists() && new File(testDir, "pom.xml").exists()) {
                supervisorDir = testDir;
                System.out.println("[SelfDevBootstrapController] [SUPERVISOR_FIND] Found supervisor dir via parent scan: " + supervisorDir.getAbsolutePath());
                break;
            }
            dir = dir.getParentFile();
        }

        // 2. Scan siblings of projectRoot as fallback
        if (supervisorDir == null && projectRoot != null && projectRoot.getParentFile() != null) {
            File testDir = new File(projectRoot.getParentFile(), "eu.kalafatic.evolution.supervisor");
            System.out.println("[SelfDevBootstrapController] [SUPERVISOR_FIND] Checking sibling path fallback: " + testDir.getAbsolutePath());
            if (testDir.exists() && new File(testDir, "pom.xml").exists()) {
                supervisorDir = testDir;
                System.out.println("[SelfDevBootstrapController] [SUPERVISOR_FIND] Found supervisor dir via sibling scan: " + supervisorDir.getAbsolutePath());
            }
        }

        // 3. Scan codebasePath as fallback
        String codebasePath = eu.kalafatic.evolution.controller.manager.ProjectModelManager.getCodebasePath();
        System.out.println("[SelfDevBootstrapController] [SUPERVISOR_FIND] ProjectModelManager.getCodebasePath() returned: " + codebasePath);
        if (supervisorDir == null && codebasePath != null) {
            File cbDir = new File(codebasePath);
            File testDir = new File(cbDir, "eu.kalafatic.evolution.supervisor");
            System.out.println("[SelfDevBootstrapController] [SUPERVISOR_FIND] Checking codebasePath path fallback: " + testDir.getAbsolutePath());
            if (testDir.exists() && new File(testDir, "pom.xml").exists()) {
                supervisorDir = testDir;
                System.out.println("[SelfDevBootstrapController] [SUPERVISOR_FIND] Found supervisor dir via codebasePath scan: " + supervisorDir.getAbsolutePath());
            }
        }

        // 4. Scan parent of codebasePath as fallback
        if (supervisorDir == null && codebasePath != null) {
            File cbDir = new File(codebasePath);
            if (cbDir.getParentFile() != null) {
                File testDir = new File(cbDir.getParentFile(), "eu.kalafatic.evolution.supervisor");
                System.out.println("[SelfDevBootstrapController] [SUPERVISOR_FIND] Checking parent of codebasePath path fallback: " + testDir.getAbsolutePath());
                if (testDir.exists() && new File(testDir, "pom.xml").exists()) {
                    supervisorDir = testDir;
                    System.out.println("[SelfDevBootstrapController] [SUPERVISOR_FIND] Found supervisor dir via parent codebasePath scan: " + supervisorDir.getAbsolutePath());
                }
            }
        }

        // 5. Scan using EclipseGitEvoTool fallback
        if (supervisorDir == null) {
            try {
                Class<?> gitToolClass = Class.forName("eu.kalafatic.evolution.controller.tools.EclipseGitEvoTool");
                java.lang.reflect.Method getEvoRepoMethod = gitToolClass.getMethod("getEvolutionRepository");
                String evoRepoPath = (String) getEvoRepoMethod.invoke(null);
                System.out.println("[SelfDevBootstrapController] [SUPERVISOR_FIND] EclipseGitEvoTool.getEvolutionRepository() returned: " + evoRepoPath);
                if (evoRepoPath != null) {
                    File evoDir = new File(evoRepoPath);
                    File testDir = new File(evoDir, "eu.kalafatic.evolution.supervisor");
                    System.out.println("[SelfDevBootstrapController] [SUPERVISOR_FIND] Checking EclipseGitEvoTool path fallback: " + testDir.getAbsolutePath());
                    if (testDir.exists() && new File(testDir, "pom.xml").exists()) {
                        supervisorDir = testDir;
                        System.out.println("[SelfDevBootstrapController] [SUPERVISOR_FIND] Found supervisor dir via EclipseGitEvoTool scan: " + supervisorDir.getAbsolutePath());
                    }
                }
            } catch (Throwable t) {
                System.out.println("[SelfDevBootstrapController] [SUPERVISOR_FIND] Failed to query EclipseGitEvoTool fallback: " + t.getMessage());
            }
        }

        // Fallback to projectRoot if still null
        if (supervisorDir == null) {
            supervisorDir = new File(projectRoot, "eu.kalafatic.evolution.supervisor");
            System.out.println("[SelfDevBootstrapController] [SUPERVISOR_FIND] All scans failed. Falling back to projectRoot: " + supervisorDir.getAbsolutePath());
        }

        return supervisorDir;
    }

    private String compileSupervisorModule(File supervisorDir) {
        try {
            System.out.println("[SelfDevBootstrapController] Compiling and packaging supervisor module: " + supervisorDir.getAbsolutePath());
            String mvnCmd = System.getProperty("os.name").toLowerCase().contains("win") ? "mvn.cmd" : "mvn";
            File parentDir = supervisorDir.getParentFile();
            System.out.println("[SelfDevBootstrapController] Executing build in parent directory: " + parentDir.getAbsolutePath() + " to package supervisor.");

            ProcessBuilder pbCompile = new ProcessBuilder(mvnCmd, "package", "-pl", "eu.kalafatic.evolution.supervisor", "-am", "-DskipTests");
            pbCompile.directory(parentDir);
            pbCompile.redirectErrorStream(true);
            Process pCompile = pbCompile.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(pCompile.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[Supervisor Compile] " + line);
                }
            }
            int compileExitCode = pCompile.waitFor();
            System.out.println("[SelfDevBootstrapController] Supervisor compile finished with exit code: " + compileExitCode);

            if (compileExitCode == 0) {
                return "SUCCESS";
            } else {
                System.out.println("[SelfDevBootstrapController] Reactor build failed. Falling back to standalone build directly inside: " + supervisorDir.getAbsolutePath());
                ProcessBuilder pbFallback = new ProcessBuilder(mvnCmd, "clean", "package", "-DskipTests");
                pbFallback.directory(supervisorDir);
                pbFallback.redirectErrorStream(true);
                Process pFallback = pbFallback.start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(pFallback.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("[Supervisor Standalone Compile] " + line);
                    }
                }
                int fallbackExitCode = pFallback.waitFor();
                System.out.println("[SelfDevBootstrapController] Supervisor standalone compile finished with exit code: " + fallbackExitCode);
                if (fallbackExitCode == 0) {
                    return "SUCCESS";
                } else {
                    return "ERROR: Supervisor build failed both in reactor and standalone (exit code " + fallbackExitCode + ")";
                }
            }
        } catch (Exception e) {
            System.err.println("[SelfDevBootstrapController] Failed to compile supervisor module: " + e.getMessage());
            e.printStackTrace();
            return "ERROR: " + e.getMessage();
        }
    }

    private String getSupervisorJarPath() {
        String dateStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("ddMMyy"));
        File customBinDir = new File(new File(System.getProperty("user.home"), "projects/evo/supervisor"), dateStr + "/bin");
        if (customBinDir.exists()) {
            File[] customJars = customBinDir.listFiles((dir, name) -> (name.endsWith("-shaded.jar") || name.endsWith(".jar")) && !name.startsWith("original-"));
            if (customJars != null && customJars.length > 0) {
                File runnableJar = customJars[0];
                for (File jar : customJars) {
                    if (jar.getName().contains("-shaded")) {
                        runnableJar = jar;
                        break;
                    }
                }
                String path = runnableJar.getAbsolutePath();
                System.out.println("[SelfDevBootstrapController] Found supervisor jar in custom bin: " + path);
                return path;
            }
        }

        File supervisorDir = findSupervisorDir();
        File targetDir = new File(supervisorDir, "target");
        System.out.println("[SelfDevBootstrapController] Scanning for supervisor shaded JAR in: " + targetDir.getAbsolutePath());

        File[] jars = targetDir.exists() ? targetDir.listFiles((dir, name) -> name.endsWith("-shaded.jar")) : null;
        if (jars == null || jars.length == 0) {
            File fallbackJar = new File(targetDir, "eu.kalafatic.evolution.supervisor-1.0.0-SNAPSHOT.jar");
            if (fallbackJar.exists()) {
                System.out.println("[SelfDevBootstrapController] Found fallback supervisor jar: " + fallbackJar.getAbsolutePath());
                jars = new File[]{fallbackJar};
            }
        }

        if (jars == null || jars.length == 0) {
            System.out.println("[SelfDevBootstrapController] Supervisor shaded JAR not found. Attempting to build supervisor module...");
            String buildResult = compileSupervisorModule(supervisorDir);
            System.out.println("[SelfDevBootstrapController] Supervisor build result: " + buildResult);
            jars = targetDir.exists() ? targetDir.listFiles((dir, name) -> name.endsWith("-shaded.jar")) : null;
            if (jars == null || jars.length == 0) {
                File fallbackJar = new File(targetDir, "eu.kalafatic.evolution.supervisor-1.0.0-SNAPSHOT.jar");
                if (fallbackJar.exists()) {
                    jars = new File[]{fallbackJar};
                }
            }
        }

        if (jars != null && jars.length > 0) {
            String path = jars[0].getAbsolutePath();
            System.out.println("[SelfDevBootstrapController] Found shaded supervisor jar: " + path);
            return path;
        }

        // Final fallback
        File fallbackJar = new File(targetDir, "eu.kalafatic.evolution.supervisor-1.0.0-SNAPSHOT.jar");
        System.out.println("[SelfDevBootstrapController] No shaded jar found, falling back to: " + fallbackJar.getAbsolutePath());
        return fallbackJar.getAbsolutePath();
    }

    public void startBootstrap() throws IOException {
        System.out.println("[SelfDevBootstrapController] [START_BOOTSTRAP] Initiated.");
        System.out.println("[SelfDevBootstrapController] [START_BOOTSTRAP] Variables: projectRoot=" + (projectRoot != null ? projectRoot.getAbsolutePath() : "null") + ", runDir=" + (runDir != null ? runDir.getAbsolutePath() : "null") + ", debugMode=" + debugMode);
        ensureSupervisorRunning();
        File stateFile = new File(runDir, "state.json");
        File contextFile = new File(runDir, "context.json");
        System.out.println("[SelfDevBootstrapController] [START_BOOTSTRAP] State File: " + stateFile.getAbsolutePath());
        System.out.println("[SelfDevBootstrapController] [START_BOOTSTRAP] Context File: " + contextFile.getAbsolutePath());

        JSONObject state = new JSONObject();
        state.put("active", true);
        state.put("iteration", 0);
        state.put("goal", "self-development");
        if (debugMode) {
            state.put("mode", "DEBUG");
            state.put("debug", true);
        } else {
            state.put("mode", "DARWIN");
        }
        state.put("plan", new JSONArray());
        state.put("contextPath", contextFile.getAbsolutePath());

        System.out.println("[SelfDevBootstrapController] [START_BOOTSTRAP] Writing state.json content: " + state.toString());
        Files.write(stateFile.toPath(), state.toString(4).getBytes());

        try {
            System.out.println("[SelfDevBootstrapController] [START_BOOTSTRAP] Building task context and serializing context.json...");
            Task task = OrchestrationFactory.eINSTANCE.createTask();
            task.setGoal("self-development");
            task.setName("Autonomous improvement");
            TaskContext taskContext = new TaskContext(orchestrator, projectRoot);
            ContextPackage pkg = ContextBuilder.build(task, taskContext);
            ObjectMapper mapper = new ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(contextFile, pkg);
            System.out.println("[SelfDevBootstrapController] [START_BOOTSTRAP] Context serialized successfully to context.json.");
        } catch (Exception e) {
            System.err.println("[SelfDevBootstrapController] [START_BOOTSTRAP_ERROR] Error generating context package during bootstrap: " + e.getMessage());
            e.printStackTrace();
        }

        JSONObject bootstrap = new JSONObject();
        bootstrap.put("sourcePath", projectRoot.getAbsolutePath());
        bootstrap.put("targetPath", new File(runDir, "workspace").getAbsolutePath());
        bootstrap.put("action", "BUILD_AND_START");
        bootstrap.put("statePath", stateFile.getAbsolutePath());

        File bootstrapFile = new File(runDir, "bootstrap.json");
        System.out.println("[SelfDevBootstrapController] [START_BOOTSTRAP] Writing bootstrap.json to: " + bootstrapFile.getAbsolutePath() + " with content: " + bootstrap.toString());
        Files.write(bootstrapFile.toPath(), bootstrap.toString(4).getBytes());
        System.out.println("[SelfDevBootstrapController] [START_BOOTSTRAP] Bootstrap process successfully started.");
    }

    public void stopBootstrap() {
        System.out.println("[SelfDevBootstrapController] Requesting to stop Supervisor...");
        if (supervisorProcess != null) {
            System.out.println("[SelfDevBootstrapController] Destroying supervisor process...");
            supervisorProcess.destroyForcibly();
            supervisorProcess = null;
            System.out.println("[SelfDevBootstrapController] Supervisor process destroyed.");
        } else {
            System.out.println("[SelfDevBootstrapController] Supervisor process was not running (null).");
        }
    }

    public JSONObject getStatus() {
        File statusFile = new File(runDir, "status.json");
        System.out.println("[SelfDevBootstrapController] Checking status.json existence: " + statusFile.getAbsolutePath());
        if (statusFile.exists()) {
            try {
                String content = new String(Files.readAllBytes(statusFile.toPath()));
                System.out.println("[SelfDevBootstrapController] Read status.json content: " + content.trim());
                if (!content.trim().isEmpty()) {
                    return new JSONObject(content);
                }
            } catch (Exception e) {
                System.err.println("[SelfDevBootstrapController] Failed to parse status.json: " + e.getMessage());
            }
        }
        boolean alive = isSupervisorAlive();
        System.out.println("[SelfDevBootstrapController] status.json not found or empty. Supervisor alive state: " + alive);
        return alive ? new JSONObject().put("phase", "RUNNING") : new JSONObject().put("phase", "STOPPED");
    }

    public boolean isRunning() {
        boolean alive = isSupervisorAlive();
        System.out.println("[SelfDevBootstrapController] isRunning check returned: " + alive);
        return alive;
    }

    public String check(String type) {
        String dashedBorder = "==========================================================================";
        System.out.println(dashedBorder);
        System.out.println("[PROCESS_VISUAL_CHECK] START PRE-FLIGHT CHECK: [" + type.toUpperCase() + "]");
        System.out.println("  [Ready] ──▶ [Checking] ──▶ [Verified/Error]");
        System.out.println("  Current Status Indicator: [Checking]");
        System.out.println("  Parameters:");
        System.out.println("    - projectRoot: " + (projectRoot != null ? projectRoot.getAbsolutePath() : "null"));
        System.out.println("    - runDir:      " + (runDir != null ? runDir.getAbsolutePath() : "null"));
        System.out.println("    - Mode:        " + (debugMode ? "DEBUG" : "STANDARD"));
        System.out.println(dashedBorder);

        if (type.equalsIgnoreCase("BUILD") || type.equalsIgnoreCase("SUPERVISOR") || type.equalsIgnoreCase("GIT_SUPERVISOR") || type.equalsIgnoreCase("MAVEN_SUPERVISOR") || type.equalsIgnoreCase("BUILD_SUPERVISOR") || type.equalsIgnoreCase("EXPORT_SUPERVISOR") || type.equalsIgnoreCase("START_EVO_SUPERVISOR") || type.equalsIgnoreCase("STOP_EVO_SUPERVISOR")) {
            ensureSupervisorRunning();
        }

        AbstractBootstrapTask task = switch (type.toUpperCase()) {
            case "GIT", "GIT_EVO" -> new AbstractBootstrapTask("Git Check", projectRoot, runDir) {
                @Override protected String run() throws Exception { return checkGit(); }
            };
            case "GIT_SUPERVISOR" -> new AbstractBootstrapTask("Git Check (Supervisor)", projectRoot, runDir) {
                @Override protected String run() throws Exception { return checkGitSupervisor(); }
            };
            case "MAVEN", "MAVEN_EVO" -> new AbstractBootstrapTask("Maven Check", projectRoot, runDir) {
                @Override protected String run() throws Exception { return checkMaven(); }
            };
            case "MAVEN_SUPERVISOR" -> new AbstractBootstrapTask("Maven Check (Supervisor)", projectRoot, runDir) {
                @Override protected String run() throws Exception { return checkMavenSupervisor(); }
            };
            case "SUPERVISOR" -> new AbstractBootstrapTask("Supervisor Check", projectRoot, runDir) {
                @Override protected String run() throws Exception { return checkSupervisor(); }
            };
            case "COPY_SUPERVISOR" -> new AbstractBootstrapTask("Copy Supervisor Source", projectRoot, runDir) {
                @Override protected String run() throws Exception { return copySupervisorSource(); }
            };
            case "BUILD_SUPERVISOR_LOCAL" -> new AbstractBootstrapTask("Build Supervisor Local", projectRoot, runDir) {
                @Override protected String run() throws Exception { return buildSupervisorLocal(); }
            };
            case "LLM" -> new AbstractBootstrapTask("LLM Check", projectRoot, runDir) {
                @Override protected String run() throws Exception { return checkLlm(); }
            };
            case "GENOME" -> new AbstractBootstrapTask("Genome Check", projectRoot, runDir) {
                @Override protected String run() throws Exception { return checkGenome(); }
            };
            case "PERMISSIONS" -> new AbstractBootstrapTask("Permissions Check", projectRoot, runDir) {
                @Override protected String run() throws Exception { return checkPermissions(); }
            };
            case "COPY" -> new AbstractBootstrapTask("Copy Source", projectRoot, runDir) {
                @Override protected String run() throws Exception { return copyCodebaseToSupervisorSource(); }
            };
            case "BUILD", "BUILD_EVO" -> new AbstractBootstrapTask("Build Project (Evo)", projectRoot, runDir) {
                @Override protected String run() throws Exception { return runBuildAndCopy(); }
            };
            case "BUILD_SUPERVISOR" -> new AbstractBootstrapTask("Build Project (Supervisor)", projectRoot, runDir) {
                @Override protected String run() throws Exception { return checkBuildSupervisor(); }
            };
            case "EXPORT", "EXPORT_EVO" -> new AbstractBootstrapTask("Export Product (Evo)", projectRoot, runDir) {
                @Override protected String run() throws Exception { return checkExport(); }
            };
            case "EXPORT_SUPERVISOR" -> new AbstractBootstrapTask("Export Product (Supervisor)", projectRoot, runDir) {
                @Override protected String run() throws Exception { return checkExportSupervisor(); }
            };
            case "START_EVO_SUPERVISOR" -> new AbstractBootstrapTask("Start Evo Product (Supervisor)", projectRoot, runDir) {
                @Override protected String run() throws Exception { return checkStartEvoSupervisor(); }
            };
            case "STOP_EVO_SUPERVISOR" -> new AbstractBootstrapTask("Stop Evo Product (Supervisor)", projectRoot, runDir) {
                @Override protected String run() throws Exception { return checkStopEvoSupervisor(); }
            };
            default -> null;
        };

        String result;
        if (task != null) {
            result = task.execute();
        } else {
            System.err.println("[SelfDevBootstrapController] [CHECK_UNKNOWN] Unknown check type requested: " + type);
            result = "UNKNOWN";
        }

        System.out.println(dashedBorder);
        System.out.println("[PROCESS_VISUAL_CHECK] PRE-FLIGHT CHECK TASK ENDED: [" + type.toUpperCase() + "]");
        System.out.println("  Result Outcome: [" + result + "]");
        System.out.println("  [Ready] ──▶ [Checking] ──▶ [" + (result.startsWith("ERROR") ? "Error" : "Verified") + "]");
        System.out.println(dashedBorder);
        return result;
    }

    private void findAndCopyJars(File dir, File buildDir, File exportDir) {
        if (dir.isDirectory()) {
            if (dir.getName().equals("target")) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.isFile() && f.getName().endsWith(".jar")) {
                            try {
                                File destJar = new File(buildDir, f.getName());
                                System.out.println("[SelfDevBootstrapController] [CHECK_BUILD] Copying produced jar to builds: " + destJar.getAbsolutePath());
                                Files.copy(f.toPath(), destJar.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                                if (exportDir != null) {
                                    File destExportJar = new File(exportDir, f.getName());
                                    System.out.println("[SelfDevBootstrapController] [CHECK_BUILD] Copying produced jar to export: " + destExportJar.getAbsolutePath());
                                    Files.copy(f.toPath(), destExportJar.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                                }
                            } catch (Exception e) {
                                System.err.println("[SelfDevBootstrapController] [CHECK_BUILD] Failed to copy " + f.getName() + ": " + e.getMessage());
                            }
                        }
                    }
                }
                return; // Don't recurse into target itself
            }
            File[] subDirs = dir.listFiles();
            if (subDirs != null) {
                for (File sub : subDirs) {
                    if (sub.isDirectory()) {
                        String name = sub.getName();
                        if (!name.equals(".git") && !name.equals("self-dev-run") && !name.equals(".settings") && !name.equals(".metadata") && !name.equals("bin") && !name.equals("iterations") && !name.equals("orchestrator")) {
                            findAndCopyJars(sub, buildDir, exportDir);
                        }
                    }
                }
            }
        }
    }

    private String runBuildAndCopy() {
        String buildWorkspacePath = null;
        if (orchestrator != null && orchestrator.getSupervisorSettings() != null) {
            buildWorkspacePath = orchestrator.getSupervisorSettings().getSourcePath();
        }
        if (buildWorkspacePath == null || buildWorkspacePath.trim().isEmpty()) {
            String dateStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("ddMMyy"));
            buildWorkspacePath = new File(new File(System.getProperty("user.home"), "projects/evo/supervisor"), dateStr + "/sources").getPath();
        }

        System.out.println("[SelfDevBootstrapController] [CHECK_BUILD] Starting local Maven build on sources folder: " + buildWorkspacePath);
        long startTime = System.currentTimeMillis();
        String response = "ERROR";
        try {
            String mvnCmd = System.getProperty("os.name").toLowerCase().contains("win") ? "mvn.cmd" : "mvn";
            ProcessBuilder pb = new ProcessBuilder(mvnCmd, "clean", "package", "-DskipTests");
            pb.directory(new File(buildWorkspacePath));
            pb.redirectErrorStream(true);
            Process p = pb.start();

            StringBuilder buildOutput = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    buildOutput.append(line).append("\n");
                    System.out.println("[Local Build] " + line);
                }
            }
            int exitCode = p.waitFor();
            long duration = System.currentTimeMillis() - startTime;
            System.out.println("[SelfDevBootstrapController] [CHECK_BUILD] Local build finished with exit code: " + exitCode + " (took " + duration + "ms)");
            if (exitCode == 0) {
                response = "SUCCESS (" + duration + "ms)";
            } else {
                response = "ERROR: Build failed with exit code " + exitCode;
            }
        } catch (Exception e) {
            System.err.println("[SelfDevBootstrapController] [CHECK_BUILD] Failed to run local build: " + e.getMessage());
            e.printStackTrace();
            response = "ERROR: " + e.getMessage();
        }

        if (response.startsWith("SUCCESS")) {
            try {
                String buildPath = null;
                if (orchestrator != null && orchestrator.getSupervisorSettings() != null) {
                    buildPath = orchestrator.getSupervisorSettings().getExecutablePath();
                }
                if (buildPath == null || buildPath.trim().isEmpty()) {
                    String dateStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("ddMMyy"));
                    buildPath = new File(new File(System.getProperty("user.home"), "projects/evo/supervisor"), dateStr + "/builds").getPath();
                }
                File buildDir = new File(buildPath);
                if (!buildDir.exists()) {
                    buildDir.mkdirs();
                }
                String exportPath = null;
                if (buildPath.endsWith("builds") || buildPath.endsWith("builds/") || buildPath.endsWith("builds\\")) {
                    exportPath = new File(buildDir.getParentFile(), "export").getPath();
                } else {
                    exportPath = buildPath + "/export";
                }
                File exportDir = new File(exportPath);
                if (!exportDir.exists()) {
                    exportDir.mkdirs();
                }

                findAndCopyJars(new File(buildWorkspacePath), buildDir, exportDir);

            } catch (Exception e) {
                System.err.println("[SelfDevBootstrapController] [CHECK_BUILD] Failed to copy build artifact: " + e.getMessage());
            }
        }
        return response;
    }

    private String copySupervisorSource() {
        long startTime = System.currentTimeMillis();
        System.out.println("[SelfDevBootstrapController] [COPY_SUPERVISOR] Starting Copy Supervisor Source...");
        File srcDir = findSupervisorDir();
        if (srcDir == null || !srcDir.exists()) {
            return "ERROR: Supervisor source directory not found";
        }
        String dateStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("ddMMyy"));
        File destDir = new File(new File(System.getProperty("user.home"), "projects/evo/supervisor"), dateStr + "/src");

        System.out.println("[SelfDevBootstrapController] [COPY_SUPERVISOR] Source: " + srcDir.getAbsolutePath());
        System.out.println("[SelfDevBootstrapController] [COPY_SUPERVISOR] Destination: " + destDir.getAbsolutePath());

        try {
            if (destDir.exists()) {
                deleteRecursively(destDir);
            }
            destDir.mkdirs();
            copyFolder(srcDir.toPath(), destDir.toPath());
            long duration = System.currentTimeMillis() - startTime;
            return "SUCCESS (" + duration + "ms)";
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR: " + e.getMessage();
        }
    }

    private void copyFolder(java.nio.file.Path source, java.nio.file.Path target) throws IOException {
        java.nio.file.Files.walkFileTree(source, new java.nio.file.SimpleFileVisitor<java.nio.file.Path>() {
            @Override
            public java.nio.file.FileVisitResult preVisitDirectory(java.nio.file.Path dir, java.nio.file.attribute.BasicFileAttributes attrs) throws IOException {
                if (dir.equals(source)) {
                    java.nio.file.Path targetDir = target.resolve(source.relativize(dir));
                    if (!java.nio.file.Files.exists(targetDir)) {
                        java.nio.file.Files.createDirectories(targetDir);
                    }
                    return java.nio.file.FileVisitResult.CONTINUE;
                }
                String name = dir.getFileName().toString();
                if (name.equals(".git") || name.equals("target") || name.equals("self-dev-run") ||
                    name.equals(".settings") || name.equals(".mvn") || name.equals(".metadata") ||
                    name.equals("bin") || name.equals("iterations") || name.equals("orchestrator")) {
                    return java.nio.file.FileVisitResult.SKIP_SUBTREE;
                }
                java.nio.file.Path targetDir = target.resolve(source.relativize(dir));
                if (!java.nio.file.Files.exists(targetDir)) {
                    java.nio.file.Files.createDirectories(targetDir);
                }
                return java.nio.file.FileVisitResult.CONTINUE;
            }

            @Override
            public java.nio.file.FileVisitResult visitFile(java.nio.file.Path file, java.nio.file.attribute.BasicFileAttributes attrs) throws IOException {
                String name = file.getFileName().toString();
                if (name.equals(".git") || name.equals("target") || name.equals("self-dev-run") ||
                    name.equals(".settings") || name.equals(".mvn") || name.equals(".metadata") ||
                    name.equals("bin") || name.equals("iterations") || name.equals("orchestrator") ||
                    name.equals("dependency-reduced-pom.xml")) {
                    return java.nio.file.FileVisitResult.CONTINUE;
                }
                java.nio.file.Path targetFile = target.resolve(source.relativize(file));
                java.nio.file.Files.copy(file, targetFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                return java.nio.file.FileVisitResult.CONTINUE;
            }
        });
    }

    private String buildSupervisorLocal() {
        long startTime = System.currentTimeMillis();
        System.out.println("[SelfDevBootstrapController] [BUILD_SUPERVISOR_LOCAL] Starting Build Supervisor...");

        if (supervisorProcess != null) {
            System.out.println("[SelfDevBootstrapController] [BUILD_SUPERVISOR_LOCAL] Forcibly destroying running supervisor process to unlock file lock...");
            supervisorProcess.destroyForcibly();
            try {
                supervisorProcess.waitFor(2, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {}
            supervisorProcess = null;
        }

        String dateStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("ddMMyy"));
        File srcDir = new File(new File(System.getProperty("user.home"), "projects/evo/supervisor"), dateStr + "/src");
        File binDir = new File(new File(System.getProperty("user.home"), "projects/evo/supervisor"), dateStr + "/bin");

        if (!srcDir.exists()) {
            return "ERROR: Supervisor src directory not found. Run 'Copy Supervisor Source' first.";
        }
        if (!binDir.exists()) {
            binDir.mkdirs();
        }

        try {
            System.out.println("[SelfDevBootstrapController] [BUILD_SUPERVISOR_LOCAL] Executing build in: " + srcDir.getAbsolutePath());
            String mvnCmd = System.getProperty("os.name").toLowerCase().contains("win") ? "mvn.cmd" : "mvn";
            ProcessBuilder pb = new ProcessBuilder(mvnCmd, "clean", "package", "-DskipTests");
            pb.directory(srcDir);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[Local Supervisor Build] " + line);
                }
            }
            int exitCode = p.waitFor();
            long duration = System.currentTimeMillis() - startTime;
            if (exitCode == 0) {
                File targetDir = new File(srcDir, "target");
                File[] jars = targetDir.listFiles((dir, name) -> name.endsWith(".jar") && !name.startsWith("original-"));
                if (jars != null) {
                    for (File jar : jars) {
                        Files.copy(jar.toPath(), new File(binDir, jar.getName()).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                return "SUCCESS (" + duration + "ms)";
            } else {
                return "ERROR: Maven build failed with exit code " + exitCode;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR: " + e.getMessage();
        }
    }

    private String encode(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }

    private String callSupervisor(String endpoint) {
        System.out.println("[SelfDevBootstrapController] Calling supervisor endpoint: " + endpoint);
        Exception lastEx = null;
        for (String host : new String[]{"127.0.0.1", "localhost"}) {
            try {
                URL url = new URL("http://" + host + ":8089" + endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                int responseCode = conn.getResponseCode();
                System.out.println("[SelfDevBootstrapController] Supervisor HTTP " + responseCode + " for: " + endpoint);
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    StringBuilder res = new StringBuilder();
                    while ((line = reader.readLine()) != null) res.append(line);
                    String body = res.toString();
                    System.out.println("[SelfDevBootstrapController] Supervisor response body: " + body);
                    return body;
                }
            } catch (Exception e) {
                System.err.println("[SelfDevBootstrapController] Failed to call supervisor on " + host + " for " + endpoint + ": " + e.getMessage());
                lastEx = e;
            }
        }
        return "ERROR: " + (lastEx != null ? lastEx.getMessage() : "Unknown connection error");
    }

    private String checkBuildSupervisor() {
        System.out.println("[SelfDevBootstrapController] [CHECK_BUILD_SUPERVISOR] Starting build check on Supervisor...");
        ensureSupervisorRunning();
        String buildWorkspacePath = null;
        if (orchestrator != null && orchestrator.getSupervisorSettings() != null) {
            buildWorkspacePath = orchestrator.getSupervisorSettings().getSourcePath();
        }
        if (buildWorkspacePath == null || buildWorkspacePath.trim().isEmpty()) {
            String dateStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("ddMMyy"));
            buildWorkspacePath = new File(new File(System.getProperty("user.home"), "projects/evo/supervisor"), dateStr + "/sources").getPath();
        }
        String endpoint = "/build?path=" + encode(buildWorkspacePath);
        String response = callSupervisor(endpoint);
        System.out.println("[SelfDevBootstrapController] [CHECK_BUILD_SUPERVISOR] Supervisor response: " + response);
        return response;
    }

    private String checkExportSupervisor() {
        System.out.println("[SelfDevBootstrapController] [CHECK_EXPORT_SUPERVISOR] Starting export check on Supervisor...");
        ensureSupervisorRunning();
        String buildWorkspacePath = null;
        if (orchestrator != null && orchestrator.getSupervisorSettings() != null) {
            buildWorkspacePath = orchestrator.getSupervisorSettings().getSourcePath();
        }
        if (buildWorkspacePath == null || buildWorkspacePath.trim().isEmpty()) {
            String dateStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("ddMMyy"));
            buildWorkspacePath = new File(new File(System.getProperty("user.home"), "projects/evo/supervisor"), dateStr + "/sources").getPath();
        }
        String endpoint = "/export?path=" + encode(buildWorkspacePath);
        String response = callSupervisor(endpoint);
        System.out.println("[SelfDevBootstrapController] [CHECK_EXPORT_SUPERVISOR] Supervisor response: " + response);
        return response;
    }

    private String checkStartEvoSupervisor() {
        System.out.println("[SelfDevBootstrapController] [CHECK_START_EVO_SUPERVISOR] Starting export evo check on Supervisor...");
        ensureSupervisorRunning();
        String buildWorkspacePath = null;
        if (orchestrator != null && orchestrator.getSupervisorSettings() != null) {
            buildWorkspacePath = orchestrator.getSupervisorSettings().getSourcePath();
        }
        if (buildWorkspacePath == null || buildWorkspacePath.trim().isEmpty()) {
            String dateStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("ddMMyy"));
            buildWorkspacePath = new File(new File(System.getProperty("user.home"), "projects/evo/supervisor"), dateStr + "/sources").getPath();
        }
        String endpoint = "/start-evo?path=" + encode(buildWorkspacePath);
        String response = callSupervisor(endpoint);
        System.out.println("[SelfDevBootstrapController] [CHECK_START_EVO_SUPERVISOR] Supervisor response: " + response);
        return response;
    }

    private String checkStopEvoSupervisor() {
        System.out.println("[SelfDevBootstrapController] [CHECK_STOP_EVO_SUPERVISOR] Starting stop evo check on Supervisor...");
        ensureSupervisorRunning();
        String response = callSupervisor("/stop-evo");
        System.out.println("[SelfDevBootstrapController] [CHECK_STOP_EVO_SUPERVISOR] Supervisor response: " + response);
        return response;
    }

    private String checkGitSupervisor() {
        System.out.println("[SelfDevBootstrapController] [CHECK_GIT_SUPERVISOR] Starting Git configuration check on Supervisor...");
        ensureSupervisorRunning();
        String localPath = null;
        if (orchestrator != null && orchestrator.getGit() != null) {
            localPath = orchestrator.getGit().getLocalPath();
        }
        if (localPath == null || localPath.isEmpty()) {
            localPath = eu.kalafatic.evolution.controller.tools.EclipseGitEvoTool.getRepositoryPath(eu.kalafatic.evolution.controller.tools.EclipseGitEvoTool.REPO_EVOLUTION);
        }
        String endpoint = "/git-check";
        if (localPath != null && !localPath.isEmpty()) {
            endpoint += "?path=" + encode(localPath);
        }
        String response = callSupervisor(endpoint);
        System.out.println("[SelfDevBootstrapController] [CHECK_GIT_SUPERVISOR] Supervisor response: " + response);
        return response;
    }

    private String checkMavenSupervisor() {
        System.out.println("[SelfDevBootstrapController] [CHECK_MAVEN_SUPERVISOR] Starting Maven check on Supervisor...");
        ensureSupervisorRunning();
        String response = callSupervisor("/maven-check");
        System.out.println("[SelfDevBootstrapController] [CHECK_MAVEN_SUPERVISOR] Supervisor response: " + response);
        return response;
    }

    private String checkSupervisor() {
        System.out.println("[SelfDevBootstrapController] [CHECK_SUPERVISOR] Checking external supervisor...");
        System.out.println("[SelfDevBootstrapController] [CHECK_SUPERVISOR] Running supervisor...");
        ensureSupervisorRunning();
        if (isSupervisorAlive()) {
            System.out.println("[SelfDevBootstrapController] [CHECK_SUPERVISOR_SUCCESS] Supervisor is running and responsive.");
            return "CHECKED (Running)";
        } else {
            System.err.println("[SelfDevBootstrapController] [CHECK_SUPERVISOR_FAIL] Supervisor failed to respond to ping.");
            return "ERROR: Supervisor not responding";
        }
    }

    private String checkGit() {
        long startTime = System.currentTimeMillis();
        System.out.println("[SelfDevBootstrapController] [CHECK_GIT] Starting Git configuration check...");

        String localPath = null;
        String repositoryUrl = null;
        if (orchestrator != null && orchestrator.getGit() != null) {
            localPath = orchestrator.getGit().getLocalPath();
            repositoryUrl = orchestrator.getGit().getRepositoryUrl();
        }
        if (localPath == null || localPath.isEmpty()) {
            localPath = eu.kalafatic.evolution.controller.tools.EclipseGitEvoTool.getRepositoryPath(eu.kalafatic.evolution.controller.tools.EclipseGitEvoTool.REPO_EVOLUTION);
        }
        if (repositoryUrl == null || repositoryUrl.isEmpty()) {
            repositoryUrl = eu.kalafatic.evolution.controller.tools.EclipseGitEvoTool.getRepositoryRemote(eu.kalafatic.evolution.controller.tools.EclipseGitEvoTool.REPO_EVOLUTION);
        }

        File localDir = new File(localPath);
        System.out.println("[SelfDevBootstrapController] [CHECK_GIT] Local path: " + localDir.getAbsolutePath());
        System.out.println("[SelfDevBootstrapController] [CHECK_GIT] Repository URL: " + repositoryUrl);

        String gitAction = "";
        try {
            if (!localDir.exists()) {
                localDir.mkdirs();
            }
            File gitDir = new File(localDir, ".git");
            if (!gitDir.exists()) {
                // Clone
                System.out.println("[SelfDevBootstrapController] [CHECK_GIT] Local folder does not contain .git. Starting clone...");
                org.eclipse.jgit.api.CloneCommand cloneCmd = org.eclipse.jgit.api.Git.cloneRepository()
                    .setURI(repositoryUrl)
                    .setDirectory(localDir)
                    .setCloneAllBranches(true)
                    .setBare(false);

                if (orchestrator != null && orchestrator.getGit() != null) {
                    String user = orchestrator.getGit().getUsername();
                    String pass = orchestrator.getGit().getPassword();
                    if (user != null && !user.isEmpty() && pass != null && !pass.isEmpty()) {
                        cloneCmd.setCredentialsProvider(new org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider(user, pass));
                    }
                }
                try (org.eclipse.jgit.api.Git git = cloneCmd.call()) {
                    System.out.println("[SelfDevBootstrapController] [CHECK_GIT_SUCCESS] Clone completed successfully.");
                    gitAction = " (Cloned)";
                }
            } else {
                // Pull
                System.out.println("[SelfDevBootstrapController] [CHECK_GIT] Local folder is already a git repository. Starting pull...");
                try (org.eclipse.jgit.api.Git git = org.eclipse.jgit.api.Git.open(localDir)) {
                    org.eclipse.jgit.api.PullCommand pullCmd = git.pull();
                    if (orchestrator != null && orchestrator.getGit() != null) {
                        String user = orchestrator.getGit().getUsername();
                        String pass = orchestrator.getGit().getPassword();
                        if (user != null && !user.isEmpty() && pass != null && !pass.isEmpty()) {
                            pullCmd.setCredentialsProvider(new org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider(user, pass));
                        }
                    }
                    pullCmd.call();
                    System.out.println("[SelfDevBootstrapController] [CHECK_GIT_SUCCESS] Pull completed successfully.");
                    gitAction = " (Pulled)";
                }
            }
        } catch (Exception e) {
            System.err.println("[SelfDevBootstrapController] [CHECK_GIT_ERROR] JGit clone/pull failed: " + e.getMessage() + ". Trying OS process fallback.");
            try {
                File gitDir = new File(localDir, ".git");
                if (!gitDir.exists()) {
                    System.out.println("[SelfDevBootstrapController] [CHECK_GIT] Fallback 'git clone' in progress...");
                    ProcessBuilder pb = new ProcessBuilder("git", "clone", repositoryUrl, localDir.getAbsolutePath());
                    pb.redirectErrorStream(true);
                    Process p = pb.start();
                    p.waitFor();
                    gitAction = " (Cloned)";
                } else {
                    System.out.println("[SelfDevBootstrapController] [CHECK_GIT] Fallback 'git pull' in progress...");
                    ProcessBuilder pb = new ProcessBuilder("git", "pull");
                    pb.directory(localDir);
                    pb.redirectErrorStream(true);
                    Process p = pb.start();
                    p.waitFor();
                    gitAction = " (Pulled)";
                }
            } catch (Exception ex) {
                System.err.println("[SelfDevBootstrapController] [CHECK_GIT_ERROR] Fallback clone/pull failed: " + ex.getMessage());
                return "ERROR: clone/pull failed: " + ex.getMessage();
            }
        }

        try {
            System.out.println("[SelfDevBootstrapController] [CHECK_GIT] Executing 'git status --porcelain' in directory: " + localDir.getAbsolutePath());
            ProcessBuilder pb = new ProcessBuilder("git", "status", "--porcelain");
            pb.directory(localDir);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    output.append(line).append("\n");
                    System.out.println("[SelfDevBootstrapController] [CHECK_GIT_OUTPUT] " + line);
                }
            }

            int exitCode = p.waitFor();
            long duration = System.currentTimeMillis() - startTime;
            System.out.println("[SelfDevBootstrapController] [CHECK_GIT] 'git status --porcelain' exited with code: " + exitCode + " (took " + duration + "ms)");
            if (exitCode == 0) {
                System.out.println("[SelfDevBootstrapController] [CHECK_GIT_SUCCESS] Git repository is valid. Pending changes count: " + output.toString().split("\n").length);
                return "CHECKED" + gitAction;
            } else {
                System.err.println("[SelfDevBootstrapController] [CHECK_GIT_FAIL] Git command failed with exit code: " + exitCode);
                return "ERROR: git command failed with exit code " + exitCode;
            }
        } catch (Exception e) {
            System.err.println("[SelfDevBootstrapController] [CHECK_GIT_ERROR] Exception occurred: " + e.getMessage());
            e.printStackTrace();
            return "ERROR: " + e.getMessage();
        }
    }

    private String checkMaven() {
        long startTime = System.currentTimeMillis();
        System.out.println("[SelfDevBootstrapController] [CHECK_MAVEN] Starting Maven check...");
        System.out.println("[SelfDevBootstrapController] [CHECK_MAVEN] Project Root: " + (projectRoot != null ? projectRoot.getAbsolutePath() : "null"));
        try {
            String mvnCmd = System.getProperty("os.name").toLowerCase().contains("win") ? "mvn.cmd" : "mvn";
            System.out.println("[SelfDevBootstrapController] [CHECK_MAVEN] OS: " + System.getProperty("os.name") + ", Maven Executable: " + mvnCmd);
            System.out.println("[SelfDevBootstrapController] [CHECK_MAVEN] Executing '" + mvnCmd + " -version' in directory: " + projectRoot.getAbsolutePath());
            ProcessBuilder pb = new ProcessBuilder(mvnCmd, "-version");
            pb.directory(projectRoot);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    output.append(line).append("\n");
                    System.out.println("[SelfDevBootstrapController] [CHECK_MAVEN_OUTPUT] " + line);
                }
            }

            int exitCode = p.waitFor();
            long duration = System.currentTimeMillis() - startTime;
            System.out.println("[SelfDevBootstrapController] [CHECK_MAVEN] '" + mvnCmd + " -version' exited with code: " + exitCode + " (took " + duration + "ms)");
            if (exitCode == 0) {
                System.out.println("[SelfDevBootstrapController] [CHECK_MAVEN_SUCCESS] Maven installation verified.");
                return "CHECKED";
            }
            System.err.println("[SelfDevBootstrapController] [CHECK_MAVEN_FAIL] Maven command failed with exit code " + exitCode);
            return "ERROR: Maven command failed with exit code " + exitCode;
        } catch (Exception e) {
            System.err.println("[SelfDevBootstrapController] [CHECK_MAVEN_ERROR] Exception: " + e.getMessage());
            e.printStackTrace();
            return "ERROR: Maven not found or executable issues: " + e.getMessage();
        }
    }

    private String checkLlm() {
        long startTime = System.currentTimeMillis();
        System.out.println("[SelfDevBootstrapController] [CHECK_LLM] Starting LLM connectivity check...");
        try {
            if (orchestrator != null && !orchestrator.getAiProviders().isEmpty()) {
                System.out.println("[SelfDevBootstrapController] [CHECK_LLM] Active AI providers in orchestrator: " + orchestrator.getAiProviders().size());
                for (int i = 0; i < orchestrator.getAiProviders().size(); i++) {
                    System.out.println("[SelfDevBootstrapController] [CHECK_LLM] Provider #" + i + ": " + orchestrator.getAiProviders().get(i).toString());
                }
                long duration = System.currentTimeMillis() - startTime;
                System.out.println("[SelfDevBootstrapController] [CHECK_LLM_SUCCESS] Orchestrator has active AI providers (took " + duration + "ms). LLM check passed.");
                return "CHECKED";
            }
            URL url = new URL("http://localhost:11434/api/tags");
            System.out.println("[SelfDevBootstrapController] [CHECK_LLM] Pinging Ollama URL: " + url.toString());
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(2000);
            con.setReadTimeout(2000);
            int code = con.getResponseCode();
            long duration = System.currentTimeMillis() - startTime;
            System.out.println("[SelfDevBootstrapController] [CHECK_LLM] Ollama response code: " + code + " (took " + duration + "ms)");
            if (code == 200) {
                System.out.println("[SelfDevBootstrapController] [CHECK_LLM_SUCCESS] Ollama is online and responsive.");
                return "CHECKED";
            }
            System.err.println("[SelfDevBootstrapController] [CHECK_LLM_FAIL] LLM unreachable. HTTP Code: " + code);
            return "ERROR: LLM unreachable. HTTP Code: " + code;
        } catch (Exception e) {
            System.err.println("[SelfDevBootstrapController] [CHECK_LLM_ERROR] Connectivity check failed with exception: " + e.getMessage());
            e.printStackTrace();
            return "ERROR: " + e.getMessage();
        }
    }

    private String compileGenomeModule(File genomeModuleDir) {
        try {
            System.out.println("[SelfDevBootstrapController] Compiling and packaging genome module: " + genomeModuleDir.getAbsolutePath());
            String mvnCmd = System.getProperty("os.name").toLowerCase().contains("win") ? "mvn.cmd" : "mvn";
            File parentDir = genomeModuleDir.getParentFile();
            System.out.println("[SelfDevBootstrapController] Executing build in parent directory: " + parentDir.getAbsolutePath() + " to resolve reactor siblings.");

            // Step 1: Clean ONLY the genome module
            System.out.println("[SelfDevBootstrapController] Step 1: Running clean on eu.kalafatic.evolution.selfdev.genome only");
            ProcessBuilder pbClean = new ProcessBuilder(mvnCmd, "clean", "-pl", "eu.kalafatic.evolution.selfdev.genome");
            pbClean.directory(parentDir);
            pbClean.redirectErrorStream(true);
            Process pClean = pbClean.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(pClean.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[Genome Clean] " + line);
                }
            }
            int cleanExitCode = pClean.waitFor();
            System.out.println("[SelfDevBootstrapController] Genome clean finished with exit code: " + cleanExitCode);

            // Step 2: Compile the genome module and dependencies as needed, without cleaning them
            System.out.println("[SelfDevBootstrapController] Step 2: Running compile on eu.kalafatic.evolution.selfdev.genome with dependencies");
            ProcessBuilder pbCompile = new ProcessBuilder(mvnCmd, "compile", "-pl", "eu.kalafatic.evolution.selfdev.genome", "-am", "-DskipTests");
            pbCompile.directory(parentDir);
            pbCompile.redirectErrorStream(true);
            Process pCompile = pbCompile.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(pCompile.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[Genome Compile] " + line);
                }
            }
            int compileExitCode = pCompile.waitFor();
            System.out.println("[SelfDevBootstrapController] Genome compile finished with exit code: " + compileExitCode);

            if (cleanExitCode == 0 && compileExitCode == 0) {
                return "SUCCESS";
            } else {
                return "ERROR: Build failed (clean exit code " + cleanExitCode + ", compile exit code " + compileExitCode + ")";
            }
        } catch (Exception e) {
            System.err.println("[SelfDevBootstrapController] Failed to compile genome module: " + e.getMessage());
            e.printStackTrace();
            return "ERROR: " + e.getMessage();
        }
    }

    private String checkGenome() {
        long startTime = System.currentTimeMillis();
        System.out.println("[SelfDevBootstrapController] [CHECK_GENOME] Starting GENOME Check. projectRoot: " + (projectRoot != null ? projectRoot.getAbsolutePath() : "null"));
        File genomeModuleDir = null;

        // 0. Prioritize checking Git repository path first
        String gitPath = null;
        if (orchestrator != null && orchestrator.getGit() != null) {
            gitPath = orchestrator.getGit().getLocalPath();
        }
        if (gitPath == null || gitPath.isEmpty()) {
            gitPath = eu.kalafatic.evolution.controller.tools.EclipseGitEvoTool.getRepositoryPath(eu.kalafatic.evolution.controller.tools.EclipseGitEvoTool.REPO_EVOLUTION);
        }
        if (gitPath != null && !gitPath.isEmpty()) {
            File gitDir = new File(gitPath);
            File testDir = new File(gitDir, "eu.kalafatic.evolution.selfdev.genome");
            System.out.println("[SelfDevBootstrapController] [CHECK_GENOME] Checking Git repository path first: " + testDir.getAbsolutePath());
            if (testDir.exists() && new File(testDir, "pom.xml").exists()) {
                genomeModuleDir = testDir;
                System.out.println("[SelfDevBootstrapController] [CHECK_GENOME] Found genome module dir via Git repository scan: " + genomeModuleDir.getAbsolutePath());
            }
        }

        if (genomeModuleDir == null) {
            File dir = projectRoot;
            // 1. Scan upwards from projectRoot
            while (dir != null) {
                File testDir = new File(dir, "eu.kalafatic.evolution.selfdev.genome");
                System.out.println("[SelfDevBootstrapController] [CHECK_GENOME] Scanning parent path: " + dir.getAbsolutePath() + " for: " + testDir.getName());
                if (testDir.exists() && new File(testDir, "pom.xml").exists()) {
                    genomeModuleDir = testDir;
                    System.out.println("[SelfDevBootstrapController] [CHECK_GENOME] Found genome module dir via parent scan: " + genomeModuleDir.getAbsolutePath());
                    break;
                }
                dir = dir.getParentFile();
            }
        }

        // 2. Scan siblings of projectRoot as fallback
        if (genomeModuleDir == null && projectRoot != null && projectRoot.getParentFile() != null) {
            File testDir = new File(projectRoot.getParentFile(), "eu.kalafatic.evolution.selfdev.genome");
            System.out.println("[SelfDevBootstrapController] [CHECK_GENOME] Checking sibling path fallback: " + testDir.getAbsolutePath());
            if (testDir.exists() && new File(testDir, "pom.xml").exists()) {
                genomeModuleDir = testDir;
                System.out.println("[SelfDevBootstrapController] [CHECK_GENOME] Found genome module dir via sibling scan: " + genomeModuleDir.getAbsolutePath());
            }
        }

        // 3. Scan codebasePath as fallback
        String codebasePath = eu.kalafatic.evolution.controller.manager.ProjectModelManager.getCodebasePath();
        System.out.println("[SelfDevBootstrapController] [CHECK_GENOME] ProjectModelManager.getCodebasePath() returned: " + codebasePath);
        if (genomeModuleDir == null && codebasePath != null) {
            File cbDir = new File(codebasePath);
            File testDir = new File(cbDir, "eu.kalafatic.evolution.selfdev.genome");
            System.out.println("[SelfDevBootstrapController] [CHECK_GENOME] Checking codebasePath path fallback: " + testDir.getAbsolutePath());
            if (testDir.exists() && new File(testDir, "pom.xml").exists()) {
                genomeModuleDir = testDir;
                System.out.println("[SelfDevBootstrapController] [CHECK_GENOME] Found genome module dir via codebasePath scan: " + genomeModuleDir.getAbsolutePath());
            }
        }

        // 4. Scan parent of codebasePath as fallback
        if (genomeModuleDir == null && codebasePath != null) {
            File cbDir = new File(codebasePath);
            if (cbDir.getParentFile() != null) {
                File testDir = new File(cbDir.getParentFile(), "eu.kalafatic.evolution.selfdev.genome");
                System.out.println("[SelfDevBootstrapController] [CHECK_GENOME] Checking parent of codebasePath path fallback: " + testDir.getAbsolutePath());
                if (testDir.exists() && new File(testDir, "pom.xml").exists()) {
                    genomeModuleDir = testDir;
                    System.out.println("[SelfDevBootstrapController] [CHECK_GENOME] Found genome module dir via parent codebasePath scan: " + genomeModuleDir.getAbsolutePath());
                }
            }
        }

        // 5. Scan using EclipseGitEvoTool fallback
        if (genomeModuleDir == null) {
            try {
                Class<?> gitToolClass = Class.forName("eu.kalafatic.evolution.controller.tools.EclipseGitEvoTool");
                java.lang.reflect.Method getEvoRepoMethod = gitToolClass.getMethod("getEvolutionRepository");
                String evoRepoPath = (String) getEvoRepoMethod.invoke(null);
                System.out.println("[SelfDevBootstrapController] [CHECK_GENOME] EclipseGitEvoTool.getEvolutionRepository() returned: " + evoRepoPath);
                if (evoRepoPath != null) {
                    File evoDir = new File(evoRepoPath);
                    File testDir = new File(evoDir, "eu.kalafatic.evolution.selfdev.genome");
                    System.out.println("[SelfDevBootstrapController] [CHECK_GENOME] Checking EclipseGitEvoTool path fallback: " + testDir.getAbsolutePath());
                    if (testDir.exists() && new File(testDir, "pom.xml").exists()) {
                        genomeModuleDir = testDir;
                        System.out.println("[SelfDevBootstrapController] [CHECK_GENOME] Found genome module dir via EclipseGitEvoTool scan: " + genomeModuleDir.getAbsolutePath());
                    }
                }
            } catch (Throwable t) {
                System.out.println("[SelfDevBootstrapController] [CHECK_GENOME] Failed to query EclipseGitEvoTool fallback: " + t.getMessage());
            }
        }

        if (genomeModuleDir == null) {
            System.err.println("[SelfDevBootstrapController] [CHECK_GENOME_FAIL] Genome module 'eu.kalafatic.evolution.selfdev.genome' could not be located in any scanned directory.");
            return "ERROR: Genome module missing";
        }

        // Compile/build the genome module using Maven to ensure it is built/deployed/exported properly
        System.out.println("[SelfDevBootstrapController] [CHECK_GENOME] Compiling genome module. Path: " + genomeModuleDir.getAbsolutePath());
        String buildRes = compileGenomeModule(genomeModuleDir);
        System.out.println("[SelfDevBootstrapController] [CHECK_GENOME] Compilation finished. Result: " + buildRes);
        if (buildRes.startsWith("ERROR")) {
            System.err.println("[SelfDevBootstrapController] [CHECK_GENOME_FAIL] GENOME Check failed during module compilation: " + buildRes);
            return "ERROR: Genome module build/compilation failed: " + buildRes;
        }

        try {
            System.out.println("[SelfDevBootstrapController] [CHECK_GENOME] Integrating and updating project genome in projectRoot: " + projectRoot.getAbsolutePath() + ", projectName: " + projectRoot.getName());
            
            Class<?> hubClass = null;
            try {
                hubClass = Class.forName("eu.kalafatic.evolution.selfdev.genome.hub.SelfDevGenomeHub");
                System.out.println("[SelfDevBootstrapController] [CHECK_GENOME] Loaded SelfDevGenomeHub via default Class.forName");
            } catch (Throwable t) {
                System.out.println("[SelfDevBootstrapController] [CHECK_GENOME] Default ClassLoader failed to load SelfDevGenomeHub: " + t.getMessage() + ". Trying URLClassLoader fallback...");
                File classesDir = new File(genomeModuleDir, "target/classes");
                if (classesDir.exists()) {
                    java.net.URL[] urls = new java.net.URL[] { classesDir.toURI().toURL() };
                    ClassLoader parentLoader = SelfDevBootstrapController.class.getClassLoader();
                    java.net.URLClassLoader urlLoader = new java.net.URLClassLoader(urls, parentLoader);
                    hubClass = urlLoader.loadClass("eu.kalafatic.evolution.selfdev.genome.hub.SelfDevGenomeHub");
                    System.out.println("[SelfDevBootstrapController] [CHECK_GENOME] Loaded SelfDevGenomeHub via URLClassLoader pointing to: " + classesDir.getAbsolutePath());
                } else {
                    throw new ClassNotFoundException("Genome target/classes directory not found: " + classesDir.getAbsolutePath(), t);
                }
            }

            java.lang.reflect.Method getInstanceMethod = hubClass.getMethod("getInstance");
            Object hubInstance = getInstanceMethod.invoke(null);
            
            java.lang.reflect.Method updateGenomeMethod = hubClass.getMethod("updateGenome", File.class, String.class, String.class);
            updateGenomeMethod.invoke(hubInstance, projectRoot, projectRoot.getName(), "v1.0.0");

            File genomeJson = new File(projectRoot, "genome/current/genome.json");
            System.out.println("[SelfDevBootstrapController] [CHECK_GENOME] Checking generated genome.json path: " + genomeJson.getAbsolutePath() + ", exists: " + genomeJson.exists());
            long duration = System.currentTimeMillis() - startTime;
            if (genomeJson.exists()) {
                System.out.println("[SelfDevBootstrapController] [CHECK_GENOME_SUCCESS] GENOME Check successful. Generated genome.json: " + genomeJson.getAbsolutePath() + " (size: " + genomeJson.length() + " bytes, took " + duration + "ms)");
                return "CHECKED (Updated)";
            } else {
                System.err.println("[SelfDevBootstrapController] [CHECK_GENOME_FAIL] GENOME Check failed: genome.json was not generated in project root: " + projectRoot.getAbsolutePath());
                return "ERROR: Failed to generate genome.json in project root";
            }
        } catch (Throwable e) {
            System.err.println("[SelfDevBootstrapController] [CHECK_GENOME_ERROR] GENOME Check failed with Throwable during updateGenome execution: " + e.getMessage());
            e.printStackTrace();
            return "ERROR: Genome update failed: " + e.getMessage();
        }
    }

    private String checkPermissions() {
        long startTime = System.currentTimeMillis();
        System.out.println("[SelfDevBootstrapController] [CHECK_PERMISSIONS] Starting filesystem permissions check...");
        System.out.println("[SelfDevBootstrapController] [CHECK_PERMISSIONS] RunDir: " + runDir.getAbsolutePath() + ", exists: " + runDir.exists() + ", canRead: " + runDir.canRead() + ", canWrite: " + runDir.canWrite());
        if (runDir.exists() && runDir.canWrite()) {
            File testFile = new File(runDir, ".perm-test");
            System.out.println("[SelfDevBootstrapController] [CHECK_PERMISSIONS] Creating testing file: " + testFile.getAbsolutePath());
            try {
                if (testFile.createNewFile()) {
                    boolean deleted = testFile.delete();
                    long duration = System.currentTimeMillis() - startTime;
                    System.out.println("[SelfDevBootstrapController] [CHECK_PERMISSIONS_SUCCESS] Created and deleted perm test file successfully. deleted: " + deleted + " (took " + duration + "ms)");
                    return "CHECKED";
                }
            } catch (IOException e) {
                System.err.println("[SelfDevBootstrapController] [CHECK_PERMISSIONS_ERROR] Filesystem permission check IOException: " + e.getMessage());
            }
        }
        System.err.println("[SelfDevBootstrapController] [CHECK_PERMISSIONS_FAIL] Filesystem permission check failed. RunDir: " + runDir.getAbsolutePath());
        return "ERROR: No write access to " + runDir.getName();
    }

    private String checkExport() {
        long startTime = System.currentTimeMillis();
        System.out.println("[SelfDevBootstrapController] [CHECK_EXPORT] Starting Export generation from builded classes...");

        String buildPath = null;
        if (orchestrator != null && orchestrator.getSupervisorSettings() != null) {
            buildPath = orchestrator.getSupervisorSettings().getExecutablePath();
        }
        if (buildPath == null || buildPath.trim().isEmpty()) {
            String dateStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("ddMMyy"));
            buildPath = new File(new File(System.getProperty("user.home"), "projects/evo/supervisor"), dateStr + "/builds").getPath();
        }
        File buildDir = new File(buildPath);

        String exportPath = null;
        if (buildPath.endsWith("builds") || buildPath.endsWith("builds/") || buildPath.endsWith("builds\\")) {
            exportPath = new File(buildDir.getParentFile(), "export").getPath();
        } else {
            exportPath = buildPath + "/export";
        }
        File exportDir = new File(exportPath);
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }

        // Copy existing Jars from builds directory
        File[] buildJars = buildDir.exists() ? buildDir.listFiles((dir, name) -> name.endsWith(".jar")) : null;
        if (buildJars != null && buildJars.length > 0) {
            for (File jar : buildJars) {
                try {
                    File destJar = new File(exportDir, jar.getName());
                    System.out.println("[SelfDevBootstrapController] [CHECK_EXPORT] Copying build jar to export: " + destJar.getAbsolutePath());
                    Files.copy(jar.toPath(), destJar.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    System.err.println("[SelfDevBootstrapController] [CHECK_EXPORT] Failed to copy build jar: " + e.getMessage());
                }
            }
        }

        // Try finding Jars in target folders of the sources directory and copying them
        String buildWorkspacePath = null;
        if (orchestrator != null && orchestrator.getSupervisorSettings() != null) {
            buildWorkspacePath = orchestrator.getSupervisorSettings().getSourcePath();
        }
        if (buildWorkspacePath == null || buildWorkspacePath.trim().isEmpty()) {
            String dateStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("ddMMyy"));
            buildWorkspacePath = new File(new File(System.getProperty("user.home"), "projects/evo/supervisor"), dateStr + "/sources").getPath();
        }
        File sourcesDir = new File(buildWorkspacePath);
        if (sourcesDir.exists()) {
            findAndCopyJars(sourcesDir, buildDir, exportDir);
        }

        // Check if export directory has the runnable jar
        File[] exportJars = exportDir.listFiles((dir, name) -> name.endsWith("-shaded.jar") || name.endsWith(".jar"));
        if (exportJars != null && exportJars.length > 0) {
            File runnableJar = exportJars[0];
            for (File jar : exportJars) {
                if (jar.getName().contains("-shaded")) {
                    runnableJar = jar;
                    break;
                }
            }
            long duration = System.currentTimeMillis() - startTime;
            System.out.println("[SelfDevBootstrapController] [CHECK_EXPORT_SUCCESS] Generated/Found runnable EVO product in export folder: " + runnableJar.getAbsolutePath() + " (took " + duration + "ms)");
            return "READY: " + runnableJar.getName();
        }

        // If we still don't have any JAR, run a package build to generate it!
        System.out.println("[SelfDevBootstrapController] [CHECK_EXPORT] No jars found. Running build to generate runnable product...");
        String buildRes = runBuildAndCopy();
        if (buildRes != null && buildRes.startsWith("SUCCESS")) {
            exportJars = exportDir.listFiles((dir, name) -> name.endsWith("-shaded.jar") || name.endsWith(".jar"));
            if (exportJars != null && exportJars.length > 0) {
                File runnableJar = exportJars[0];
                for (File jar : exportJars) {
                    if (jar.getName().contains("-shaded")) {
                        runnableJar = jar;
                        break;
                    }
                }
                return "READY: " + runnableJar.getName();
            }
        }

        return "ERROR: Runnable EVO product could not be generated. Please run Build first.";
    }

    private String copyCodebaseToSupervisorSource() {
        long startTime = System.currentTimeMillis();
        System.out.println("[SelfDevBootstrapController] [COPY] Initiating Codebase Copy task...");
        String srcPath = null;
        if (orchestrator != null && orchestrator.getGit() != null) {
            srcPath = orchestrator.getGit().getLocalPath();
        }
        if (srcPath == null || srcPath.trim().isEmpty()) {
            srcPath = eu.kalafatic.evolution.controller.tools.EclipseGitEvoTool.getRepositoryPath(eu.kalafatic.evolution.controller.tools.EclipseGitEvoTool.REPO_EVOLUTION);
        }
        if (srcPath == null || srcPath.trim().isEmpty()) {
            srcPath = eu.kalafatic.evolution.controller.manager.ProjectModelManager.getCodebasePath();
            System.out.println("[SelfDevBootstrapController] [COPY] Falling back to ProjectModelManager.getCodebasePath(): " + srcPath);
        }
        if (srcPath == null || srcPath.trim().isEmpty()) {
            System.err.println("[SelfDevBootstrapController] [COPY_FAIL] Codebase Copy failed: Could not resolve codebase source path.");
            return "ERROR: Could not resolve codebase path";
        }

        String destPath = null;
        if (orchestrator != null && orchestrator.getSupervisorSettings() != null) {
            destPath = orchestrator.getSupervisorSettings().getSourcePath();
        }
        if (destPath == null || destPath.trim().isEmpty()) {
            String dateStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("ddMMyy"));
            destPath = new File(new File(System.getProperty("user.home"), "projects/evo/supervisor"), dateStr + "/sources").getPath();
        }

        File src = new File(srcPath);
        File dest = new File(destPath);
        System.out.println("[SelfDevBootstrapController] [COPY] Source directory: " + src.getAbsolutePath());
        System.out.println("[SelfDevBootstrapController] [COPY] Destination directory: " + dest.getAbsolutePath());

        if (!src.exists()) {
            System.err.println("[SelfDevBootstrapController] [COPY_FAIL] Codebase Copy failed: Source directory does not exist.");
            return "ERROR: Source path does not exist: " + src.getAbsolutePath();
        }

        final int[] filesCopied = {0};

        try {
            if (dest.exists()) {
                System.out.println("[SelfDevBootstrapController] [COPY] Destination folder exists. Deleting recursively: " + dest.getAbsolutePath());
                deleteRecursively(dest);
            }
            dest.mkdirs();

            final java.nio.file.Path sourcePath = src.toPath();
            final java.nio.file.Path targetPath = dest.toPath();

            System.out.println("[SelfDevBootstrapController] [COPY] Walking source directory tree...");
            java.nio.file.Files.walkFileTree(sourcePath, new java.nio.file.SimpleFileVisitor<java.nio.file.Path>() {
                @Override
                public java.nio.file.FileVisitResult preVisitDirectory(java.nio.file.Path dir, java.nio.file.attribute.BasicFileAttributes attrs) throws IOException {
                    if (dir.equals(sourcePath)) {
                        java.nio.file.Path targetDir = targetPath.resolve(sourcePath.relativize(dir));
                        if (!java.nio.file.Files.exists(targetDir)) {
                            java.nio.file.Files.createDirectories(targetDir);
                        }
                        return java.nio.file.FileVisitResult.CONTINUE;
                    }
                    String name = dir.getFileName().toString();
                    if (name.equals(".git") || name.equals("target") || name.equals("self-dev-run") ||
                        name.equals(".settings") || name.equals(".mvn") || name.equals(".metadata") ||
                        name.equals("bin") || name.equals("iterations") || name.equals("orchestrator")) {
                        System.out.println("[SelfDevBootstrapController] [COPY] Skipping excluded directory: " + dir);
                        return java.nio.file.FileVisitResult.SKIP_SUBTREE;
                    }
                    java.nio.file.Path targetDir = targetPath.resolve(sourcePath.relativize(dir));
                    if (!java.nio.file.Files.exists(targetDir)) {
                        java.nio.file.Files.createDirectories(targetDir);
                    }
                    return java.nio.file.FileVisitResult.CONTINUE;
                }

                @Override
                public java.nio.file.FileVisitResult visitFile(java.nio.file.Path file, java.nio.file.attribute.BasicFileAttributes attrs) throws IOException {
                    String name = file.getFileName().toString();
                    if (name.equals(".git") || name.equals("target") || name.equals("self-dev-run") ||
                        name.equals(".settings") || name.equals(".mvn") || name.equals(".metadata") ||
                        name.equals("bin") || name.equals("iterations") || name.equals("orchestrator") ||
                        name.equals("dependency-reduced-pom.xml")) {
                        return java.nio.file.FileVisitResult.CONTINUE;
                    }
                    java.nio.file.Path targetFile = targetPath.resolve(sourcePath.relativize(file));
                    java.nio.file.Files.copy(file, targetFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    filesCopied[0]++;
                    return java.nio.file.FileVisitResult.CONTINUE;
                }
            });
            long duration = System.currentTimeMillis() - startTime;
            System.out.println("[SelfDevBootstrapController] [COPY_SUCCESS] Codebase Copy successful. Total files copied: " + filesCopied[0] + " (took " + duration + "ms)");
            return "SUCCESS: " + filesCopied[0] + " files";
        } catch (IOException e) {
            System.err.println("[SelfDevBootstrapController] [COPY_ERROR] Codebase Copy IOException: " + e.getMessage());
            e.printStackTrace();
            return "ERROR: " + e.getMessage();
        }
    }

    private void deleteRecursively(File file) throws IOException {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    deleteRecursively(f);
                }
            }
        }
        if (!file.delete() && file.exists()) {
            throw new IOException("Failed to delete: " + file.getAbsolutePath());
        }
    }
}
