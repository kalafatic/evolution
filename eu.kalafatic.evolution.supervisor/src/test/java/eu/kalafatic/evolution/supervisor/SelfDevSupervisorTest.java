package eu.kalafatic.evolution.supervisor;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

public class SelfDevSupervisorTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private File baseDir;
    private SelfDevSupervisor supervisor;

    @Before
    public void setUp() throws IOException {
        baseDir = tempFolder.newFolder("supervisor-test-base");
        new File(baseDir, "self-dev-run").mkdirs();
        supervisor = new SelfDevSupervisor(baseDir);
    }

    @Test
    public void testSuccessfulTaskLifecycle() throws IOException {
        supervisor.getRunner().setMockBuildResult(true);
        supervisor.getRunner().setMockRCPResult(true);
        supervisor.getRunner().setMockPatchResult(true);
        supervisor.getRunner().setMockTestResult(true);
        supervisor.getRunner().setMockRunResult(true);
        supervisor.getRunner().setMockVerifyResult(true);

        DevelopmentTask task = new DevelopmentTask("task-1", "Add repository refresh operation");
        task.setStatus(TaskStatus.READY);
        supervisor.getState().getTaskQueue().add(task);

        supervisor.getState().setCurrentTask(supervisor.getState().getTaskQueue().remove(0));
        Assert.assertEquals("task-1", supervisor.getState().getCurrentTask().getId());
    }

    @Test
    public void testDirectLifecycleSuccess() throws IOException {
        supervisor.getRunner().setMockBuildResult(true);
        supervisor.getRunner().setMockRCPResult(true);
        supervisor.getRunner().setMockPatchResult(true);
        supervisor.getRunner().setMockTestResult(true);
        supervisor.getRunner().setMockRunResult(true);
        supervisor.getRunner().setMockVerifyResult(true);

        DevelopmentTask task = new DevelopmentTask("task-1", "Add repository refresh operation");
        task.setStatus(TaskStatus.READY);
        supervisor.getState().setCurrentTask(task);

        Thread thread = new Thread(() -> supervisor.run());
        thread.start();

        try {
            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < 5000) {
                if (supervisor.getState().getTaskHistory().size() > 0) {
                    break;
                }
                Thread.sleep(100);
            }

            SelfDevProtocol.Control control = new SelfDevProtocol.Control();
            control.forceAction = "STOP";
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.writeValue(new File(baseDir, "self-dev-run/control.json"), control);

            thread.join(2000);
        } catch (InterruptedException ignored) {}

        Assert.assertEquals(1, supervisor.getState().getTaskHistory().size());
        DevelopmentTask completed = supervisor.getState().getTaskHistory().get(0);
        Assert.assertEquals("task-1", completed.getId());
        Assert.assertEquals(TaskStatus.COMPLETED, completed.getStatus());
        Assert.assertEquals(0, supervisor.getState().getConsecutiveFailures());
    }

    @Test
    public void testEvoFailureAndRetry() throws IOException {
        supervisor.getRunner().setMockBuildResult(true);
        supervisor.getRunner().setMockRCPResult(false);
        supervisor.getRunner().setMockTestResult(true);
        supervisor.getRunner().setMockRunResult(true);
        supervisor.getRunner().setMockVerifyResult(true);

        supervisor.getState().setMaxTaskAttempts(2);

        DevelopmentTask task = new DevelopmentTask("task-fail-retry", "Fix repository path resolution");
        task.setStatus(TaskStatus.READY);
        supervisor.getState().setCurrentTask(task);

        Thread thread = new Thread(() -> supervisor.run());
        thread.start();

        try {
            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < 5000) {
                if (supervisor.getState().getTaskHistory().size() > 0) {
                    break;
                }
                Thread.sleep(100);
            }

            SelfDevProtocol.Control control = new SelfDevProtocol.Control();
            control.forceAction = "STOP";
            new com.fasterxml.jackson.databind.ObjectMapper().writeValue(new File(baseDir, "self-dev-run/control.json"), control);
            thread.join(2000);
        } catch (InterruptedException ignored) {}

        Assert.assertEquals(1, supervisor.getState().getTaskHistory().size());
        DevelopmentTask failed = supervisor.getState().getTaskHistory().get(0);
        Assert.assertEquals(TaskStatus.EVO_FAILED, failed.getStatus());
        Assert.assertEquals(2, failed.getAttempts());
        Assert.assertEquals(1, supervisor.getState().getConsecutiveFailures());
    }

    @Test
    public void testBuildFailureAndRepairTask() throws IOException {
        supervisor.getRunner().setMockBuildResult(false); // Build always fails
        supervisor.getRunner().setMockRCPResult(true);
        supervisor.getRunner().setMockTestResult(true);
        supervisor.getRunner().setMockRunResult(true);
        supervisor.getRunner().setMockVerifyResult(true);

        // Customize limits to trigger failure on 2nd repair
        supervisor.getState().setMaxRepairAttempts(1);

        DevelopmentTask task = new DevelopmentTask("task-build-fail", "Add error handling");
        task.setStatus(TaskStatus.READY);
        supervisor.getState().setCurrentTask(task);

        Thread thread = new Thread(() -> supervisor.run());
        thread.start();

        try {
            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < 5000) {
                // Should stop after parent fails and repair fails (exceeding limit of 1 repair)
                if (supervisor.getState().getTaskHistory().size() >= 2) {
                    break;
                }
                Thread.sleep(100);
            }

            SelfDevProtocol.Control control = new SelfDevProtocol.Control();
            control.forceAction = "STOP";
            new com.fasterxml.jackson.databind.ObjectMapper().writeValue(new File(baseDir, "self-dev-run/control.json"), control);
            thread.join(2000);
        } catch (InterruptedException ignored) {}

        // Parent and its first repair should be in history
        Assert.assertTrue(supervisor.getState().getTaskHistory().size() >= 2);
        DevelopmentTask parent = supervisor.getState().getTaskHistory().get(0);
        Assert.assertEquals(TaskStatus.BUILD_FAILED, parent.getStatus());

        DevelopmentTask repair = supervisor.getState().getTaskHistory().get(1);
        Assert.assertEquals("task-build-fail-repair-1", repair.getId());
        Assert.assertEquals("task-build-fail", repair.getParentTaskId());
        Assert.assertEquals(TaskStatus.BUILD_FAILED, repair.getStatus());
    }

    @Test
    public void testTestFailureAndRepairTask() throws IOException {
        supervisor.getRunner().setMockBuildResult(true);
        supervisor.getRunner().setMockRCPResult(true);
        supervisor.getRunner().setMockTestResult(false); // Tests always fail
        supervisor.getRunner().setMockRunResult(true);
        supervisor.getRunner().setMockVerifyResult(true);

        supervisor.getState().setMaxRepairAttempts(1);

        DevelopmentTask task = new DevelopmentTask("task-test-fail", "Add test");
        task.setStatus(TaskStatus.READY);
        supervisor.getState().setCurrentTask(task);

        Thread thread = new Thread(() -> supervisor.run());
        thread.start();

        try {
            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < 5000) {
                if (supervisor.getState().getTaskHistory().size() >= 2) {
                    break;
                }
                Thread.sleep(100);
            }

            SelfDevProtocol.Control control = new SelfDevProtocol.Control();
            control.forceAction = "STOP";
            new com.fasterxml.jackson.databind.ObjectMapper().writeValue(new File(baseDir, "self-dev-run/control.json"), control);
            thread.join(2000);
        } catch (InterruptedException ignored) {}

        Assert.assertTrue(supervisor.getState().getTaskHistory().size() >= 2);
        DevelopmentTask parent = supervisor.getState().getTaskHistory().get(0);
        Assert.assertEquals(TaskStatus.TEST_FAILED, parent.getStatus());

        DevelopmentTask repair = supervisor.getState().getTaskHistory().get(1);
        Assert.assertEquals("task-test-fail-repair-1", repair.getId());
        Assert.assertEquals("task-test-fail", repair.getParentTaskId());
    }

    @Test
    public void testRuntimeFailureAndRepairTask() throws IOException {
        supervisor.getRunner().setMockBuildResult(true);
        supervisor.getRunner().setMockRCPResult(true);
        supervisor.getRunner().setMockTestResult(true);
        supervisor.getRunner().setMockRunResult(false); // Runtime always fails
        supervisor.getRunner().setMockVerifyResult(true);

        supervisor.getState().setMaxRepairAttempts(1);

        DevelopmentTask task = new DevelopmentTask("task-run-fail", "Configure run");
        task.setStatus(TaskStatus.READY);
        supervisor.getState().setCurrentTask(task);

        Thread thread = new Thread(() -> supervisor.run());
        thread.start();

        try {
            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < 5000) {
                if (supervisor.getState().getTaskHistory().size() >= 2) {
                    break;
                }
                Thread.sleep(100);
            }

            SelfDevProtocol.Control control = new SelfDevProtocol.Control();
            control.forceAction = "STOP";
            new com.fasterxml.jackson.databind.ObjectMapper().writeValue(new File(baseDir, "self-dev-run/control.json"), control);
            thread.join(2000);
        } catch (InterruptedException ignored) {}

        Assert.assertTrue(supervisor.getState().getTaskHistory().size() >= 2);
        DevelopmentTask parent = supervisor.getState().getTaskHistory().get(0);
        Assert.assertEquals(TaskStatus.RUN_FAILED, parent.getStatus());

        DevelopmentTask repair = supervisor.getState().getTaskHistory().get(1);
        Assert.assertEquals("task-run-fail-repair-1", repair.getId());
        Assert.assertEquals("task-run-fail", repair.getParentTaskId());
    }

    @Test
    public void testInterruptedTaskRecovery() throws IOException {
        DevelopmentTask task = new DevelopmentTask("task-interrupted", "Verify repository");
        task.setStatus(TaskStatus.BUILDING);

        SupervisorState state = new SupervisorState();
        state.setCurrentTask(task);
        state.save(new File(baseDir, "self-dev-run/supervisor_state.json"));

        SelfDevSupervisor recoverySupervisor = new SelfDevSupervisor(baseDir);
        recoverySupervisor.getRunner().setMockBuildResult(true);
        recoverySupervisor.getRunner().setMockRCPResult(true);
        recoverySupervisor.getRunner().setMockTestResult(true);
        recoverySupervisor.getRunner().setMockRunResult(true);
        recoverySupervisor.getRunner().setMockVerifyResult(true);

        Thread thread = new Thread(() -> recoverySupervisor.run());
        thread.start();

        try {
            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < 5000) {
                if (recoverySupervisor.getState().getTaskHistory().size() > 0) {
                    break;
                }
                Thread.sleep(100);
            }

            SelfDevProtocol.Control control = new SelfDevProtocol.Control();
            control.forceAction = "STOP";
            new com.fasterxml.jackson.databind.ObjectMapper().writeValue(new File(baseDir, "self-dev-run/control.json"), control);
            thread.join(2000);
        } catch (InterruptedException ignored) {}

        Assert.assertEquals(1, recoverySupervisor.getState().getTaskHistory().size());
        DevelopmentTask completed = recoverySupervisor.getState().getTaskHistory().get(0);
        Assert.assertEquals("task-interrupted", completed.getId());
        Assert.assertEquals(TaskStatus.COMPLETED, completed.getStatus());
    }

    @Test
    public void testPlatformDetectionAndBuildCommandSelection() {
        // Test OS Detection
        PlatformInfo.OperatingSystem os = PlatformInfo.getOperatingSystem();
        Assert.assertNotNull(os);

        PlatformInfo.Architecture arch = PlatformInfo.getArchitecture();
        Assert.assertNotNull(arch);

        boolean isWin = PlatformInfo.isWindows();
        boolean isLin = PlatformInfo.isLinux();

        if (isWin) {
            Assert.assertTrue(PlatformInfo.isWindows());
            Assert.assertFalse(PlatformInfo.isLinux());
        } else if (isLin) {
            Assert.assertTrue(PlatformInfo.isLinux());
            Assert.assertFalse(PlatformInfo.isWindows());
        }
    }

    @Test
    public void testCleanEVOProcessShutdown() throws IOException {
        supervisor.getRunner().setMockBuildResult(true);
        supervisor.getRunner().setMockRCPResult(true);
        supervisor.getRunner().setMockTestResult(true);
        supervisor.getRunner().setMockRunResult(true);
        supervisor.getRunner().setMockVerifyResult(true);

        DevelopmentTask task = new DevelopmentTask("task-shutdown", "Graceful termination");
        task.setStatus(TaskStatus.READY);
        supervisor.getState().setCurrentTask(task);

        Thread thread = new Thread(() -> supervisor.run());
        thread.start();

        try {
            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < 5000) {
                if (supervisor.getState().getTaskHistory().size() > 0) {
                    break;
                }
                Thread.sleep(100);
            }

            SelfDevProtocol.Control control = new SelfDevProtocol.Control();
            control.forceAction = "STOP";
            new com.fasterxml.jackson.databind.ObjectMapper().writeValue(new File(baseDir, "self-dev-run/control.json"), control);
            thread.join(2000);
        } catch (InterruptedException ignored) {}

        Assert.assertEquals(1, supervisor.getState().getTaskHistory().size());
        Assert.assertEquals(TaskStatus.COMPLETED, supervisor.getState().getTaskHistory().get(0).getStatus());

        File eventLog = new File(baseDir, "self-dev-run/events.log");
        Assert.assertTrue(eventLog.exists());
        String logs = new String(java.nio.file.Files.readAllBytes(eventLog.toPath()));
        Assert.assertTrue(logs.contains("EVO_SHUTDOWN"));
    }

    @Test
    public void testFindEvoTargetWithExecutableProduct() throws IOException {
        File exportFolder = new File(baseDir, "export");
        exportFolder.mkdirs();

        boolean isWin = PlatformInfo.isWindows();
        File dummyExe = new File(exportFolder, isWin ? "evo.exe" : "evo.sh");
        dummyExe.createNewFile();

        String target = supervisor.findEvoTarget(baseDir);
        Assert.assertNotNull(target);
        Assert.assertEquals(dummyExe.getAbsolutePath(), target);
    }
}
