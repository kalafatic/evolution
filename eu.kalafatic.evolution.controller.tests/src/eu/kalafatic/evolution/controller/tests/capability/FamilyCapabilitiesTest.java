package eu.kalafatic.evolution.controller.tests.capability;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityContext;
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityHealth;
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityStatus;
import eu.kalafatic.evolution.controller.orchestration.capability.DCodeFamilyCapability;
import eu.kalafatic.evolution.controller.orchestration.capability.EnrFamilyCapability;

/**
 * Family capability registration tests (Phase 6, WP-6.5/WP-6.8). DCode and ENR must be
 * discoverable through the existing Darwin CapabilityRegistry as default family
 * components.
 */
public class FamilyCapabilitiesTest {

    @Test
    public void dcodeCapabilityContract() throws Exception {
        DCodeFamilyCapability capability = new DCodeFamilyCapability();
        assertEquals("family.dcode.harness", capability.getCapabilityId());
        assertEquals("1.0", capability.getVersion());
        List<String> contracts = capability.getSupportedContracts();
        assertTrue(contracts.contains("harness.delegation"));
        assertTrue(contracts.contains("harness.discovery"));

        capability.initialize(new CapabilityContext());
        assertEquals(CapabilityStatus.INITIALIZED, capability.getStatus());
        capability.start();
        assertEquals(CapabilityStatus.STARTED, capability.getStatus());
        capability.stop();
        assertEquals(CapabilityStatus.STOPPED, capability.getStatus());

        CapabilityHealth health = capability.getHealth();
        assertTrue(health.getStabilityScore() >= 0.0 && health.getStabilityScore() <= 1.0);
        assertTrue(!health.getStatusMessage().isBlank());
    }

    @Test
    public void enrCapabilityContract() throws Exception {
        EnrFamilyCapability capability = new EnrFamilyCapability();
        assertEquals("family.enr.inference", capability.getCapabilityId());
        List<String> contracts = capability.getSupportedContracts();
        assertTrue(contracts.contains("inference.generation"));
        assertTrue(contracts.contains("inference.streaming"));
        assertTrue(contracts.contains("inference.model.lifecycle"));

        capability.initialize(new CapabilityContext());
        capability.start();
        assertEquals(CapabilityStatus.STARTED, capability.getStatus());

        CapabilityHealth health = capability.getHealth();
        assertTrue(health.getStabilityScore() >= 0.0 && health.getStabilityScore() <= 1.0);
    }
}
