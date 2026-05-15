package eu.kalafatic.evolution.controller.mediation.analysis;

import java.io.File;
import java.util.List;
import eu.kalafatic.evolution.controller.mediation.model.SemanticNode;
import eu.kalafatic.evolution.controller.mediation.model.TargetSnapshot;
import eu.kalafatic.evolution.controller.mediation.scanner.TargetScanner;
import eu.kalafatic.utils.semantic.AIContextTool;
import eu.kalafatic.utils.semantic.EvoMetadata;
import eu.kalafatic.utils.log.Log;

/**
 * Action to generate and persist AI metadata sidecars (.ai.json) for a target project.
 */
public class MetadataGenerator {

    private final AIContextTool contextTool = new AIContextTool();
    private final TargetScanner scanner = new TargetScanner();
    private final SemanticExtractor extractor = new SemanticExtractor();

    public void generate(File projectRoot) {
        Log.log("[METADATA] Generating AI Metadata for: " + projectRoot.getAbsolutePath());

        TargetSnapshot.TargetType type = projectRoot.getAbsolutePath().contains("evolution") ?
                TargetSnapshot.TargetType.SELF : TargetSnapshot.TargetType.PROJECT;

        // 1. Scan project
        TargetSnapshot snapshot = scanner.scanToSnapshot(projectRoot, type);

        // 2. Extract semantic information
        extractor.extractToSnapshot(snapshot);

        // 3. Persist as sidecars
        int count = 0;
        for (SemanticNode node : snapshot.getNodes().values()) {
            if (node.getPath().endsWith(".ai.json")) continue;

            File artifact = new File(projectRoot, node.getPath());
            if (!artifact.exists() || artifact.isDirectory()) continue;

            EvoMetadata metadata = contextTool.loadMetadata(artifact);
            if (metadata == null) {
                metadata = new EvoMetadata();
                metadata.setPath(node.getPath());
            }

            // Sync from extraction results (Authority Hierarchy: Annotation > Sidecar > Heuristic)
            String domain = node.getAttributes().get("domain");
            String role = node.getAttributes().get("role");

            if (domain != null) metadata.setDomain(domain);
            if (role != null) metadata.setRole(role);

            if ((metadata.getPurpose() == null || metadata.getPurpose().isEmpty() || metadata.getPurpose().equals("unknown"))
                && node.getSummary() != null) {
                metadata.setPurpose(node.getSummary());
            }

            contextTool.saveMetadata(artifact, metadata);
            count++;
        }

        Log.log("[METADATA] Completed. Processed " + count + " files.");
    }
}
