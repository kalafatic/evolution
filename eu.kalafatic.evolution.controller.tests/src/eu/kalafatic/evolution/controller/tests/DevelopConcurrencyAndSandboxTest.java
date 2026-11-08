package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.develop.DevelopPermissions;
import eu.kalafatic.evolution.controller.orchestration.develop.DevelopSession;
import eu.kalafatic.evolution.controller.orchestration.develop.DevelopSessionManager;
import eu.kalafatic.evolution.controller.orchestration.develop.DevelopTask;
import eu.kalafatic.evolution.controller.orchestration.develop.DevelopTaskManager;
import eu.kalafatic.evolution.controller.orchestration.develop.DevelopTaskStatus;
import eu.kalafatic.evolution.controller.orchestration.develop.agent.DevelopTaskPlanner;
import eu.kalafatic.evolution.controller.orchestration.develop.executor.ProcessExecutor;
import eu.kalafatic.evolution.controller.orchestration.develop.executor.SandboxProcessExecutor;
import eu.kalafatic.evolution.controller.orchestration.develop.sync.ProjectLockManager;
import eu.kalafatic.evolution.controller.orchestration.develop.vcs.GitService;
import eu.kalafatic.evolution.controller.orchestration.develop.vcs.GitServiceImpl;
import eu.kalafatic.evolution.controller.orchestration.develop.workspace.RepositoryWorkspaceManager;

public class DevelopConcurrencyAndSandboxTest {

    private DevelopTaskManager taskManager;
    private DevelopSessionManager sessionManager;
    private File tempRepoA;
    private File tempRepoB;

    @Before
    public void setUp() throws Exception {
        taskManager = DevelopTaskManager.getInstance();
        sessionManager = DevelopSessionManager.getInstance();
        sessionManager.clearAllSessions();

        GitService gitService = new GitServiceImpl();

        tempRepoA = Files.createTempDirectory("evo-test-repo-a").toFile();
        gitService.ensureInitialCommit(tempRepoA);
        Files.writeString(new File(tempRepoA, "App.java").toPath(), "public class App {}");
        gitService.commitChanges(tempRepoA, "Initial commit A");

        tempRepoB = Files.createTempDirectory("evo-test-repo-b").toFile();
        gitService.ensureInitialCommit(tempRepoB);
        Files.writeString(new File(tempRepoB, "AppB.java").toPath(), "public class AppB {}");
        gitService.commitChanges(tempRepoB, "Initial commit B");
    }

    @After
    public void tearDown() throws Exception {
        sessionManager.clearAllSessions();
        deleteDirectory(tempRepoA);
        deleteDirectory(tempRepoB);
    }

