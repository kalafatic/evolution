package eu.kalafatic.evolution.controller.orchestration.workspace;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventBus;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;

/**
 * Determines which semantic artifacts should be injected into the context.
 * Scores artifacts based on relevance, confidence, and decay.
 */
public class ContextResolver {
    private static final int MAX_ARTIFACTS_PER_INJECTION = 10;
    private static final double MIN_RELEVANCE_THRESHOLD = 0.3;
    private String sessionId = "GLOBAL";

    public ContextResolver() {}

    public ContextResolver(String sessionId) {
        this.sessionId = sessionId;
    }

    public List<WorkspaceArtifact> resolveRelevantArtifacts(String currentGoal, SemanticWorkspace workspace) {
        List<WorkspaceArtifact> allArtifacts = workspace.getAllArtifacts();

        for (WorkspaceArtifact artifact : allArtifacts) {
            double score = calculateRelevance(currentGoal, artifact);
            artifact.setRelevanceScore(score);
        }

        List<WorkspaceArtifact> relevant = allArtifacts.stream()
                .filter(a -> a.getRelevanceScore() >= MIN_RELEVANCE_THRESHOLD)
                .sorted(Comparator.comparingDouble(WorkspaceArtifact::getRelevanceScore).reversed())
                .limit(MAX_ARTIFACTS_PER_INJECTION)
                .collect(Collectors.toList());

        SessionContainer session = SessionManager.getInstance().getSession(sessionId);
        if (session == null) {
            throw new IllegalStateException("ContextResolver: session is null for sessionId: " + sessionId);
        }
        RuntimeEventBus bus = session.getEventBus();

        if (relevant.size() > MAX_ARTIFACTS_PER_INJECTION * 0.8) {
            bus.publish(new RuntimeEvent(
                RuntimeEventType.CONTEXT_OVERLOAD_DETECTED,
                sessionId,
                "ContextResolver",
                "High density of relevant artifacts (" + relevant.size() + ") detected."));
        }

        if (!relevant.isEmpty()) {
            bus.publish(new RuntimeEvent(
                RuntimeEventType.CONTEXT_RETRIEVED,
                sessionId,
                "ContextResolver",
                "Retrieved " + relevant.size() + " semantic artifacts."));
        }

        return relevant;
    }

    private double calculateRelevance(String currentGoal, WorkspaceArtifact artifact) {
        if (currentGoal == null || artifact.getContent() == null) {
            return 0.0;
        }

        double score = 0.0;
        String goalLower = currentGoal.toLowerCase();
        String contentLower = artifact.getContent().toLowerCase();

        if (contentLower.contains(goalLower)) {
            score += 0.5;
        }

        for (String tag : artifact.getSemanticTags()) {
            if (goalLower.contains(tag.toLowerCase())) {
                score += 0.3;
            }
        }

        for (String tag : artifact.getSemanticTags()) {
            if (goalLower.equals(tag.toLowerCase())) {
                score += 0.4;
            }
        }

        score *= artifact.getConfidence();
        score *= artifact.getDecayScore();

        if ("architecture-summary".equals(artifact.getArtifactType())) {
            score += 0.2;
        } else if ("implementation-decision".equals(artifact.getArtifactType())) {
            score += 0.15;
        } else if ("clarification-conclusion".equals(artifact.getArtifactType())) {
            score += 0.2;
        } else if ("failure-cause".equals(artifact.getArtifactType())) {
            score += 0.25;
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
