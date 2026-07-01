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
            server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            System.out.println("[HTTP] Server started on port 8089");
        } catch (Exception e) {
            System.err.println("[HTTP] Failed to start server: " + e.getMessage());
        }

        SelfDevSupervisor supervisor = new SelfDevSupervisor(baseDir);
        supervisor.run();
        System.out.println("=== EVO AI SUPERVISOR FINISHED ===");
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        if ("/ping".equals(uri)) return newFixedLengthResponse("READY");

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

        return newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Not Found");
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
