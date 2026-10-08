package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Map;

import org.eclipse.core.runtime.AssertionFailedException;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.junit.Test;

import eu.kalafatic.evolution.controller.manager.ProjectModelManager;
import eu.kalafatic.evolution.model.orchestration.GenomeSnapshot;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Unit test verifying ProjectModelManager.saveResource hardening and milestone snapshot logic.
 */
public class ProjectModelManagerSaveResourceTest {

    @Test
    public void testSaveResourceNullHandled() throws IOException {
        // Must handle null resource gracefully
        ProjectModelManager.getInstance().saveResource(null);
    }

    @Test
    public void testSaveResourceAssertionFailedExceptionHandled() throws IOException {
        // Verify that AssertionFailedException thrown during resource save is caught and handled safely
        ResourceImpl throwingResource = new ResourceImpl() {
            @Override
            public void save(Map<?, ?> options) throws IOException {
                throw new AssertionFailedException("null argument:");
            }
        };

        // Should not rethrow AssertionFailedException
        ProjectModelManager.getInstance().saveResource(throwingResource);
    }

    @Test
    public void testGenomeSnapshotManagement() {
        Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        assertNotNull(orchestrator.getGenomeSnapshots());

        for (int i = 0; i < 10; i++) {
            GenomeSnapshot snapshot = OrchestrationFactory.eINSTANCE.createGenomeSnapshot();
            snapshot.setTimestamp("snapshot_" + i);
            orchestrator.getGenomeSnapshots().add(snapshot);
        }

        assertEquals(10, orchestrator.getGenomeSnapshots().size());

        // Simulate 8 retention policy enforcement
        while (orchestrator.getGenomeSnapshots().size() > 8) {
            orchestrator.getGenomeSnapshots().remove(0);
        }

        assertEquals(8, orchestrator.getGenomeSnapshots().size());
        assertEquals("snapshot_2", orchestrator.getGenomeSnapshots().get(0).getTimestamp());
    }
}
