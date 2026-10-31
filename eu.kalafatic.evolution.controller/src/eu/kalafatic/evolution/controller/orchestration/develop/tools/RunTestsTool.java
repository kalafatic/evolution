package eu.kalafatic.evolution.controller.orchestration.develop.tools;

import java.io.File;

import eu.kalafatic.evolution.controller.orchestration.develop.DevelopPermissions;
import eu.kalafatic.evolution.controller.orchestration.develop.DevelopSession;
import eu.kalafatic.evolution.controller.orchestration.develop.executor.ProcessExecutor;
import eu.kalafatic.evolution.controller.orchestration.develop.executor.ProcessExecutorImpl;

public class RunTestsTool implements DevelopTool {

    private final ProcessExecutor processExecutor = new ProcessExecutorImpl();

    @Override
    public String getName() {
        return "RunTests";
    }

    @Override
    public String getDescription() {
        return "Executes test suite using detected build tool.";
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

        String testCmd = resolveTestCommand(repoDir);
        eu.kalafatic.evolution.controller.orchestration.develop.ExecutionMetrics metrics =
            new eu.kalafatic.evolution.controller.orchestration.develop.ExecutionMetrics("TEST");
        metrics.setStartedAt(System.currentTimeMillis());
        metrics.setCommand(testCmd);

        if (testCmd == null || testCmd.isEmpty()) {
            metrics.setCompletedAt(System.currentTimeMillis());
            metrics.setStatus(eu.kalafatic.evolution.controller.orchestration.develop.ExecutionStatus.NOT_AVAILABLE);
            metrics.setStdout("No test configuration detected. Skipping test execution.");
            session.getTask().getResult().setTestMetrics(metrics);
            return new ToolResult(true, metrics.getStdout(), "");
        }

        ProcessExecutor.ProcessResult res = processExecutor.execute(testCmd, repoDir, null, 300000);
        metrics.setCompletedAt(System.currentTimeMillis());
        metrics.setExitCode(res.getExitCode());
        metrics.setStdout(res.getStdout());
        metrics.setStderr(res.getStderr());

        if (res.isSuccess()) {
            metrics.setStatus(eu.kalafatic.evolution.controller.orchestration.develop.ExecutionStatus.SUCCESS);
            session.getTask().getResult().setTestMetrics(metrics);
            return new ToolResult(true, res.getStdout(), "");
        } else {
            metrics.setStatus(eu.kalafatic.evolution.controller.orchestration.develop.ExecutionStatus.FAILED);
            session.getTask().getResult().setTestMetrics(metrics);
            return new ToolResult(false, res.getStdout(), res.getStderr());
        }
    }

    public String resolveTestCommand(File repoDir) {
        boolean isWin = System.getProperty("os.name").toLowerCase().contains("win");
        if (new File(repoDir, "pom.xml").exists()) {
            File mvnw = new File(repoDir, isWin ? "mvnw.cmd" : "mvnw");
            return mvnw.exists() ? (mvnw.getAbsolutePath() + " test") : "mvn test";
        }
        if (new File(repoDir, "build.gradle").exists() || new File(repoDir, "build.gradle.kts").exists()) {
            File gradlew = new File(repoDir, isWin ? "gradlew.bat" : "gradlew");
            return gradlew.exists() ? (gradlew.getAbsolutePath() + " test") : "gradle test";
        }
        if (new File(repoDir, "build.xml").exists()) {
            return "ant test";
        }
        if (new File(repoDir, "package.json").exists()) {
            if (new File(repoDir, "pnpm-lock.yaml").exists()) {
                return "pnpm test";
            } else if (new File(repoDir, "yarn.lock").exists()) {
                return "yarn test";
            } else {
                return "npm test";
            }
        }
        if (new File(repoDir, "pytest.ini").exists() || new File(repoDir, "requirements.txt").exists() || new File(repoDir, "setup.py").exists()) {
            return "pytest";
        }
        if (new File(repoDir, "CMakeLists.txt").exists()) {
            return "ctest";
        }
        if (new File(repoDir, "Makefile").exists()) {
            return "make test";
        }
        return "";
    }
}
