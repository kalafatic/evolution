package eu.kalafatic.evolution.supervisor;

import java.io.File;
import eu.kalafatic.evolution.supervisor.bootstrap.CodebaseCopyTool;
import eu.kalafatic.evolution.supervisor.bootstrap.CopyConfiguration;
import eu.kalafatic.evolution.supervisor.bootstrap.CopyResult;
import eu.kalafatic.evolution.supervisor.bootstrap.RcpBuildTool;
import eu.kalafatic.evolution.supervisor.bootstrap.BuildConfiguration;
import eu.kalafatic.evolution.supervisor.bootstrap.BuildResult;

public class SupervisorMain {
    public static void main(String[] args) {
        if (args.length > 0 && args[0].startsWith("--")) {
            handleCommand(args);
            return;
        }

        System.out.println("=== EVO AI SUPERVISOR STARTING ===");
        String path = (args.length > 0) ? args[0] : ".";
        File baseDir = new File(path);
        System.out.println("[CONFIG] Base Directory: " + baseDir.getAbsolutePath());
        SelfDevSupervisor supervisor = new SelfDevSupervisor(baseDir);
        supervisor.run();
        System.out.println("=== EVO AI SUPERVISOR FINISHED ===");
    }

    private static void handleCommand(String[] args) {
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
