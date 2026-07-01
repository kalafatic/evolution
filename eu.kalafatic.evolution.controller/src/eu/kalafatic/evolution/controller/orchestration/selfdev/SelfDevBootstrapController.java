package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;

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
    private volatile Process supervisorProcess;

    public SelfDevBootstrapController(File projectRoot, Orchestrator orchestrator) {
        this.projectRoot = projectRoot;
        this.orchestrator = orchestrator;
        this.runDir = new File(projectRoot, "self-dev-run");
        if (!runDir.exists()) {
            runDir.mkdirs();
        }
    }

    public void startBootstrap() throws IOException {
        if (supervisorProcess != null && supervisorProcess.isAlive()) return;
        File stateFile = new File(runDir, "state.json");
        File contextFile = new File(runDir, "context.json");
        JSONObject state = new JSONObject();
        state.put("active", true);
        state.put("iteration", 0);
        state.put("goal", "self-development");
        state.put("mode", "DARWIN");
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
        triggerSupervisor();
    }

    public void stopBootstrap() {
        if (supervisorProcess != null) {
            supervisorProcess.destroy();
            supervisorProcess = null;
        }
    }

    private String getSupervisorJarPath() {
        File supervisorDir = new File(projectRoot, "eu.kalafatic.evolution.supervisor/target");
        File[] jars = supervisorDir.listFiles((dir, name) -> name.endsWith("-shaded.jar"));
        if (jars != null && jars.length > 0) return jars[0].getAbsolutePath();
        return new File(supervisorDir, "eu.kalafatic.evolution.supervisor-1.0.0-SNAPSHOT.jar").getAbsolutePath();
    }

    private void triggerSupervisor() {
        new Thread(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder("java", "-jar", getSupervisorJarPath(), projectRoot.getAbsolutePath());
                pb.directory(projectRoot);
                pb.inheritIO();
                supervisorProcess = pb.start();
            } catch (IOException e) {
                System.err.println("Failed to trigger Supervisor: " + e.getMessage());
            }
        }).start();
    }

    public JSONObject getStatus() {
        if (supervisorProcess != null && !supervisorProcess.isAlive()) return new JSONObject().put("phase", "STOPPED");
        File statusFile = new File(runDir, "status.json");
        if (statusFile.exists()) {
            try {
                String content = new String(Files.readAllBytes(statusFile.toPath()));
                if (!content.trim().isEmpty()) return new JSONObject(content);
            } catch (Exception e) {}
        }
        return null;
    }

    public boolean isRunning() { return supervisorProcess != null && supervisorProcess.isAlive(); }

    public String check(String type) {
        return switch (type.toUpperCase()) {
            case "GIT" -> checkGit();
            case "MAVEN" -> checkMaven();
            case "LLM" -> checkLlm();
            case "GENOME" -> checkGenome();
            case "PERMISSIONS" -> checkPermissions();
            case "COPY" -> launchTool("--copy", projectRoot.getAbsolutePath(), new File(runDir, "workspace").getAbsolutePath());
            case "BUILD" -> launchTool("--build", new File(runDir, "workspace").getAbsolutePath());
            case "EXPORT" -> checkExport();
            default -> "UNKNOWN";
        };
    }

    private String launchTool(String... args) {
        try {
            java.util.List<String> command = new java.util.ArrayList<>();
            command.add("java");
            command.add("-jar");
            command.add(getSupervisorJarPath());
            for (String arg : args) command.add(arg);
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(projectRoot);
            Process p = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                String lastLine = "ERROR: No output";
                while ((line = reader.readLine()) != null) lastLine = line;
                p.waitFor();
                return lastLine;
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
        File genomeDir = new File(projectRoot, "eu.kalafatic.evolution.selfdev.genome");
        if (genomeDir.exists() && new File(genomeDir, "pom.xml").exists()) return "CHECKED";
        return "ERROR: Genome module missing";
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
}
