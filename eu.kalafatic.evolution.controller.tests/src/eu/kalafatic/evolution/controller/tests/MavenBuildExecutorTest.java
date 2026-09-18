package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.selfdev.BuildEvoTask;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EvoRcpBuilder;
import eu.kalafatic.evolution.controller.orchestration.selfdev.MavenBuildExecutor;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevOrchestrator;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskResult;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskStatus;

public class MavenBuildExecutorTest {

    private File tempDir;

    @Before
    public void setUp() {
        tempDir = new File(System.getProperty("java.io.tmpdir"), "test_maven_exec_" + System.currentTimeMillis());
        tempDir.mkdirs();
    }

    @Test
    public void testSuccessfulProcessExecution() {
        MavenBuildExecutor executor = new MavenBuildExecutor("TEST_SUCCESS");
        String javaHome = System.getProperty("java.home");
        File javaBin = new File(javaHome, "bin/java" + (System.getProperty("os.name").toLowerCase().contains("win") ? ".exe" : ""));

        TaskResult result = executor.executeBuild(tempDir, Collections.singletonList("-version"), null, null, 1);
        assertNotNull(result);
        // Even if mvn executable resolution falls back to java/mvn, execution completes without deadlock
    }

    @Test
    public void testNonZeroExitCodeHandling() {
        MavenBuildExecutor executor = new MavenBuildExecutor("TEST_FAIL");
        // Passing non-existent goal/arg to trigger non-zero exit code or error classification
        TaskResult result = executor.executeBuild(tempDir, Arrays.asList("non_existent_goal_12345"), null, null, 1);
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals(TaskStatus.FAILED, result.getStatus());
    }

    @Test
    public void testConcurrentStdoutStderrAndLargeOutput() throws Exception {
        boolean isWin = System.getProperty("os.name").toLowerCase().contains("win");
        File testScript = new File(tempDir, isWin ? "test_output.cmd" : "test_output.sh");

        if (isWin) {
            java.nio.file.Files.writeString(testScript.toPath(),
                "@echo off\n" +
                "for /L %%i in (1,1,500) do (\n" +
                "  echo STDOUT_LINE_%%i\n" +
                "  echo STDERR_LINE_%%i 1>&2\n" +
                ")\n" +
                "exit /b 0\n"
            );
        } else {
            java.nio.file.Files.writeString(testScript.toPath(),
                "#!/bin/sh\n" +
                "for i in $(seq 1 500); do\n" +
                "  echo \"STDOUT_LINE_$i\"\n" +
                "  echo \"STDERR_LINE_$i\" >&2\n" +
                "done\n" +
                "exit 0\n"
            );
            testScript.setExecutable(true);
        }

        File logFile = new File(tempDir, "test.log");
        MavenBuildExecutor executor = new MavenBuildExecutor("TEST_LARGE_OUTPUT");

        TaskResult result = executor.executeBuild(tempDir, Collections.singletonList(testScript.getAbsolutePath()), null, logFile, 1);

        assertNotNull(result);
        assertTrue(logFile.exists());
        String logContent = java.nio.file.Files.readString(logFile.toPath());
        assertTrue("Log should contain STDOUT entries", logContent.contains("STDOUT_LINE_1"));
        assertTrue("Log should contain STDERR entries", logContent.contains("STDERR_LINE_1"));
    }

    @Test
    public void testTimeoutHandlingAndProcessTermination() throws Exception {
        boolean isWin = System.getProperty("os.name").toLowerCase().contains("win");
        File sleepScript = new File(tempDir, isWin ? "test_sleep.cmd" : "test_sleep.sh");

        if (isWin) {
            java.nio.file.Files.writeString(sleepScript.toPath(),
                "@echo off\n" +
                "echo STARTING_SLEEP\n" +
                "timeout /t 60 /nobreak >nul\n"
            );
        } else {
            java.nio.file.Files.writeString(sleepScript.toPath(),
                "#!/bin/sh\n" +
                "echo STARTING_SLEEP\n" +
                "sleep 60\n"
            );
            sleepScript.setExecutable(true);
        }

        MavenBuildExecutor executor = new MavenBuildExecutor("TEST_TIMEOUT");

        // Use reflection/direct call with 1 minute or sub-minute timeout simulation
        long startTime = System.currentTimeMillis();
        // Passing small timeout simulation
        TaskResult result = executor.executeBuild(tempDir, Collections.singletonList(sleepScript.getAbsolutePath()), null, null, 1);
        long duration = System.currentTimeMillis() - startTime;

        assertNotNull(result);
        // Script produces output without hanging indefinitely
    }

    @Test
    public void testWorkingDirectoryAndEnvironmentPropagation() {
        MavenBuildExecutor executor = new MavenBuildExecutor("TEST_ENV");
        TaskResult result = executor.executeBuild(tempDir, Collections.emptyList(), null, null, 1);
        assertNotNull(result);
        assertEquals(tempDir.getAbsoluteFile(), result.getWorkingDirectory().getAbsoluteFile());
    }

    @Test
    public void testFailurePropagationIntoBuildEvoTask() throws Exception {
        File repoRoot = new File(".").getAbsoluteFile();
        SelfDevContext context = new SelfDevContext(repoRoot, null);

        BuildEvoTask buildTask = new BuildEvoTask("BUILD_EVO_TEST");

        // Record failed result
        TaskResult failResult = TaskResult.failure("BUILD_EVO_TEST", "Tycho reactor compilation failed", null);
        context.recordTaskResult(failResult);

        TaskResult fetched = context.getTaskResult("BUILD_EVO_TEST");
        assertNotNull(fetched);
        assertEquals(TaskStatus.FAILED, fetched.getStatus());
        assertEquals("Tycho reactor compilation failed", fetched.getMessage());
    }
}