    private void deleteDirectory(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) deleteDirectory(f);
        }
        dir.delete();
    }

    @Test
    public void test1_ConcurrentTasksSameRepositoryCoordination() throws Exception {
        ProjectLockManager lockMgr = ProjectLockManager.getInstance();
        String projectKey = tempRepoA.getAbsolutePath();

        boolean lock1 = lockMgr.acquireProjectLock(projectKey, "task-1", 1000, 30000);
        assertTrue("First task should acquire lock", lock1);

        boolean lock2 = lockMgr.acquireProjectLock(projectKey, "task-2", 200, 30000);
        assertFalse("Second task on same repo should not acquire lock while held", lock2);

        lockMgr.releaseProjectLock(projectKey, "task-1");

        boolean lock2Retry = lockMgr.acquireProjectLock(projectKey, "task-2", 1000, 30000);
        assertTrue("Second task should acquire lock after release", lock2Retry);
        lockMgr.releaseProjectLock(projectKey, "task-2");
    }

    @Test
    public void test2_ConcurrentTasksDifferentRepositoriesParallel() throws Exception {
        ProjectLockManager lockMgr = ProjectLockManager.getInstance();
        String repoA = tempRepoA.getAbsolutePath();
        String repoB = tempRepoB.getAbsolutePath();

        boolean lockA = lockMgr.acquireProjectLock(repoA, "task-A", 1000, 30000);
        boolean lockB = lockMgr.acquireProjectLock(repoB, "task-B", 1000, 30000);

        assertTrue("Task A should acquire lock on repo A", lockA);
        assertTrue("Task B should acquire lock on repo B in parallel", lockB);

        lockMgr.releaseProjectLock(repoA, "task-A");
        lockMgr.releaseProjectLock(repoB, "task-B");
    }

    @Test
    public void test3_RepositoryCloneReuseAndMirrorKey() throws Exception {
        RepositoryWorkspaceManager rwm = RepositoryWorkspaceManager.getInstance();
        String key1 = rwm.computeRepoKey("https://github.com/org/repo.git", "user1");
        String key2 = rwm.computeRepoKey("https://github.com/org/repo.git", "user1");
        String keyUser2 = rwm.computeRepoKey("https://github.com/org/repo.git", "user2");

        assertEquals("Same repo and user must have stable cache key", key1, key2);
        assertFalse("Different users must have isolated cache keys", key1.equals(keyUser2));
    }

    @Test
    public void test4_IndependentWorkspaceIsolation() throws Exception {
        DevelopPermissions perms = new DevelopPermissions(true, true, true, false);
        DevelopTask t1 = taskManager.createTask("Task 1", "Desc", tempRepoA.getAbsolutePath(), "branch-1", perms);
        DevelopTask t2 = taskManager.createTask("Task 2", "Desc", tempRepoA.getAbsolutePath(), "branch-2", perms);

        DevelopSession s1 = sessionManager.createSession(t1);
        DevelopSession s2 = sessionManager.createSession(t2);

        assertFalse("Workspaces must have separate paths",
                s1.getWorkspaceDir().getAbsolutePath().equals(s2.getWorkspaceDir().getAbsolutePath()));

        File fileS1 = new File(s1.getRepositoryDir(), "NewFile1.txt");
        Files.writeString(fileS1.toPath(), "Hello from S1");

        File fileS2 = new File(s2.getRepositoryDir(), "NewFile1.txt");
        assertFalse("File created in s1 must not exist in s2 workspace", fileS2.exists());
    }

    @Test
    public void test5_WorkspaceAndRepositoryLockingTimeoutAndPreemption() throws Exception {
        ProjectLockManager lockMgr = ProjectLockManager.getInstance();
        String project = "dummy-project";

        boolean acq1 = lockMgr.acquireProjectLock(project, "task-short", 500, 200);
        assertTrue(acq1);

        Thread.sleep(300); // Wait for lease expiry

        boolean acq2 = lockMgr.acquireProjectLock(project, "task-new", 500, 5000);
        assertTrue("Expired lease should allow preemption by new task", acq2);
        lockMgr.releaseProjectLock(project, "task-new");
    }

    @Test
    public void test6_TaskDependencyExecutionOrder() throws Exception {
        DevelopPermissions perms = new DevelopPermissions(true, true, true, false);
        DevelopTask dep = taskManager.createTask("Dep Task", "Prerequisite", "", "main", perms);
        DevelopTask child = taskManager.createTask("Child Task", "Follow-up", "", "main", perms);

        child.setDependsOnTaskIds(Arrays.asList(dep.getId()));

        taskManager.submitTaskForExecution(child.getId());
        taskManager.triggerDispatch();
        assertEquals(DevelopTaskStatus.WAITING_FOR_DEPENDENCIES, child.getStatus());

        dep.setStatus(DevelopTaskStatus.COMPLETED);
        taskManager.triggerDispatch();

        assertTrue(child.getStatus() != DevelopTaskStatus.WAITING_FOR_DEPENDENCIES);
    }

    @Test
    public void test7_DuplicateTaskDispatchPrevention() throws Exception {
        DevelopPermissions perms = new DevelopPermissions(true, true, true, false);
        DevelopTask task = taskManager.createTask("Dup Task", "Test duplicate submit", "", "main", perms);

        taskManager.submitTaskForExecution(task.getId());
        DevelopTaskStatus status1 = task.getStatus();

        taskManager.submitTaskForExecution(task.getId());
        assertEquals(status1, task.getStatus());
    }

    @Test
    public void test8_SandboxEnvironmentSanitization() throws Exception {
        SandboxProcessExecutor sandbox = new SandboxProcessExecutor();
        Map<String, String> taintedEnv = new HashMap<>();
        taintedEnv.put("PATH", "/usr/bin:/bin");
        taintedEnv.put("EVO_API_KEY", "super-secret-token");
        taintedEnv.put("GITHUB_TOKEN", "ghp_12345");
        taintedEnv.put("SECRET_PASSWORD", "admin123");

        Map<String, String> sanitized = sandbox.sanitizeEnvironment(taintedEnv);

        assertTrue("PATH must be preserved", sanitized.containsKey("PATH"));
        assertFalse("EVO_API_KEY must be stripped", sanitized.containsKey("EVO_API_KEY"));
        assertFalse("GITHUB_TOKEN must be stripped", sanitized.containsKey("GITHUB_TOKEN"));
        assertFalse("SECRET_PASSWORD must be stripped", sanitized.containsKey("SECRET_PASSWORD"));
    }

    @Test
    public void test9_SandboxProcessTerminationOnTimeout() throws Exception {
        SandboxProcessExecutor sandbox = new SandboxProcessExecutor();
        boolean isWin = System.getProperty("os.name").toLowerCase().contains("win");
        String sleepCmd = isWin ? "powershell -Command Start-Sleep -Seconds 10" : "sleep 10";

        File tempDir = Files.createTempDirectory("evo-sandbox-timeout").toFile();
        try {
            long start = System.currentTimeMillis();
            ProcessExecutor.ProcessResult res = sandbox.executeSandboxed(sleepCmd, tempDir, tempDir, null, 1000);
            long elapsed = System.currentTimeMillis() - start;

            assertTrue("Process should be terminated around 1000ms", elapsed < 5000);
            assertTrue("Result should reflect timeout", res.isTimedOut());
        } finally {
            deleteDirectory(tempDir);
        }
    }

    @Test
    public void test10_SandboxDirectoryContainmentViolation() throws Exception {
        SandboxProcessExecutor sandbox = new SandboxProcessExecutor();
        File allowedRoot = Files.createTempDirectory("evo-allowed-root").toFile();
        File externalDir = Files.createTempDirectory("evo-allowed-root-sibling").toFile();

        try {
            ProcessExecutor.ProcessResult res = sandbox.executeSandboxed("echo test", externalDir, allowedRoot, null, 5000);
            assertFalse("Execution outside allowed root must fail", res.isSuccess());
            assertTrue("Error must describe sandbox violation", res.getStderr().contains("Sandbox violation"));
        } finally {
            deleteDirectory(allowedRoot);
            deleteDirectory(externalDir);
        }
    }

    @Test
    public void test11_ServerRestartStateRecovery() throws Exception {
        DevelopPermissions perms = new DevelopPermissions(true, true, true, false);
        DevelopTask runningTask = taskManager.createTask("Interrupted Task", "Running during crash", "", "main", perms);
        runningTask.setStatus(DevelopTaskStatus.BUILDING);

        taskManager.recoverTasksOnRestart();

        assertEquals(DevelopTaskStatus.FAILED, runningTask.getStatus());
    }

    @Test
    public void test12_UserIsolationAndCredentialSegregation() throws Exception {
        RepositoryWorkspaceManager rwm = RepositoryWorkspaceManager.getInstance();
        String repoUrl = "https://user:token@github.com/myorg/private-repo.git";

        String user1Key = rwm.computeRepoKey(repoUrl, "alice");
        String user2Key = rwm.computeRepoKey(repoUrl, "bob");

        assertFalse("Different users accessing same private URL must not share cache keys",
                user1Key.equals(user2Key));
    }

    @Test
    public void test13_AiTaskPlannerDecomposition() throws Exception {
        DevelopTaskPlanner planner = new DevelopTaskPlanner();
        String compound = "Analyze architecture, fix auth bugs, and update documentation";

        DevelopTaskPlanner.ExecutionPlanProposal proposal = planner.planCompoundRequest(compound, "repo-url", "main");
        assertNotNull(proposal);
        assertTrue("Should decompose into multiple sub-tasks", proposal.subTasks.size() >= 3);

        List<DevelopTask> created = planner.materializePlan(proposal, "repo-url", "main", "alice");
        assertEquals(proposal.subTasks.size(), created.size());
    }

    @Test
    public void test14_GitBranchIsolation() throws Exception {
        GitService gitService = new GitServiceImpl();
        gitService.createAndCheckoutBranch(tempRepoA, "develop/feature-xyz");
        assertTrue("Must be a valid git repository", gitService.isGitRepository(tempRepoA));
    }
}
