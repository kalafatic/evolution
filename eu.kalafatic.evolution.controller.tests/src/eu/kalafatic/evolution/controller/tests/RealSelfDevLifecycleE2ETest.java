package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevBootstrapController;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

public class RealSelfDevLifecycleE2ETest {

    private File projectRoot;
    private Orchestrator orchestrator;
    private File cleanRunDir;
    private SelfDevBootstrapController controller;

    @Before
    public void setUp() {
        projectRoot = new File(".").getAbsoluteFile();
        orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();

        cleanRunDir = new File(System.getProperty("java.io.tmpdir"), "selfdev_e2e_run_" + System.currentTimeMillis());
        if (cleanRunDir.exists()) {
            deleteRecursively(cleanRunDir);
        }
        cleanRunDir.mkdirs();

        controller = new SelfDevBootstrapController(projectRoot, orchestrator);
    }

    @Test
    public void testDirectEvoRcpLifecycleE2E() throws Exception {
        System.out.println("============================================================");
        System.out.println("E2E TEST 1: Direct EVO RCP Lifecycle");
        System.out.println("Run Workspace: " + cleanRunDir.getAbsolutePath());
        System.out.println("============================================================");

        // Step 1: Check Git
        String gitRes = controller.check("GIT_EVO");
        System.out.println("[E2E] Git Check: " + gitRes);
        assertEquals("SUCCESS", gitRes);

        // Step 2: Check Permissions
        String permRes = controller.check("PERMISSIONS");
        System.out.println("[E2E] Permissions Check: " + permRes);
        assertEquals("SUCCESS", permRes);

        // Step 3: Check Genome
        String genomeRes = controller.check("GENOME");
        System.out.println("[E2E] Genome Check: " + genomeRes);
        assertEquals("SUCCESS", genomeRes);

        // Step 4: Check Source Snapshot Integrity
        String integrityRes = controller.checkSourceSnapshotIntegrity(projectRoot);
        System.out.println("[E2E] Source Snapshot Integrity: " + integrityRes);
        assertNull("Snapshot integrity check should pass for repo root", integrityRes);
    }

    @Test
    public void testSupervisorLifecycleE2E() throws Exception {
        System.out.println("============================================================");
        System.out.println("E2E TEST 2: Supervisor Lifecycle");
        System.out.println("Run Workspace: " + cleanRunDir.getAbsolutePath());
        System.out.println("============================================================");

        // Step 1: Check Supervisor status
        assertFalse("Supervisor should initially not be running", controller.isRunning());

        // Step 2: Start Supervisor
        controller.startBootstrap();
        System.out.println("[E2E] Supervisor Bootstrap Started.");

        // Step 3: Check Supervisor check task
        String supervisorCheck = controller.check("SUPERVISOR");
        System.out.println("[E2E] Supervisor Check: " + supervisorCheck);

        // Step 4: Stop Supervisor
        controller.stopBootstrap();
        System.out.println("[E2E] Supervisor Bootstrap Stopped.");
    }

    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.setWritable(true);
        file.delete();
    }
}
