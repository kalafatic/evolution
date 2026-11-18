package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import eu.kalafatic.evolution.controller.orchestration.selfdev.ProcessOwnershipRecord;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SupervisorRuntime;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskResult;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskStatus;
import eu.kalafatic.evolution.controller.resource.ResourceManager;

/**
 * WP5 - Supervisor process ownership and lifecycle tests.
 * All termination tests use records whose PID identity cannot match the current
 * JVM so that refusal paths are verified without killing anything real.
 */
public class SelfDevProcessOwnershipTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private ResourceManager resourceManager;
    private final java.util.List<File> createdRunDirs = new java.util.ArrayList<>();

    @Before
    public void setUp() {
        resourceManager = ResourceManager.getInstance();
        resourceManager.refresh();
    }

    @After
    public void tearDown() {
        for (File dir : createdRunDirs) {
            try {
                deleteRecursively(dir);
            } catch (Exception ignored) {}
        }
        createdRunDirs.clear();
    }

    private void deleteRecursively(File f) throws Exception {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File c : children) {
                    deleteRecursively(c);
                }
            }
        }
        Files.deleteIfExists(f.toPath());
    }

    private SelfDevContext newContext() {
        SelfDevContext context = new SelfDevContext(null, resourceManager.getOrchestrator());
        createdRunDirs.add(context.getBuildDirectory().getParentFile());
        return context;
    }

    @Test
    public void testOwnershipRecordRoundTrip() throws Exception {
        SelfDevContext context = newContext();
        Map<String, Integer> ports = new LinkedHashMap<>();
        ports.put("supervisor", 28480);
        ports.put("control", 28580);

        ProcessOwnershipRecord record = new ProcessOwnershipRecord(
                ProcessOwnershipRecord.ProcessKind.SUPERVISOR, context.getRunId(), "inst-1",
                12345, 999, "java", "java -jar /x/supervisor.jar --port 28480",
                "/x/runtime", "/x/runtime/supervisor", System.currentTimeMillis(), ports, null);

        ProcessOwnershipRecord.write(context, record);

        File ownershipFile = new File(context.getRuntimeDirectory(), ProcessOwnershipRecord.OWNERSHIP_FILE_NAME);
        assertTrue("Ownership record must be persisted", ownershipFile.exists());

        ProcessOwnershipRecord read = ProcessOwnershipRecord.read(context, ProcessOwnershipRecord.ProcessKind.SUPERVISOR);
        assertNotNull(read);
        assertEquals(12345, read.getPid());
        assertEquals(context.getRunId(), read.getRunId());
        assertEquals("inst-1", read.getSupervisorInstanceId());
        assertEquals(Integer.valueOf(28480), read.getEffectivePorts().get("supervisor"));
        assertTrue("Run-scoped ownership check must pass", ProcessOwnershipRecord.isOwnedByRun(read, context));
        assertFalse("A foreign run must not be considered owned",
                ProcessOwnershipRecord.isOwnedByRun(read, newContext()));

        ProcessOwnershipRecord.clear(context, ProcessOwnershipRecord.ProcessKind.SUPERVISOR, 143);
        ProcessOwnershipRecord cleared = ProcessOwnershipRecord.read(context, ProcessOwnershipRecord.ProcessKind.SUPERVISOR);
        assertNotNull(cleared);
        assertEquals(Integer.valueOf(143), cleared.getExitStatus());
    }

    @Test
    public void testVerifyIdentityRejectsDeadAndUnknownPids() {
        ProcessOwnershipRecord dead = new ProcessOwnershipRecord(
                ProcessOwnershipRecord.ProcessKind.SUPERVISOR, "run-x", "i",
                999999999, -1, "java", "java -jar supervisor.jar", "/x", "/x", System.currentTimeMillis(),
                new LinkedHashMap<>(), null);
        assertFalse("Unknown PID must fail identity verification", ProcessOwnershipRecord.verifyIdentity(dead));
    }

    @Test
    public void testVerifyIdentityRejectsMismatchedCommandLine() {
        // Record claims the supervisor jar command line, but the live PID is the
        // current JVM (whose command line does not contain the supervisor jar).
        long ownPid = ProcessHandle.current().pid();
        ProcessOwnershipRecord mismatched = new ProcessOwnershipRecord(
                ProcessOwnershipRecord.ProcessKind.SUPERVISOR, "run-x", "i",
                ownPid, -1, "java", "java -jar /somewhere/eu.kalafatic.evolution.supervisor.jar --port 28480",
                "/x", "/x", System.currentTimeMillis(), new LinkedHashMap<>(), null);
        assertFalse("Command-line mismatch must fail identity verification",
                ProcessOwnershipRecord.verifyIdentity(mismatched));
    }

    @Test
    public void testTerminateRefusesMismatchedRecordAndParentSurvives() {
        long ownPid = ProcessHandle.current().pid();
        ProcessOwnershipRecord mismatched = new ProcessOwnershipRecord(
                ProcessOwnershipRecord.ProcessKind.SUPERVISOR, "run-x", "i",
                ownPid, -1, "java", "java -jar /somewhere/eu.kalafatic.evolution.supervisor.jar --port 28480",
                "/x", "/x", System.currentTimeMillis(), new LinkedHashMap<>(), null);
        // Must refuse to terminate and the current JVM must survive.
        ProcessOwnershipRecord.terminateOwnedProcessTree(mismatched);
        assertTrue("Parent JVM must survive a mismatched termination attempt",
                ProcessHandle.current().isAlive());
    }

    @Test
    public void testSupervisorStopRefusesMismatchedLivePid() throws Exception {
        SelfDevContext context = newContext();
        long ownPid = ProcessHandle.current().pid();
        // Record a live PID (the current JVM) with a command line that cannot
        // match it -> stop must REFUSE with FAILED, not report success.
        Map<String, Integer> ports = new LinkedHashMap<>();
        ports.put("supervisor", 28480);
        ports.put("control", 28580);
        ProcessOwnershipRecord mismatched = new ProcessOwnershipRecord(
                ProcessOwnershipRecord.ProcessKind.SUPERVISOR, context.getRunId(), "i",
                ownPid, -1, "java", "java -jar /somewhere/eu.kalafatic.evolution.supervisor.jar --port 28480",
                "/x", "/x", System.currentTimeMillis(), ports, null);
        ProcessOwnershipRecord.write(context, mismatched);

        SupervisorRuntime runtime = new SupervisorRuntime();
        TaskResult res = runtime.stop(context);
        assertEquals("STOP must refuse a live PID whose identity does not match (D-18)",
                TaskStatus.FAILED, res.getStatus());
        assertTrue(res.getMessage().contains("refused"));
        assertTrue("Parent JVM must survive", ProcessHandle.current().isAlive());
    }

    @Test
    public void testSupervisorStopNoopWithoutOwnership() {
        SelfDevContext context = newContext();
        SupervisorRuntime runtime = new SupervisorRuntime();
        TaskResult res = runtime.stop(context);
        assertTrue("No ownership => honest no-op success", res.isSuccess());
        assertTrue(res.getMessage().contains("not owned"));
    }

    @Test
    public void testEvoOwnershipRoundTrip() throws Exception {
        SelfDevContext context = newContext();
        Map<String, Integer> ports = new LinkedHashMap<>();
        ports.put("server", 28080);
        ProcessOwnershipRecord record = new ProcessOwnershipRecord(
                ProcessOwnershipRecord.ProcessKind.EVO_RCP, context.getRunId(), context.getRunId() + "-EVO_RCP",
                4242, 100, "evo", "./evo --port 28080 --mode=SELF_DEV", "/x/evo", "/x/evo",
                System.currentTimeMillis(), ports, null);
        ProcessOwnershipRecord.write(context, record);
        ProcessOwnershipRecord read = ProcessOwnershipRecord.read(context, ProcessOwnershipRecord.ProcessKind.EVO_RCP);
        assertNotNull(read);
        assertEquals(4242, read.getPid());
        assertEquals(Integer.valueOf(28080), read.getEffectivePorts().get("server"));
        assertNull("Fresh record must have no exit status", read.getExitStatus());
    }
}
