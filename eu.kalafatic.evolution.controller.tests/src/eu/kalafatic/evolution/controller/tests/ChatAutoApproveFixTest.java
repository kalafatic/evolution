package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertTrue;
import org.junit.Test;
import eu.kalafatic.evolution.controller.kernel.EvolutionProfile;
import eu.kalafatic.evolution.controller.orchestration.cognitive.CapabilityType;

public class ChatAutoApproveFixTest {

    @Test
    public void testChatCapabilityProfile() {
        // Test for various intensities
        for (int intensity = 1; intensity <= 4; intensity++) {
            EvolutionProfile profile = EvolutionProfile.create(CapabilityType.CHAT, intensity);
            assertTrue("requireUserSelection should be true for CHAT intensity " + intensity, profile.requireUserSelection());
            assertTrue("useParallelBranches should be true for CHAT intensity " + intensity, profile.useParallelBranches());
        }
    }
}
