package eu.kalafatic.evolution.supervisor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import fi.iki.elonen.NanoHTTPD;
import eu.kalafatic.evolution.supervisor.bootstrap.CodebaseCopyTool;
import eu.kalafatic.evolution.supervisor.bootstrap.CopyConfiguration;
import eu.kalafatic.evolution.supervisor.bootstrap.CopyResult;
import eu.kalafatic.evolution.supervisor.bootstrap.RcpBuildTool;
import eu.kalafatic.evolution.supervisor.bootstrap.BuildConfiguration;
import eu.kalafatic.evolution.supervisor.bootstrap.BuildResult;

public class SupervisorMain extends NanoHTTPD {
    private static File baseDir;
    private static volatile Process activeEvoProcess;

    public SupervisorMain(int port) {
        super(port);
    }

    public static void main(String[] args) {
        if (args.length > 0 && args[0].startsWith("--")) {
            handleCliCommand(args);
            return;
        }

        System.out.println("=== EVO AI SUPERVISOR STARTING ===");
        String path = (args.length > 0) ? args[0] : ".";
        baseDir = new File(path);
        System.out.println("[CONFIG] Base Directory: " + baseDir.getAbsolutePath());

        SupervisorMain server = new SupervisorMain(8089);
        try {
            System.out.println("[HTTP] Attempting to start NanoHTTPD on port 8089...");
            server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            System.out.println("[HTTP] Server started on port 8089");
        } catch (Throwable t) {
            System.err.println("[HTTP] Failed to start server: " + t.getMessage());
            t.printStackTrace();
        }

        SelfDevSupervisor supervisor = new SelfDevSupervisor(baseDir);
        supervisor.run();
        System.out.println("=== EVO AI SUPERVISOR FINISHED ===");
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        if ("/ping".equals(uri)) return newFixedLengthResponse("READY");

        if ("/git-check".equals(uri)) {
            String workspace = session.getParms().get("path");
            if (workspace == null || workspace.trim().isEmpty()) {
                workspace = baseDir != null ? baseDir.getAbsolutePath() : ".";
            }
            File localDir = new File(workspace);
            try {
                ProcessBuilder pb = new ProcessBuilder("git", "status", "--porcelain");
                pb.directory(localDir);
                pb.redirectErrorStream(true);
                Process p = pb.start();
                StringBuilder output = new StringBuilder();
                try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                }
                int exitCode = p.waitFor();
                if (exitCode == 0) {
                    return newFixedLengthResponse("CHECKED (Supervisor) - Pending changes: " + output.toString().split("\n").length);
                } else {
                    return newFixedLengthResponse("ERROR: git command failed with exit code " + exitCode);
                }
            } catch (Exception e) {
                return newFixedLengthResponse("ERROR: " + e.getMessage());
            }
        }

