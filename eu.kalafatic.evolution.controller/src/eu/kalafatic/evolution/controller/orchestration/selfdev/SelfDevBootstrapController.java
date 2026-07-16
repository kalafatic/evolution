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
        this.debugMode = debugMode;
    }

    public boolean isDebugMode() {
        return this.debugMode;
    }

    public SelfDevBootstrapController(File projectRoot, Orchestrator orchestrator) {
        this.projectRoot = projectRoot;
        this.orchestrator = orchestrator;
        this.runDir = new File(projectRoot, "self-dev-run");
        if (!runDir.exists()) {
            runDir.mkdirs();
        }
    }

    private void ensureSupervisorRunning() {
        if (isSupervisorAlive()) return;
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add("java");
            if (debugMode) {
                cmd.add("-Devo.mode=debug");
                cmd.add("-Ddebug=true");
            }
            cmd.add("-jar");
            cmd.add(getSupervisorJarPath());
            cmd.add(projectRoot.getAbsolutePath());
            if (debugMode) {
                cmd.add("--debug");
            }
            ProcessBuilder pb = new ProcessBuilder(cmd);
            if (debugMode) {
                pb.environment().put("EVO_DEBUG", "true");
            }
            pb.directory(projectRoot);
            pb.redirectErrorStream(true);
            supervisorProcess = pb.start();
            
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(supervisorProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) System.out.println("[Supervisor] " + line);
                } catch (IOException e) {}
            }).start();

            waitUntilReady();
        } catch (IOException e) {
            System.err.println("Failed to start Supervisor: " + e.getMessage());
        }
    }

    private boolean isSupervisorAlive() {
        try {
            URL url = new URL("http://localhost:8089/ping");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(500);
            return conn.getResponseCode() == 200;
        } catch (Exception e) { return false; }
    }

    private void waitUntilReady() {
        for (int i = 0; i < 10; i++) {
            if (isSupervisorAlive()) return;
            try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

    private String getSupervisorJarPath() {
        File supervisorDir = new File(projectRoot, "eu.kalafatic.evolution.supervisor/target");
        File[] jars = supervisorDir.listFiles((dir, name) -> name.endsWith("-shaded.jar"));
        if (jars != null && jars.length > 0) return jars[0].getAbsolutePath();
        return new File(supervisorDir, "eu.kalafatic.evolution.supervisor-1.0.0-SNAPSHOT.jar").getAbsolutePath();
    }

    public void startBootstrap() throws IOException {
        ensureSupervisorRunning();
        File stateFile = new File(runDir, "state.json");
        File contextFile = new File(runDir, "context.json");
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
        Files.write(stateFile.toPath(), state.toString(4).getBytes());
        try {
            Task task = OrchestrationFactory.eINSTANCE.createTask();
            task.setGoal("self-development");
            task.setName("Autonomous improvement");
            TaskContext taskContext = new TaskContext(orchestrator, projectRoot);
            ContextPackage pkg = ContextBuilder.build(task, taskContext);
            ObjectMapper mapper = new ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(contextFile, pkg);
        } catch (Exception e) {}
        JSONObject bootstrap = new JSONObject();
        bootstrap.put("sourcePath", projectRoot.getAbsolutePath());
        bootstrap.put("targetPath", new File(runDir, "workspace").getAbsolutePath());
        bootstrap.put("action", "BUILD_AND_START");
        bootstrap.put("statePath", stateFile.getAbsolutePath());
        Files.write(new File(runDir, "bootstrap.json").toPath(), bootstrap.toString(4).getBytes());
    }

    public void stopBootstrap() {
        if (supervisorProcess != null) {
            supervisorProcess.destroy();
            supervisorProcess = null;
        }
    }

    public JSONObject getStatus() {
        File statusFile = new File(runDir, "status.json");
        if (statusFile.exists()) {
            try {
                String content = new String(Files.readAllBytes(statusFile.toPath()));
                if (!content.trim().isEmpty()) return new JSONObject(content);
            } catch (Exception e) {}
        }
        return isSupervisorAlive() ? new JSONObject().put("phase", "RUNNING") : new JSONObject().put("phase", "STOPPED");
    }

    public boolean isRunning() { return isSupervisorAlive(); }

    public String check(String type) {
        ensureSupervisorRunning();
        return switch (type.toUpperCase()) {
            case "GIT" -> checkGit();
            case "MAVEN" -> checkMaven();
            case "LLM" -> checkLlm();
            case "GENOME" -> checkGenome();
            case "PERMISSIONS" -> checkPermissions();
            case "COPY" -> copyCodebaseToSupervisorSource();
            case "BUILD" -> callSupervisor("/build?path=" + encode(new File(runDir, "workspace").getAbsolutePath()));
            case "EXPORT" -> checkExport();
            default -> "UNKNOWN";
        };
    }

    private String encode(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }

    private String callSupervisor(String endpoint) {
        try {
            URL url = new URL("http://localhost:8089" + endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                StringBuilder res = new StringBuilder();
                while ((line = reader.readLine()) != null) res.append(line);
                return res.toString();
            }
        } catch (Exception e) { return "ERROR: " + e.getMessage(); }
    }

    private String checkGit() {
        try {
            File gitDir = new File(projectRoot, ".git");
            if (!gitDir.exists()) return "ERROR: Not a Git repository";
            ProcessBuilder pb = new ProcessBuilder("git", "status", "--porcelain");
            pb.directory(projectRoot);
            Process p = pb.start();
            p.getInputStream().readAllBytes();
            return "CHECKED";
        } catch (Exception e) { return "ERROR: " + e.getMessage(); }
    }

    private String checkMaven() {
        try {
            String mvnCmd = System.getProperty("os.name").toLowerCase().contains("win") ? "mvn.cmd" : "mvn";
            ProcessBuilder pb = new ProcessBuilder(mvnCmd, "-version");
            pb.directory(projectRoot);
            Process p = pb.start();
            if (p.waitFor() == 0) return "CHECKED";
            return "ERROR: Maven failed";
        } catch (Exception e) { return "ERROR: Maven not found"; }
    }

    private String checkLlm() {
        try {
            if (orchestrator != null && !orchestrator.getAiProviders().isEmpty()) return "CHECKED";
            URL url = new URL("http://localhost:11434/api/tags");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(2000);
            if (con.getResponseCode() == 200) return "CHECKED";
            return "ERROR: LLM unreachable";
        } catch (Exception e) { return "ERROR: " + e.getMessage(); }
    }

    private String checkGenome() {
        File dir = projectRoot;
        File genomeModuleDir = null;
        while (dir != null) {
            File testDir = new File(dir, "eu.kalafatic.evolution.selfdev.genome");
            if (testDir.exists() && new File(testDir, "pom.xml").exists()) {
                genomeModuleDir = testDir;
                break;
            }
            dir = dir.getParentFile();
        }

        if (genomeModuleDir == null) {
            String codebasePath = eu.kalafatic.evolution.controller.manager.ProjectModelManager.getCodebasePath();
            if (codebasePath != null) {
                File cbDir = new File(codebasePath);
                File testDir = new File(cbDir, "eu.kalafatic.evolution.selfdev.genome");
                if (testDir.exists() && new File(testDir, "pom.xml").exists()) {
                    genomeModuleDir = testDir;
                }
            }
        }

        if (genomeModuleDir == null) {
            return "ERROR: Genome module missing";
        }

        try {
            System.out.println("[GENOME_INTEGRATION] Found genome module. Integrating and updating project genome in: " + projectRoot.getAbsolutePath());
            eu.kalafatic.evolution.selfdev.genome.hub.SelfDevGenomeHub.getInstance()
                .updateGenome(projectRoot, projectRoot.getName(), "v1.0.0");

            File genomeJson = new File(projectRoot, "genome/current/genome.json");
            if (genomeJson.exists()) {
                return "CHECKED (Updated)";
            } else {
                return "ERROR: Failed to generate genome.json in project root";
            }
        } catch (Exception e) {
            return "ERROR: Genome update failed: " + e.getMessage();
        }
    }

    private String checkPermissions() {
        if (runDir.exists() && runDir.canWrite()) {
            File testFile = new File(runDir, ".perm-test");
            try { if (testFile.createNewFile()) { testFile.delete(); return "CHECKED"; } } catch (IOException e) {}
        }
        return "ERROR: No write access to " + runDir.getName();
    }

    private String checkExport() {
        File sandbox = new File(runDir, "workspace");
        File supervisorTarget = new File(sandbox, "eu.kalafatic.evolution.supervisor/target");
        if (supervisorTarget.exists()) {
            File[] jars = supervisorTarget.listFiles((dir, name) -> name.endsWith("-shaded.jar"));
            if (jars != null && jars.length > 0) return "READY: " + jars[0].getName();
        }
        return "ERROR: Artifact not found. Run Build first.";
    }

    private String copyCodebaseToSupervisorSource() {
        String srcPath = null;
        try {
            Class<?> pmClass = Class.forName("eu.kalafatic.evolution.view.provider.ProjectManager");
            java.lang.reflect.Method m = pmClass.getMethod("getCodebasePath");
            srcPath = (String) m.invoke(null);
        } catch (Throwable t) {
            // fallback
        }
        if (srcPath == null) {
            srcPath = eu.kalafatic.evolution.controller.manager.ProjectModelManager.getCodebasePath();
        }
        if (srcPath == null) {
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

        if (!src.exists()) {
            return "ERROR: Source path does not exist: " + src.getAbsolutePath();
        }

        final int[] filesCopied = {0};

        try {
            if (dest.exists()) {
                deleteRecursively(dest);
            }
            dest.mkdirs();

            final java.nio.file.Path sourcePath = src.toPath();
            final java.nio.file.Path targetPath = dest.toPath();

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
            return "SUCCESS: " + filesCopied[0] + " files";
        } catch (IOException e) {
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
