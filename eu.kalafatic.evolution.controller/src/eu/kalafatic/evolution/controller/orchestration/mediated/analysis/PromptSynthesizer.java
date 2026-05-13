package eu.kalafatic.evolution.controller.orchestration.mediated.analysis;

import java.util.List;
import eu.kalafatic.evolution.controller.orchestration.mediated.model.TargetDescriptor;

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
}
