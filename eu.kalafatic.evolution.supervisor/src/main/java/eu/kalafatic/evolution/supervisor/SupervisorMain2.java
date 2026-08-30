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

public class SupervisorMain2 extends NanoHTTPD {
    private static File baseDir;
    private static volatile Process activeEvoProcess;

    public SupervisorMain2(int port) {
        super("127.0.0.1", port);
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

        SupervisorMain2 server = new SupervisorMain2(8089);
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
            String workspace = getDecodedParam(session, "path");
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
            String src = getDecodedParam(session, "src");
            String dest = getDecodedParam(session, "dest");
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
            String workspace = getDecodedParam(session, "path");
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
            String workspace = getDecodedParam(session, "path");
            File exportDir = resolveExportDir(workspace, baseDir);
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }
            try {
                File srcDir = (workspace != null && !workspace.trim().isEmpty()) ? new File(workspace.trim()) : baseDir;
                int copiedCount = copyJars(srcDir, exportDir);
                if (copiedCount > 0) {
                    return newFixedLengthResponse("SUCCESS: Exported " + copiedCount + " assets to " + exportDir.getAbsolutePath());
                } else {
                    return newFixedLengthResponse("ERROR: No runnable jars or assets found in " + srcDir.getAbsolutePath() + ". Please build first.");
                }
            } catch (Exception e) {
                return newFixedLengthResponse("ERROR: " + e.getMessage());
            }
        }

        if ("/start-evo".equals(uri)) {
            String workspace = getDecodedParam(session, "path");
            File exportDir = resolveExportDir(workspace, baseDir);

            File executable = findExecutable(exportDir);
            if (executable == null && exportDir.exists()) {
                File[] archives = exportDir.listFiles((dir, name) -> name.endsWith(".zip") || name.endsWith(".tar.gz"));
                if (archives != null && archives.length > 0) {
                    for (File archive : archives) {
                        try {
                            if (archive.getName().endsWith(".zip")) {
                                unzip(archive, exportDir);
                            } else if (archive.getName().endsWith(".tar.gz")) {
                                untar(archive, exportDir);
                            }
                        } catch (Exception ignored) {}
                    }
                    executable = findExecutable(exportDir);
                }
            }

            if (executable != null) {
                try {
                    if (activeEvoProcess != null && activeEvoProcess.isAlive()) {
                        activeEvoProcess.destroyForcibly();
                    }
                    if (!System.getProperty("os.name").toLowerCase().contains("win")) {
                        executable.setExecutable(true);
                    }
                    ProcessBuilder pb = new ProcessBuilder(executable.getAbsolutePath(), "--mode=SELF_DEV", "--variant=" + baseDir.getAbsolutePath());
                    pb.directory(executable.getParentFile());
                    activeEvoProcess = pb.start();
                    return newFixedLengthResponse("SUCCESS: Started executable product " + executable.getName());
                } catch (Exception e) {
                    return newFixedLengthResponse("ERROR: " + e.getMessage());
                }
            }

            File[] jars = exportDir.exists() ? exportDir.listFiles((dir, name) -> name.endsWith(".jar") && !name.startsWith("original-")) : null;
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

    private static String getDecodedParam(IHTTPSession session, String paramName) {
        String val = session.getParms().get(paramName);
        if (val == null) return null;
        try {
            return java.net.URLDecoder.decode(val, java.nio.charset.StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return val;
        }
    }

    private static File resolveExportDir(String pathParam, File baseDir) {
        if (pathParam != null && !pathParam.trim().isEmpty()) {
            String decodedPath = pathParam.trim();
            try {
                decodedPath = java.net.URLDecoder.decode(decodedPath, java.nio.charset.StandardCharsets.UTF_8.name());
            } catch (Exception ignored) {}
            File pFile = new File(decodedPath);
            if (pFile.getName().equalsIgnoreCase("export")) {
                return pFile;
            }
            File subExport = new File(pFile, "export");
            if (subExport.exists() && subExport.isDirectory()) {
                return subExport;
            }
            if (pFile.getParentFile() != null) {
                File parentExport = new File(pFile.getParentFile(), "export");
                if (parentExport.exists() && parentExport.isDirectory()) {
                    return parentExport;
                }
            }
            if (pFile.getParentFile() != null && (pFile.getName().equalsIgnoreCase("sources") || pFile.getName().equalsIgnoreCase("builds"))) {
                File parentExport = new File(pFile.getParentFile(), "export");
                if (!parentExport.exists()) parentExport.mkdirs();
                return parentExport;
            }
            return subExport;
        }
        File exportDir = new File(baseDir, "export");
        if (!exportDir.exists() && baseDir.getName().equalsIgnoreCase("export")) {
            return baseDir;
        }
        return exportDir;
    }

    private static File findExecutable(File exportDir) {
        if (exportDir == null || !exportDir.exists()) return null;
        boolean isWin = System.getProperty("os.name").toLowerCase().contains("win");
        String exeName = isWin ? "evo.exe" : "evo";
        String shName = "evo.sh";

        File directExe = new File(exportDir, exeName);
        if (directExe.exists() && directExe.isFile()) return directExe;
        if (!isWin) {
            File directSh = new File(exportDir, shName);
            if (directSh.exists() && directSh.isFile()) return directSh;
        }

        File[] subDirs = exportDir.listFiles(File::isDirectory);
        if (subDirs != null) {
            for (File sub : subDirs) {
                File subExe = new File(sub, exeName);
                if (subExe.exists() && subExe.isFile()) return subExe;
                if (!isWin) {
                    File subSh = new File(sub, shName);
                    if (subSh.exists() && subSh.isFile()) return subSh;
                }
            }
        }
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
