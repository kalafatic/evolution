package eu.kalafatic.evolution.controller.orchestration.develop.tools;

import java.io.File;

import eu.kalafatic.evolution.controller.orchestration.develop.DevelopPermissions;
import eu.kalafatic.evolution.controller.orchestration.develop.DevelopSession;
import eu.kalafatic.evolution.controller.orchestration.develop.executor.ProcessExecutor;
import eu.kalafatic.evolution.controller.orchestration.develop.executor.ProcessExecutorImpl;

public class RunBuildTool implements DevelopTool {

    private final ProcessExecutor processExecutor = new ProcessExecutorImpl();

    @Override
    public String getName() {
        return "RunBuild";
    }

    @Override
    public String getDescription() {
        return "Executes project build using detected build tool.";
    }

    @Override
    public boolean isAllowed(DevelopPermissions permissions) {
        return permissions != null && permissions.canExecute();
    }

    @Override
    public ToolResult execute(DevelopSession session, ToolInput input) throws Exception {
        if (!isAllowed(session.getTask().getPermissions())) {
            return new ToolResult(false, "", "EXECUTE permission denied");
        }

        File repoDir = session.getRepositoryDir();
        if (!repoDir.exists()) {
            return new ToolResult(false, "", "Repository directory does not exist: " + repoDir);
        }

        String buildCmd = resolveBuildCommand(repoDir);
        ProcessExecutor.ProcessResult res = processExecutor.execute(buildCmd, repoDir, null, 300000);

        if (res.isSuccess()) {
            return new ToolResult(true, res.getStdout(), "");
        } else {
            return new ToolResult(false, res.getStdout(), res.getStderr());
        }
    }

    private String resolveBuildCommand(File repoDir) {
        boolean isWin = System.getProperty("os.name").toLowerCase().contains("win");
        if (new File(repoDir, "pom.xml").exists()) {
            File mvnw = new File(repoDir, isWin ? "mvnw.cmd" : "mvnw");
            return mvnw.exists() ? (mvnw.getAbsolutePath() + " compile -DskipTests") : "mvn compile -DskipTests";
        }
        if (new File(repoDir, "build.gradle").exists() || new File(repoDir, "build.gradle.kts").exists()) {
            File gradlew = new File(repoDir, isWin ? "gradlew.bat" : "gradlew");
            return gradlew.exists() ? (gradlew.getAbsolutePath() + " assemble") : "gradle assemble";
        }
        if (new File(repoDir, "package.json").exists()) {
            return "npm run build --if-present";
        }
        if (new File(repoDir, "CMakeLists.txt").exists()) {
            return "cmake --build .";
        }
        if (new File(repoDir, "Makefile").exists()) {
            return "make";
        }
        return "echo 'No explicit build tool detected'";
    }
}
