package eu.kalafatic.evolution.controller.mediation.analysis;

import java.util.List;

import eu.kalafatic.evolution.controller.mediation.model.ArchitecturalFact;
import eu.kalafatic.evolution.controller.mediation.model.SemanticNode;
import eu.kalafatic.evolution.controller.mediation.model.Subsystem;
import eu.kalafatic.evolution.controller.mediation.model.TargetDescriptor;
import eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel;
import eu.kalafatic.evolution.controller.mediation.model.TargetSnapshot;

/**
 * Synthesizes architecturally informed prompts for external LLMs.
 */
public class PromptSynthesizer {

    public String synthesize(String originalRequest, TargetDescriptor target, List<String> curatedFiles) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ARCHITECTURALLY INFORMED PROMPT\n\n");
        sb.append("## USER REQUEST\n").append(originalRequest).append("\n\n");
        sb.append("## PROJECT ARCHITECTURE (INFERRED)\n").append(target.getArchitectureInference()).append("\n\n");
        sb.append("## DETECTED TECHNOLOGIES\n").append(String.join(", ", target.getDetectedTechnologies())).append("\n\n");

        sb.append("## HIGH-VALUE CONTEXT FILES\n");
        for (String path : curatedFiles) {
            sb.append("- ").append(path).append("\n");
        }

        sb.append("\n## INSTRUCTIONS\n");
        sb.append("Please provide a response that respects the architectural patterns identified above. ");
        sb.append("Focus on stabilization and evolutionary improvements.");

        return sb.toString();
    }

    public String synthesizeOptimized(String request, TargetSnapshot snapshot, List<String> selectedPaths, String evolvedUnderstanding) {
        return synthesizeOptimized(request, snapshot, selectedPaths, evolvedUnderstanding, null);
    }

    public String synthesizeOptimized(String request, TargetSnapshot snapshot, List<String> selectedPaths, String evolvedUnderstanding, TargetRealityModel realityModel) {
        StringBuilder sb = new StringBuilder();
        sb.append("# OPTIMIZED MEDIATED CONTEXT PROMPT\n\n");

        sb.append("## REFORMULATED REQUEST (KNOWLEDGE ACQUISITION FOCUS)\n");
        sb.append("Your primary objective is to EXPLORE and RESOLVE UNKNOWN REGIONS of the repository to satisfy the following goal:\n");
        sb.append("> ").append(request).append("\n\n");

        if (evolvedUnderstanding != null && !evolvedUnderstanding.isEmpty()) {
            sb.append("## EVOLVED COGNITIVE UNDERSTANDING\n");
            sb.append("The platform has evolved the following repository understanding through iterative Darwinian reasoning:\n");
            sb.append("> ").append(evolvedUnderstanding).append("\n\n");
        }

        sb.append("## PROJECT SNAPSHOT (Metadata-Only)\n");
        sb.append("Architecture: ").append(snapshot.getMetadata().get("architectureInference")).append("\n");
        sb.append("Technologies: ").append(snapshot.getMetadata().get("detectedTechnologies")).append("\n\n");

        if (realityModel == null) {
            realityModel = (eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel) snapshot.getMetadata().get("targetRealityModel");
        }

        if (realityModel != null) {
            sb.append("## DISCOVERED TARGET REALITY\n");
            sb.append("Domain: ").append(realityModel.getDomain()).append("\n");
            sb.append("Purpose: ").append(realityModel.getPurpose()).append("\n");
            sb.append("Architecture: ").append(realityModel.getArchitectureSummary()).append("\n\n");

            sb.append("### Identified Hotspots\n");
            for (eu.kalafatic.evolution.controller.mediation.model.Hotspot hotspot : realityModel.getHotspots()) {
                sb.append("- ").append(hotspot.getName()).append(" [").append(hotspot.getType()).append("]: ").append(hotspot.getDescription()).append("\n");
            }

            if (!realityModel.getSubsystems().isEmpty()) {
                sb.append("\n### Discovered Subsystems\n");
                for (Subsystem sub : realityModel.getSubsystems()) {
                    sb.append("- ").append(sub.getName()).append(": ").append(sub.getPurpose()).append("\n");
                }
            }

            if (!realityModel.getArchitecturalFacts().isEmpty()) {
                sb.append("\n### Discovered Architectural Facts\n");
                for (ArchitecturalFact fact : realityModel.getArchitecturalFacts()) {
                    sb.append("- ").append(fact.toString()).append("\n");
                }
            }
            sb.append("\n");
        }

        sb.append("## SELECTED CONTEXT SUMMARIES\n");
        for (String path : selectedPaths) {
            SemanticNode node = snapshot.getNodes().get(path);
            if (node != null) {
                sb.append("### File: ").append(path).append("\n");
                sb.append("- **Summary:** ").append(node.getSummary()).append("\n");
                sb.append("- **Tags:** ").append(node.getTags()).append("\n");
                if (!node.getStructures().isEmpty()) {
                    sb.append("- **Structure:** ").append(node.getStructures().size()).append(" significant units detected.\n");
                }
                sb.append("\n");
            }
        }

        sb.append("## CONSTRAINTS & PRIORITIES\n");
        sb.append("- **PRIORITY: Resolve Knowledge Gaps.** Focus your analysis on the identified hotspots and gaps listed above.\n");
        sb.append("- Respect existing architectural patterns.\n");
        sb.append("- Prioritize stability and modularity.\n");
        sb.append("- Propose structured changes.\n");

        return sb.toString();
    }
}
