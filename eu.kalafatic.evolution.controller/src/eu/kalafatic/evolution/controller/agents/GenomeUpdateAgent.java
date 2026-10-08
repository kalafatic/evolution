package eu.kalafatic.evolution.controller.agents;

import java.io.File;

import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;
import eu.kalafatic.evolution.selfdev.genome.hub.SelfDevGenomeHub;
import eu.kalafatic.evolution.selfdev.genome.model.GenomeUpdateResult;

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

    public GenomeUpdateResult runUpdate(File root, String projectName) {
        long start = System.currentTimeMillis();
        System.out.println("UPDATE_GENOME START");
        System.out.println("Source/Repository: " + (root != null ? root.getAbsolutePath() : "NULL"));

        publishEvent("ANALYZING_CHANGES");

        GenomeUpdateResult result = SelfDevGenomeHub.getInstance().updateGenome(root, projectName, "v1.0.0");

        System.out.println("Branch: " + result.getBranch() + " | Commit: " + result.getCommitHash());
        System.out.println("Scanned: " + result.getScannedFiles() + " | Added: " + result.getNewFiles() +
                " | Modified: " + result.getChangedFiles() + " | Removed: " + result.getRemovedFiles() +
                " | Unchanged: " + result.getUnchangedFiles());

        if (result.isHasChanges()) {
            System.out.println("Updated Documents: " + String.join(", ", result.getUpdatedDocuments()));
            System.out.println("Historical Snapshot: " + result.getHistoricalSnapshotPath());
            System.out.println("Analytical Snapshot: " + result.getAnalyticalSnapshotPath());
        } else {
            System.out.println("No relevant source changes detected. Genome documents preserved without diff noise.");
        }

        publishEvent("GENOME_SNAPSHOT_CREATED");
        publishEvent("CHANGE_REPORT_GENERATED");

        System.out.println("UPDATE_GENOME COMPLETE (" + result.getElapsedTimeMs() + " ms)");
        return result;
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
