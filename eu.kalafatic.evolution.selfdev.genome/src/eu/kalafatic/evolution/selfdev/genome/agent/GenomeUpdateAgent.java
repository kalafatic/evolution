package eu.kalafatic.evolution.selfdev.genome.agent;

import java.io.File;
import eu.kalafatic.evolution.selfdev.genome.hub.SelfDevGenomeHub;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.agents.BaseAiAgent;
import org.json.JSONObject;

/**
 * Specialized agent for Genome maintenance and updates.
 */
public class GenomeUpdateAgent extends BaseAiAgent {

    public GenomeUpdateAgent(SessionContainer container) {
        super("GenomeUpdateAgent", "GENOME_UPDATE", container);
    }

    @Override
    protected String getAgentInstructions() {
        return "You are the Genome Update Agent. Your goal is to synchronize architectural knowledge.\n" +
               "1. Analyze project changes.\n" +
               "2. Identify affected documentation.\n" +
               "3. Regenerate summaries and update indexes.\n" +
               "4. Create a timestamped historical snapshot.\n" +
               "5. Generate a change report.";
    }

    public void runUpdate(File root, String projectName) {
        publishEvent("ANALYZING_CHANGES");

        // Orchestrate the update via the Hub
        SelfDevGenomeHub.getInstance().updateGenome(root, projectName, "v1.0.0");

        publishEvent("GENOME_SNAPSHOT_CREATED");
        publishEvent("CHANGE_REPORT_GENERATED");
    }

    private void publishEvent(String action) {
        if (sessionContainer != null && sessionContainer.getEventBus() != null) {
            sessionContainer.getEventBus().publish(new RuntimeEvent(
                RuntimeEventType.UI_STATE_UPDATED,
                sessionContainer.getSessionId(),
                "GenomeUpdateAgent",
                action
            ));
        }
    }
}
