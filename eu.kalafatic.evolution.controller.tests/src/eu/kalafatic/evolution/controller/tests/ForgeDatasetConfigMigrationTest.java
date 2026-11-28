package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager;
import eu.kalafatic.evolution.model.orchestration.ForgeSession;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Regression test for the forge dataset-config migration: when a global EVO session binds
 * its Forge state via {@code getOrCreateForgeSession}, the previously-configured dataset
 * folders (stored on the legacy seeded "Self-Evo Forging" session) are carried over so the
 * Darwin engine reads them under the global id instead of falling back to the chat target
 * folder.
 */
public class ForgeDatasetConfigMigrationTest {

    @Test
    public void testGetOrCreateForgeSessionMigratesDatasetsFromDefault() throws Exception {
        ForgeSessionManager mgr = ForgeSessionManager.getInstance();
        Path stateDir = Files.createTempDirectory("forge-migrate-test");
        ForgeSessionManager.setForgeStateDirectory(stateDir);
        try {
            Orchestrator orch = OrchestrationFactory.eINSTANCE.createOrchestrator();
            orch.setId("test-orch-migrate");
            mgr.initialize(orch); // seeds the default "Self-Evo Forging" session

            ForgeSession def = mgr.findDefaultSession();
            assertNotNull("default session must be seeded", def);

            String datasetsJson = "[{\"path\":\"/home/petr/workspace/forge-input/OpenAssist\",\"checked\":true,\"type\":\"FOLDER\"}]";
            mgr.updateUiState(def.getSessionId(), "datasets", datasetsJson);

            ForgeSession bound = mgr.getOrCreateForgeSession("global-uuid-1");
            assertNotNull(bound);
            assertEquals("global-uuid-1", bound.getSessionId());

            // Use the Object view of the uiState JSON to avoid the org.json classloader
            // constraint conflict between the controller and test bundles.
            Object ui = mgr.getUiState("global-uuid-1");
            assertNotNull(ui);
            String uiStr = ui.toString();
            assertTrue("datasets must be migrated from the default session", uiStr.contains("datasets"));
            assertTrue("migrated datasets must reference the configured folder", uiStr.contains("OpenAssist"));
        } finally {
            ForgeSessionManager.setForgeStateDirectory(null);
        }
    }
}
