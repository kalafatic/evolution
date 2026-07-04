package eu.kalafatic.evolution.forge.agent;

import java.nio.file.Paths;

public class ForgeEvoAgent {
    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }

        String projectPath = ".";
        for (int i = 0; i < args.length; i++) {
            if ("--project-path".equals(args[i]) && i + 1 < args.length) {
                projectPath = args[i + 1];
            }
        }

        System.out.println("Starting Evo Forge Agent...");

        try {
            EvoForgeServer server = new EvoForgeServer(58081);
            server.start(fi.iki.elonen.NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            System.out.println("Dashboard available at http://localhost:58081");

            TrainingOrchestrator orchestrator = new TrainingOrchestrator(Paths.get(projectPath), null);
            server.updateProgress("Processing Data", 20);
            orchestrator.runPipeline();

            server.updateProgress("Complete", 100);
            System.out.println("Agent execution finished.");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void printUsage() {
        System.out.println("Usage: forge-evo --project-path <path>");
    }
}
