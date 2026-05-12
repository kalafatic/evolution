package eu.kalafatic.evolution.controller.orchestration.workspace;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Determines which semantic artifacts should be injected into the context.
 * Scores artifacts based on relevance, confidence, and decay.
 */
public class ContextResolver {
    private static final int MAX_ARTIFACTS_PER_INJECTION = 10;
    private static final double MIN_RELEVANCE_THRESHOLD = 0.3;

    public List<WorkspaceArtifact> resolveRelevantArtifacts(String currentGoal, SemanticWorkspace workspace) {
        List<WorkspaceArtifact> allArtifacts = workspace.getAllArtifacts();

        for (WorkspaceArtifact artifact : allArtifacts) {
            double score = calculateRelevance(currentGoal, artifact);
            artifact.setRelevanceScore(score);
        }

        return allArtifacts.stream()
                .filter(a -> a.getRelevanceScore() >= MIN_RELEVANCE_THRESHOLD)
                .sorted(Comparator.comparingDouble(WorkspaceArtifact::getRelevanceScore).reversed())
                .limit(MAX_ARTIFACTS_PER_INJECTION)
                .collect(Collectors.toList());
    }

    private double calculateRelevance(String currentGoal, WorkspaceArtifact artifact) {
        if (currentGoal == null || artifact.getContent() == null) {
            return 0.0;
        }

        double score = 0.0;
        String goalLower = currentGoal.toLowerCase();
        String contentLower = artifact.getContent().toLowerCase();

        // 1. Keyword matching (simple)
        if (contentLower.contains(goalLower)) {
            score += 0.5;
        }

        // 2. Tag matching
        for (String tag : artifact.getSemanticTags()) {
            if (goalLower.contains(tag.toLowerCase())) {
                score += 0.3;
            }
        }

        // 3. Weight by confidence and decay
        score *= artifact.getConfidence();
        score *= artifact.getDecayScore();

        // 4. Boost by type
        if ("architecture-summary".equals(artifact.getArtifactType())) {
            score += 0.2;
        } else if ("implementation-decision".equals(artifact.getArtifactType())) {
            score += 0.1;
        }

        return Math.min(1.0, score);
    }

    public String formatArtifactsForPrompt(List<WorkspaceArtifact> artifacts) {
        if (artifacts.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("### SEMANTIC WORKSPACE CONTEXT\n");
        sb.append("The following reasoning artifacts have been retrieved from persistent memory:\n\n");

        for (WorkspaceArtifact artifact : artifacts) {
            sb.append("- [").append(artifact.getArtifactType()).append("] ");
            if (artifact.getSourceIteration() != null) {
                sb.append("(from ").append(artifact.getSourceIteration()).append(") ");
            }
            sb.append("\n  ").append(artifact.getContent().replace("\n", "\n  ")).append("\n\n");
        }

        return sb.toString();
    }
}
