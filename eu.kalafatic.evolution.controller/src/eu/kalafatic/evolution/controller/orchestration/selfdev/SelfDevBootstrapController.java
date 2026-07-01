package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.io.IOException;
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
import eu.kalafatic.evolution.supervisor.bootstrap.BuildConfiguration;
import eu.kalafatic.evolution.supervisor.bootstrap.BuildResult;
import eu.kalafatic.evolution.supervisor.bootstrap.CodebaseCopyTool;
import eu.kalafatic.evolution.supervisor.bootstrap.CopyConfiguration;
import eu.kalafatic.evolution.supervisor.bootstrap.CopyResult;
import eu.kalafatic.evolution.supervisor.bootstrap.RcpBuildTool;

/**
 * Controller for bootstrapping the self-development flow.
 * Triggered by the RCP application to start an external Supervisor.
 *
 * @evo:22:A reason=self-dev-bootstrap-flow
 */
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

    /**
     * Writes bootstrap.json and triggers the external Supervisor.
     */
    public void startBootstrap() throws IOException {
        if (supervisorProcess != null && supervisorProcess.isAlive()) {
            return;
        }

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

        // Generate initial context.json
        try {
            Task task = OrchestrationFactory.eINSTANCE.createTask();
            task.setGoal("self-development");
            task.setName("Autonomous improvement");

            TaskContext taskContext = new TaskContext(orchestrator, projectRoot);
            ContextPackage pkg = ContextBuilder.build(task, taskContext);

            ObjectMapper mapper = new ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(contextFile, pkg);
        } catch (Exception e) {
            System.err.println("Failed to generate context.json: " + e.getMessage());
        }

        JSONObject bootstrap = new JSONObject();
        bootstrap.put("sourcePath", projectRoot.getAbsolutePath());
        bootstrap.put("targetPath", new File(runDir, "workspace").getAbsolutePath());
        bootstrap.put("action", "BUILD_AND_START");
        bootstrap.put("statePath", stateFile.getAbsolutePath());

        File bootstrapFile = new File(runDir, "bootstrap.json");
        Files.write(bootstrapFile.toPath(), bootstrap.toString(4).getBytes());

        // Signal loop active (Legacy)
        JSONObject legacyState = new JSONObject();
        legacyState.put("active", true);
        legacyState.put("iteration", 1);
        Files.write(new File(projectRoot, "self-dev.json").toPath(), legacyState.toString(4).getBytes());

        // Trigger Supervisor
        triggerSupervisor();
    }

    public void stopBootstrap() {
        if (supervisorProcess != null) {
            supervisorProcess.destroy();
            supervisorProcess = null;
        }

        // Signal loop inactive
        try {
            JSONObject state = new JSONObject();
            state.put("active", false);
            state.put("iteration", 0);
            Files.write(new File(projectRoot, "self-dev.json").toPath(), state.toString(4).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
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
                supervisorProcess = pb.start();
            } catch (IOException e) {
                System.err.println("Failed to trigger Supervisor: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Reads the current status from status.json.
     */
    public JSONObject getStatus() {
        if (supervisorProcess != null && !supervisorProcess.isAlive()) {
            return new JSONObject().put("phase", "STOPPED");
        }

        File statusFile = new File(runDir, "status.json");
        if (statusFile.exists()) {
            try {
                String content = new String(Files.readAllBytes(statusFile.toPath()));
                if (content.trim().isEmpty()) return null;
                return new JSONObject(content);
            } catch (Exception e) {
                // Return null if file is being written or invalid
            }
        }
        return null;
    }

    public boolean isRunning() {
        return supervisorProcess != null && supervisorProcess.isAlive();
    }

    /**
     * Performs a specific preflight check or action.
     */
    public String check(String type) {
        return switch (type.toUpperCase()) {
            case "GIT" -> checkGit();
            case "MAVEN" -> checkMaven();
            case "LLM" -> checkLlm();
            case "GENOME" -> checkGenome();
            case "PERMISSIONS" -> checkPermissions();
            case "COPY" -> checkCopy();
            case "BUILD" -> checkBuild();
            case "EXPORT" -> checkExport();
            default -> "UNKNOWN";
        };
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
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    private String checkMaven() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            String mvnCmd = os.contains("win") ? "mvn.cmd" : "mvn";
            ProcessBuilder pb = new ProcessBuilder(mvnCmd, "-version");
            pb.directory(projectRoot);
            Process p = pb.start();
            if (p.waitFor() == 0) return "CHECKED";
            return "ERROR: Maven failed";
        } catch (Exception e) {
            return "ERROR: Maven not found";
        }
    }

    private String checkLlm() {
        try {
            if (orchestrator != null && !orchestrator.getAiProviders().isEmpty()) {
                return "CHECKED";
            }
            // Fallback: check Ollama local
            URL url = new URL("http://localhost:11434/api/tags");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(2000);
            if (con.getResponseCode() == 200) return "CHECKED";
            return "ERROR: LLM unreachable";
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    private String checkGenome() {
        File genomeDir = new File(projectRoot, "eu.kalafatic.evolution.selfdev.genome");
        if (genomeDir.exists() && new File(genomeDir, "pom.xml").exists()) {
            return "CHECKED";
        }
        return "ERROR: Genome module missing";
    }

    private String checkPermissions() {
        if (runDir.exists() && runDir.canWrite()) {
            File testFile = new File(runDir, ".perm-test");
            try {
                if (testFile.createNewFile()) {
                    testFile.delete();
                    return "CHECKED";
                }
            } catch (IOException e) {}
        }
        return "ERROR: No write access to " + runDir.getName();
    }

    private String checkCopy() {
        File sandbox = new File(runDir, "workspace");
        CodebaseCopyTool tool = new CodebaseCopyTool();
        CopyConfiguration config = new CopyConfiguration(projectRoot, sandbox);
        config.setOverwrite(true);
        config.addExclusion(".git");
        config.addExclusion("target");
        config.addExclusion("self-dev-run");
        config.addExclusion(".settings");
        config.addExclusion(".mvn");
        config.addExclusion(".metadata");
        config.addExclusion("bin");
        config.addExclusion("iterations");
        config.addExclusion("orchestrator");

        CopyResult result = tool.copy(config);
        if (result.isSuccess()) {
            return "CHECKED (" + result.getFilesCopied() + " files, " + (result.getTotalBytes() / 1024 / 1024) + " MB)";
        } else {
            return "ERROR: " + result.getMessage();
        }
    }

    private String checkBuild() {
        File sandbox = new File(runDir, "workspace");
        if (!sandbox.exists()) return "ERROR: Sandbox missing. Run Copy first.";

        RcpBuildTool tool = new RcpBuildTool();
        BuildConfiguration config = new BuildConfiguration(sandbox);
        config.setSkipTests(true);
        config.addGoal("clean");
        config.addGoal("package");

        BuildResult result = tool.build(config);
        if (result.isSuccess()) {
            return "SUCCESS (" + result.getExecutionTimeMs() + "ms)";
        } else {
            return "ERROR: Build failed. Exit code: " + result.getExitCode();
        }
    }

    private String checkExport() {
        File sandbox = new File(runDir, "workspace");
        RcpBuildTool tool = new RcpBuildTool();
        BuildConfiguration config = new BuildConfiguration(sandbox);

        // We reuse the findArtifact logic from RcpBuildTool by calling a build with no goals if needed,
        // but it's better to just use the discovery logic.
        // For now, let's just check if the artifact exists using the tool's result pattern.
        BuildResult dummyResult = new BuildResult(true, "", 0, "", "", 0);
        File artifact = tool.build(config).getProducedArtifact(); // RcpBuildTool.build() finds it.

        if (artifact != null && artifact.exists()) {
            return "READY: " + artifact.getName();
        }
        return "ERROR: Artifact not found. Run Build first.";
    }
}
