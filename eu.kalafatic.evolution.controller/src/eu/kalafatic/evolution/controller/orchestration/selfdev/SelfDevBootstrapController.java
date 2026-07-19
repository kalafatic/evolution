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
        try {
            URL url = new URL("http://localhost:8089/ping");
            System.out.println("[SelfDevBootstrapController] Pinging supervisor at: " + url);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(500);
            int code = conn.getResponseCode();
            System.out.println("[SelfDevBootstrapController] Ping response code: " + code);
            return code == 200;
        } catch (Exception e) {
            System.out.println("[SelfDevBootstrapController] Supervisor ping failed (not responding/unreachable). Details: " + e.getMessage());
            return false;
        }
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

    private String getSupervisorJarPath() {
        File supervisorDir = new File(projectRoot, "eu.kalafatic.evolution.supervisor/target");
        System.out.println("[SelfDevBootstrapController] Scanning for supervisor shaded JAR in: " + supervisorDir.getAbsolutePath());
        File[] jars = supervisorDir.listFiles((dir, name) -> name.endsWith("-shaded.jar"));
        if (jars != null && jars.length > 0) {
            String path = jars[0].getAbsolutePath();
            System.out.println("[SelfDevBootstrapController] Found shaded supervisor jar: " + path);
            return path;
        }
        File fallbackJar = new File(supervisorDir, "eu.kalafatic.evolution.supervisor-1.0.0-SNAPSHOT.jar");
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
            supervisorProcess.destroy();
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
        System.out.println("[SelfDevBootstrapController] [CHECK_START] Executing check for type: " + type);
        System.out.println("[SelfDevBootstrapController] [CHECK_START] Current variables: projectRoot=" + (projectRoot != null ? projectRoot.getAbsolutePath() : "null") + ", runDir=" + (runDir != null ? runDir.getAbsolutePath() : "null") + ", orchestrator=" + (orchestrator != null ? orchestrator.toString() : "null"));
        ensureSupervisorRunning();
        String result = switch (type.toUpperCase()) {
            case "GIT" -> checkGit();
            case "MAVEN" -> checkMaven();
            case "LLM" -> checkLlm();
            case "GENOME" -> checkGenome();
            case "PERMISSIONS" -> checkPermissions();
            case "COPY" -> copyCodebaseToSupervisorSource();
            case "BUILD" -> {
                String buildWorkspacePath = new File(runDir, "workspace").getAbsolutePath();
                System.out.println("[SelfDevBootstrapController] [CHECK_BUILD] Dispatching BUILD call to supervisor. path=" + buildWorkspacePath);
                yield callSupervisor("/build?path=" + encode(buildWorkspacePath));
            }
            case "EXPORT" -> checkExport();
            default -> {
                System.err.println("[SelfDevBootstrapController] [CHECK_UNKNOWN] Unknown check type requested: " + type);
                yield "UNKNOWN";
            }
        };
        System.out.println("[SelfDevBootstrapController] [CHECK_END] Check for type " + type + " completed. Returned: " + result);
        return result;
    }

    private String encode(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }

    private String callSupervisor(String endpoint) {
        System.out.println("[SelfDevBootstrapController] Calling supervisor endpoint: " + endpoint);
        try {
            URL url = new URL("http://localhost:8089" + endpoint);
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
            System.err.println("[SelfDevBootstrapController] Failed to call supervisor on " + endpoint + ": " + e.getMessage());
            return "ERROR: " + e.getMessage();
        }
    }

    private String checkGit() {
        System.out.println("[SelfDevBootstrapController] [CHECK_GIT] Starting Git configuration check...");
        try {
            File gitDir = new File(projectRoot, ".git");
            System.out.println("[SelfDevBootstrapController] [CHECK_GIT] Verifying Git directory at: " + gitDir.getAbsolutePath());
            if (!gitDir.exists()) {
                System.err.println("[SelfDevBootstrapController] [CHECK_GIT_FAIL] Git folder missing. Not a Git repository.");
                return "ERROR: Not a Git repository";
            }
            System.out.println("[SelfDevBootstrapController] [CHECK_GIT] Executing 'git status --porcelain' in directory: " + projectRoot.getAbsolutePath());
            ProcessBuilder pb = new ProcessBuilder("git", "status", "--porcelain");
            pb.directory(projectRoot);
            Process p = pb.start();
            p.getInputStream().readAllBytes();
            int exitCode = p.waitFor();
            System.out.println("[SelfDevBootstrapController] [CHECK_GIT] 'git status --porcelain' exited with code: " + exitCode);
            if (exitCode == 0) {
                return "CHECKED";
            } else {
                return "ERROR: git command failed with exit code " + exitCode;
            }
        } catch (Exception e) {
            System.err.println("[SelfDevBootstrapController] [CHECK_GIT_ERROR] Exception occurred: " + e.getMessage());
            e.printStackTrace();
            return "ERROR: " + e.getMessage();
        }
    }

    private String checkMaven() {
        System.out.println("[SelfDevBootstrapController] [CHECK_MAVEN] Starting Maven check...");
        try {
            String mvnCmd = System.getProperty("os.name").toLowerCase().contains("win") ? "mvn.cmd" : "mvn";
            System.out.println("[SelfDevBootstrapController] [CHECK_MAVEN] OS: " + System.getProperty("os.name") + ", Maven Executable: " + mvnCmd);
            System.out.println("[SelfDevBootstrapController] [CHECK_MAVEN] Executing '" + mvnCmd + " -version' in directory: " + projectRoot.getAbsolutePath());
            ProcessBuilder pb = new ProcessBuilder(mvnCmd, "-version");
            pb.directory(projectRoot);
            Process p = pb.start();
            int exitCode = p.waitFor();
            System.out.println("[SelfDevBootstrapController] [CHECK_MAVEN] '" + mvnCmd + " -version' exited with code: " + exitCode);
            if (exitCode == 0) return "CHECKED";
            return "ERROR: Maven command failed with exit code " + exitCode;
        } catch (Exception e) {
            System.err.println("[SelfDevBootstrapController] [CHECK_MAVEN_ERROR] Exception: " + e.getMessage());
            e.printStackTrace();
            return "ERROR: Maven not found or executable issues: " + e.getMessage();
        }
    }

    private String checkLlm() {
        System.out.println("[SelfDevBootstrapController] [CHECK_LLM] Starting LLM connectivity check...");
        try {
            if (orchestrator != null && !orchestrator.getAiProviders().isEmpty()) {
                System.out.println("[SelfDevBootstrapController] [CHECK_LLM] Active AI providers in orchestrator: " + orchestrator.getAiProviders().size() + ". LLM check passed.");
                return "CHECKED";
            }
            URL url = new URL("http://localhost:11434/api/tags");
            System.out.println("[SelfDevBootstrapController] [CHECK_LLM] Pinging Ollama URL: " + url.toString());
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(2000);
            int code = con.getResponseCode();
            System.out.println("[SelfDevBootstrapController] [CHECK_LLM] Ollama response code: " + code);
            if (code == 200) return "CHECKED";
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

            ProcessBuilder pb = new ProcessBuilder(mvnCmd, "clean", "compile", "-pl", "eu.kalafatic.evolution.selfdev.genome", "-am", "-DskipTests");
            pb.directory(parentDir);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[Genome Build] " + line);
                }
            }
            int exitCode = p.waitFor();
            System.out.println("[SelfDevBootstrapController] Genome module build finished with exit code: " + exitCode);
            if (exitCode == 0) {
                return "SUCCESS";
            } else {
                return "ERROR: Build failed (exit code " + exitCode + ")";
            }
        } catch (Exception e) {
            System.err.println("[SelfDevBootstrapController] Failed to compile genome module: " + e.getMessage());
            e.printStackTrace();
            return "ERROR: " + e.getMessage();
        }
    }

    private String checkGenome() {
        System.out.println("[SelfDevBootstrapController] [CHECK_GENOME] Starting GENOME Check. projectRoot: " + (projectRoot != null ? projectRoot.getAbsolutePath() : "null"));
        File dir = projectRoot;
        File genomeModuleDir = null;

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
            eu.kalafatic.evolution.selfdev.genome.hub.SelfDevGenomeHub.getInstance()
                .updateGenome(projectRoot, projectRoot.getName(), "v1.0.0");

            File genomeJson = new File(projectRoot, "genome/current/genome.json");
            System.out.println("[SelfDevBootstrapController] [CHECK_GENOME] Checking generated genome.json path: " + genomeJson.getAbsolutePath() + ", exists: " + genomeJson.exists());
            if (genomeJson.exists()) {
                System.out.println("[SelfDevBootstrapController] [CHECK_GENOME_SUCCESS] GENOME Check successful. Generated genome.json: " + genomeJson.getAbsolutePath());
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
        System.out.println("[SelfDevBootstrapController] [CHECK_PERMISSIONS] Starting filesystem permissions check...");
        System.out.println("[SelfDevBootstrapController] [CHECK_PERMISSIONS] runDir: " + runDir.getAbsolutePath() + ", exists: " + runDir.exists() + ", canWrite: " + runDir.canWrite());
        if (runDir.exists() && runDir.canWrite()) {
            File testFile = new File(runDir, ".perm-test");
            System.out.println("[SelfDevBootstrapController] [CHECK_PERMISSIONS] Creating testing file: " + testFile.getAbsolutePath());
            try {
                if (testFile.createNewFile()) {
                    boolean deleted = testFile.delete();
                    System.out.println("[SelfDevBootstrapController] [CHECK_PERMISSIONS] Created and deleted perm test file successfully. deleted: " + deleted);
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
        System.out.println("[SelfDevBootstrapController] [CHECK_EXPORT] Checking exported supervisor artifact...");
        File sandbox = new File(runDir, "workspace");
        File supervisorTarget = new File(sandbox, "eu.kalafatic.evolution.supervisor/target");
        System.out.println("[SelfDevBootstrapController] [CHECK_EXPORT] Scanning sandbox target directory: " + supervisorTarget.getAbsolutePath() + ", exists: " + supervisorTarget.exists());
        if (supervisorTarget.exists()) {
            File[] jars = supervisorTarget.listFiles((dir, name) -> name.endsWith("-shaded.jar"));
            if (jars != null && jars.length > 0) {
                System.out.println("[SelfDevBootstrapController] [CHECK_EXPORT_SUCCESS] Found exported shaded supervisor jar: " + jars[0].getAbsolutePath() + " (size: " + jars[0].length() + " bytes)");
                return "READY: " + jars[0].getName();
            }
        }
        System.err.println("[SelfDevBootstrapController] [CHECK_EXPORT_FAIL] Exported supervisor artifact not found in sandbox target: " + supervisorTarget.getAbsolutePath());
        return "ERROR: Artifact not found. Run Build first.";
    }

    private String copyCodebaseToSupervisorSource() {
        System.out.println("[SelfDevBootstrapController] Initiating Codebase Copy task...");
        String srcPath = null;
        try {
            System.out.println("[SelfDevBootstrapController] Reflectively querying ProjectManager.getCodebasePath()...");
            Class<?> pmClass = Class.forName("eu.kalafatic.evolution.view.provider.ProjectManager");
            java.lang.reflect.Method m = pmClass.getMethod("getCodebasePath");
            srcPath = (String) m.invoke(null);
            System.out.println("[SelfDevBootstrapController] ProjectManager.getCodebasePath() returned: " + srcPath);
        } catch (Throwable t) {
            System.out.println("[SelfDevBootstrapController] ProjectManager.getCodebasePath() fallback via reflection ignored: " + t.getMessage());
        }
        if (srcPath == null) {
            srcPath = eu.kalafatic.evolution.controller.manager.ProjectModelManager.getCodebasePath();
            System.out.println("[SelfDevBootstrapController] Falling back to ProjectModelManager.getCodebasePath(): " + srcPath);
        }
        if (srcPath == null) {
            System.err.println("[SelfDevBootstrapController] Codebase Copy failed: Could not resolve codebase source path.");
            return "ERROR: Could not resolve codebase path";
        }

        String destPath = null;
        if (orchestrator != null && orchestrator.getSupervisorSettings() != null) {
            destPath = orchestrator.getSupervisorSettings().getSourcePath();
        }
        if (destPath == null || destPath.trim().isEmpty()) {
            destPath = new File(System.getProperty("user.home"), "supervisor/source").getPath();
        }

        File src = new File(srcPath);
        File dest = new File(destPath);
        System.out.println("[SelfDevBootstrapController] Source directory: " + src.getAbsolutePath());
        System.out.println("[SelfDevBootstrapController] Destination directory: " + dest.getAbsolutePath());

        if (!src.exists()) {
            System.err.println("[SelfDevBootstrapController] Codebase Copy failed: Source directory does not exist.");
            return "ERROR: Source path does not exist: " + src.getAbsolutePath();
        }

        final int[] filesCopied = {0};

        try {
            if (dest.exists()) {
                System.out.println("[SelfDevBootstrapController] Destination folder exists. Deleting recursively: " + dest.getAbsolutePath());
                deleteRecursively(dest);
            }
            dest.mkdirs();

            final java.nio.file.Path sourcePath = src.toPath();
            final java.nio.file.Path targetPath = dest.toPath();

            System.out.println("[SelfDevBootstrapController] Walking source directory tree...");
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
                        name.equals("bin") || name.equals("iterations") || name.equals("orchestrator")) {
                        return java.nio.file.FileVisitResult.CONTINUE;
                    }
                    java.nio.file.Path targetFile = targetPath.resolve(sourcePath.relativize(file));
                    java.nio.file.Files.copy(file, targetFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    filesCopied[0]++;
                    return java.nio.file.FileVisitResult.CONTINUE;
                }
            });
            System.out.println("[SelfDevBootstrapController] Codebase Copy successful. Total files copied: " + filesCopied[0]);
            return "SUCCESS: " + filesCopied[0] + " files";
        } catch (IOException e) {
            System.err.println("[SelfDevBootstrapController] Codebase Copy IOException: " + e.getMessage());
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
