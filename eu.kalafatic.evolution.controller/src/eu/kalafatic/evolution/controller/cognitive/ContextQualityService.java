package eu.kalafatic.evolution.controller.cognitive;

import java.util.ArrayList;
import java.util.List;

import eu.kalafatic.evolution.controller.cognitive.model.ContextAssessment;
import eu.kalafatic.evolution.controller.cognitive.model.ContextItem;
import eu.kalafatic.evolution.controller.cognitive.model.ContextPackage;
import eu.kalafatic.evolution.controller.cognitive.model.ContextRequest;

/**
 * Produces the ContextAssessment attached to every reconstructed package:
 * completeness against requested categories, conflicts, staleness, budget
 * utilization and warnings.
 */
public class ContextQualityService {

    private static ContextQualityService instance;

    public static synchronized ContextQualityService getInstance() {
        if (instance == null) {
            instance = new ContextQualityService();
        }
        return instance;
    }

    public static synchronized void resetInstance() {
        instance = null;
    }

    public ContextAssessment assess(ContextRequest request, ContextPackage pkg,
            int candidateCount, int selectedCount) {
        ContextAssessment assessment = new ContextAssessment();
        assessment.setContextId(pkg.getContextId());

        // Budget utilization
        if (pkg.getBudgetTokens() > 0) {
            assessment.setBudgetUtilization(Math.min(2.0,
                    (double) pkg.getTokenEstimate() / (double) pkg.getBudgetTokens()));
            if (pkg.getTokenEstimate() > pkg.getBudgetTokens()) {
                assessment.getWarnings().add("token estimate " + pkg.getTokenEstimate()
                        + " exceeds budget " + pkg.getBudgetTokens());
            }
        }

        // Completeness per requested category
        int requested = 0;
        int covered = 0;
        if (request.wantsCategory(ContextReconstructionService.CATEGORY_USER)) {
            requested++;
            if (pkg.getUserContext() != null && !pkg.getUserContext().keySet().isEmpty()) covered++;
        }
        if (request.wantsCategory(ContextReconstructionService.CATEGORY_SESSION)) {
            requested++;
            if (pkg.getSessionContext() != null && !pkg.getSessionContext().keySet().isEmpty()) covered++;
        }
        if (request.wantsCategory(ContextReconstructionService.CATEGORY_PROJECT)) {
            requested++;
            if (pkg.getProjectContext() != null && !pkg.getProjectContext().keySet().isEmpty()) covered++;
        }
        if (request.wantsCategory(ContextReconstructionService.CATEGORY_MEMORY)) {
            requested++;
            if (!pkg.getMemories().isEmpty()) covered++;
        }
        if (request.wantsCategory(ContextReconstructionService.CATEGORY_DECISIONS)) {
            requested++;
            if (!pkg.getDecisions().isEmpty()) covered++;
        }
        if (request.wantsCategory(ContextReconstructionService.CATEGORY_ISSUES)) {
            requested++;
            if (!pkg.getKnownIssues().isEmpty()) covered++;
        }
        assessment.setCompleteness(requested == 0 ? 0.0 : (double) covered / (double) requested);

        // Conflicts and stale items
        List<String> conflicts = new ArrayList<>();
        List<String> staleItems = new ArrayList<>();
        for (ContextItem item : pkg.getMemories()) {
            if (item.isStale()) {
                staleItems.add(item.getId() != null ? item.getId() : "memory item");
            }
        }
        for (ContextItem item : pkg.getProcedures()) {
            if (item.isStale()) {
                staleItems.add(item.getId() != null ? item.getId() : "procedure item");
            }
        }
        for (String warning : pkg.getWarnings()) {
            if (warning != null && warning.toLowerCase().contains("conflict")) {
                conflicts.add(warning);
            }
        }
        assessment.setConflicts(conflicts);
        assessment.setStaleItems(staleItems);
        for (String warning : pkg.getWarnings()) {
            assessment.getWarnings().add(warning);
        }

        // Quality: confidence weighted by completeness and trimmings
        double trimPenalty = candidateCount > 0 ? (double) selectedCount / (double) candidateCount : 1.0;
        double quality = pkg.getConfidence() * (0.6 + 0.4 * assessment.getCompleteness()) * (0.5 + 0.5 * trimPenalty);
        if (!assessment.getConflicts().isEmpty()) {
            quality *= 0.85;
        }
        if (!assessment.getStaleItems().isEmpty()) {
            quality *= 0.9;
        }
        assessment.setQuality(Math.max(0.0, Math.min(1.0, quality)));
        return assessment;
    }
}