        if ("/maven-check".equals(uri)) {
            try {
                String mvnCmd = System.getProperty("os.name").toLowerCase().contains("win") ? "mvn.cmd" : "mvn";
                ProcessBuilder pb = new ProcessBuilder(mvnCmd, "-version");
                pb.redirectErrorStream(true);
                Process p = pb.start();
                StringBuilder output = new StringBuilder();
                try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                }
                int exitCode = p.waitFor();
                if (exitCode == 0) {
                    return newFixedLengthResponse("CHECKED (Supervisor)");
                } else {
                    return newFixedLengthResponse("ERROR: mvn -version exited with code " + exitCode);
                }
            } catch (Exception e) {
                return newFixedLengthResponse("ERROR: " + e.getMessage());
            }
        }
        
        if ("/copy".equals(uri)) {
            String src = session.getParms().get("src");
            String dest = session.getParms().get("dest");
            if (src == null || dest == null) return newFixedLengthResponse(Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT, "Missing parameters");
            
            CodebaseCopyTool tool = new CodebaseCopyTool();
            CopyConfiguration config = new CopyConfiguration(new File(src), new File(dest));
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
            return newFixedLengthResponse(result.isSuccess() ? "SUCCESS: " + result.getFilesCopied() + " files" : "ERROR: " + result.getMessage());
        }

        if ("/build".equals(uri)) {
            String workspace = session.getParms().get("path");
            if (workspace == null) return newFixedLengthResponse(Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT, "Missing path");
            
            RcpBuildTool tool = new RcpBuildTool();
            BuildConfiguration config = new BuildConfiguration(new File(workspace));
            config.setSkipTests(true);
            config.addGoal("clean");
            config.addGoal("package");
            BuildResult result = tool.build(config);
            
            saveLog(workspace, result);
            
            return newFixedLengthResponse(result.isSuccess() ? "SUCCESS (" + result.getExecutionTimeMs() + "ms). Log: logs/build.log" : "ERROR: Build failed. See logs/build.log");
        }

        if ("/export".equals(uri)) {
            String workspace = session.getParms().get("path");
            if (workspace == null || workspace.trim().isEmpty()) {
                workspace = baseDir != null ? baseDir.getAbsolutePath() : ".";
            }
            File srcDir = new File(workspace);
            File parentDir = srcDir.getParentFile();
            if (parentDir == null) parentDir = srcDir;
            File exportDir = new File(parentDir, "export");
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }
            try {
                int copiedCount = copyJars(srcDir, exportDir);
                if (copiedCount > 0) {
                    return newFixedLengthResponse("SUCCESS: Exported " + copiedCount + " jars to " + exportDir.getAbsolutePath());
                } else {
                    return newFixedLengthResponse("ERROR: No runnable jars found in " + srcDir.getAbsolutePath() + ". Please build first.");
                }
            } catch (Exception e) {
                return newFixedLengthResponse("ERROR: " + e.getMessage());
            }
        }

        if ("/start-evo".equals(uri)) {
            String workspace = session.getParms().get("path");
            if (workspace == null || workspace.trim().isEmpty()) {
                workspace = baseDir != null ? baseDir.getAbsolutePath() : ".";
            }
            File srcDir = new File(workspace);
            File parentDir = srcDir.getParentFile();
            if (parentDir == null) parentDir = srcDir;
            File exportDir = new File(parentDir, "export");
            File[] jars = exportDir.exists() ? exportDir.listFiles((dir, name) -> name.endsWith(".jar")) : null;
            if (jars == null || jars.length == 0) {
                return newFixedLengthResponse("ERROR: No exported products found in " + exportDir.getAbsolutePath() + ". Please export first.");
            }
            File runnableJar = jars[0];
            for (File jar : jars) {
                if (jar.getName().contains("-shaded")) {
                    runnableJar = jar;
                    break;
                }
            }
            try {
                if (activeEvoProcess != null && activeEvoProcess.isAlive()) {
                    return newFixedLengthResponse("READY (Running) - Already started.");
                }
                ProcessBuilder pb = new ProcessBuilder("java", "-jar", runnableJar.getAbsolutePath());
                pb.directory(exportDir);
                activeEvoProcess = pb.start();
                new Thread(() -> {
                    try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(activeEvoProcess.getInputStream()))) {
                        while (reader.readLine() != null) {}
                    } catch (Exception ignored) {}
                }).start();
                return newFixedLengthResponse("SUCCESS: Started product " + runnableJar.getName());
            } catch (Exception e) {
                return newFixedLengthResponse("ERROR: " + e.getMessage());
            }
        }

        if ("/stop-evo".equals(uri)) {
            if (activeEvoProcess != null && activeEvoProcess.isAlive()) {
                activeEvoProcess.destroyForcibly();
                try {
                    activeEvoProcess.waitFor(2, java.util.concurrent.TimeUnit.SECONDS);
                } catch (Exception ignored) {}
                activeEvoProcess = null;
                return newFixedLengthResponse("SUCCESS: Stopped running product.");
            } else {
                activeEvoProcess = null;
                return newFixedLengthResponse("READY (Stopped) - Product was not running.");
            }
        }

        return newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Not Found");
    }

    private static int copyJars(File dir, File exportDir) {
        int count = 0;
        if (dir.isDirectory()) {
            if (dir.getName().equals("target")) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.isFile() && f.getName().endsWith(".jar")) {
                            try {
                                java.nio.file.Files.copy(f.toPath(), new File(exportDir, f.getName()).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                                count++;
                            } catch (Exception ignored) {}
                        }
                    }
                }
                return count;
            }
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) {
                        String name = f.getName();
                        if (!name.equals(".git") && !name.equals("self-dev-run") && !name.equals(".settings") && !name.equals(".metadata") && !name.equals("bin")) {
                            count += copyJars(f, exportDir);
                        }
                    }
                }
            }
        }
        return count;
    }

    private void saveLog(String workspace, BuildResult result) {
        File logDir = new File(workspace, "../logs");
        if (!logDir.exists()) logDir.mkdirs();
        try (FileWriter fw = new FileWriter(new File(logDir, "build.log"))) {
            fw.write("STDOUT:\n" + result.getStdout() + "\n\nSTDERR:\n" + result.getStderr());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleCliCommand(String[] args) {
        String cmd = args[0];
        if ("--copy".equals(cmd)) {
            if (args.length < 3) return;
            CodebaseCopyTool tool = new CodebaseCopyTool();
            CopyConfiguration config = new CopyConfiguration(new File(args[1]), new File(args[2]));
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
            System.out.println(result.isSuccess() ? "SUCCESS: " + result.getFilesCopied() + " files" : "ERROR: " + result.getMessage());
        } else if ("--build".equals(cmd)) {
            if (args.length < 2) return;
            RcpBuildTool tool = new RcpBuildTool();
            BuildConfiguration config = new BuildConfiguration(new File(args[1]));
            config.setSkipTests(true);
            config.addGoal("clean");
            config.addGoal("package");
            BuildResult result = tool.build(config);
            System.out.println(result.isSuccess() ? "SUCCESS: " + result.getExecutionTimeMs() + "ms" : "ERROR: Build failed");
        }
    }
}
