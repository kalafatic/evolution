package eu.kalafatic.evolution.controller.orchestration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.Instant;
import java.io.File;

import eu.kalafatic.evolution.controller.supervision.DecisionSnapshot;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.controller.orchestration.util.EvolutionConstants;

/**
 * Assembler responsible for transforming orchestration state into a consistent user-facing result.
 */
public class FinalResponseAssembler {

    public FinalResponse assemble(TaskContext context, String summary, boolean success, Instant startTime) {
        OrchestrationState state = context.getOrchestrationState();

        // 1. Collect proposals/thoughts
        List<String> proposals = collectProposals(context);

        // 2. Track files
        List<FileReference> files = collectFiles(context);

        // 3. Execution metrics
        ExecutionMetrics metrics = null;
        if (startTime != null) {
            metrics = new ExecutionMetrics(startTime, Instant.now());
        }

        // 4. Selected variant
        String selectedVariantId = null;
        DecisionSnapshot snapshot = (DecisionSnapshot) state.getMetadata().get("lastDecisionSnapshot");
        if (snapshot != null) {
            selectedVariantId = snapshot.getSelectedVariantId();
        }

        String executionStatus = buildExecutionStatus(context, success);
        String accomplishments = buildAccomplishments(context);

        // Standardized Summary Construction
        StringBuilder sb = new StringBuilder();

        // A. Status & Accomplishments
        if (executionStatus != null && !executionStatus.isEmpty()) {
            sb.append(executionStatus).append(" ");
        }
        if (accomplishments != null && !accomplishments.isEmpty()) {
            sb.append(accomplishments).append(" ");
        }
        if (summary != null && !summary.isEmpty() && !sb.toString().contains(summary)) {
            sb.append(summary);
        }

        // B. Mediated Context Injection (if applicable)
        Object mediatedTarget = state.getMetadata().get("mediatedTarget");
        if (mediatedTarget instanceof eu.kalafatic.evolution.controller.mediation.model.TargetDescriptor) {
            eu.kalafatic.evolution.controller.mediation.model.TargetDescriptor target = (eu.kalafatic.evolution.controller.mediation.model.TargetDescriptor) mediatedTarget;
            sb.append("\n\n---\n### 🧠 Mediated Target Intelligence\n");
            sb.append("**Target Path:** `").append(target.getRootPath()).append("`\n");
            sb.append("**Detected Technologies:** ").append(target.getDetectedTechnologies().stream().sorted().collect(Collectors.joining(", "))).append("\n");
            sb.append("**Inferred Architecture:** ").append(target.getArchitectureInference()).append("\n");

            Object curated = state.getMetadata().get("mediatedCuratedFiles");
            if (curated instanceof List) {
                List<?> curatedList = (List<?>) curated;
                sb.append("**Curated Context:** ").append(curatedList.size()).append(" high-signal files selected for synthesis.\n");
            }

            Object synthesized = state.getMetadata().get("mediatedSynthesizedPrompt");
            if (synthesized instanceof String && !((String) synthesized).isEmpty()) {
                sb.append("\n#### 📝 Proposed Evolution Prompt\n```markdown\n").append(synthesized).append("\n```\n");
            }
        }

        String finalSummary = sb.toString().trim();

        return new FinalResponse(
            finalSummary,
            proposals,
            files,
            success,
            selectedVariantId,
            executionStatus,
            metrics
        );
    }

    private List<String> collectProposals(TaskContext context) {
        List<String> proposals = new ArrayList<>();
        OrchestrationState state = context.getOrchestrationState();

        // From decision snapshot (selected branch explanation)
        DecisionSnapshot snapshot = (DecisionSnapshot) state.getMetadata().get("lastDecisionSnapshot");
        if (snapshot != null && snapshot.getActivationReason() != null) {
            String reason = snapshot.getActivationReason();
            if (!reason.equalsIgnoreCase("Highest Score") && !reason.equalsIgnoreCase("No high-quality variant found.")) {
                proposals.add("Evolutionary Strategy: " + reason);
            }
        }

        // From intent expansion (Ambiguities/Dimensions)
        Object expansion = state.getMetadata().get("intentExpansion");
        if (expansion instanceof eu.kalafatic.evolution.controller.orchestration.intent.IntentExpansionResult) {
            eu.kalafatic.evolution.controller.orchestration.intent.IntentExpansionResult exp = (eu.kalafatic.evolution.controller.orchestration.intent.IntentExpansionResult) expansion;
            if (exp.getDimensions() != null) {
                for (eu.kalafatic.evolution.controller.orchestration.intent.IntentDimension dim : exp.getDimensions()) {
                    if (dim.getAmbiguityScore() > 0.5) {
                        proposals.add("Ambiguity detected in " + dim.getName() + ": " + dim.getRationale());
                    }
                }
            }
        }

        return proposals.stream().distinct().sorted().collect(Collectors.toList());
    }

    private List<FileReference> collectFiles(TaskContext context) {
        List<FileReference> refs = new ArrayList<>();
        Map<String, FileChangeTracker.ChangeType> changes = context.getFileChangeTracker().getChangedFiles();

        File projectRoot = context.getProjectRoot();

        for (Map.Entry<String, FileChangeTracker.ChangeType> entry : changes.entrySet()) {
            String relativePath = entry.getKey();
            String displayName = new File(relativePath).getName();

            // Generate Eclipse-compatible URI
            String absolutePath = new File(projectRoot, relativePath).getAbsolutePath().replace('\\', '/');
            if (!absolutePath.startsWith("/")) {
                absolutePath = "/" + absolutePath;
            }
            String uri = "file://" + absolutePath;

            refs.add(new FileReference(relativePath, displayName, uri));
        }

        return refs;
    }

    private String buildExecutionStatus(TaskContext context, boolean success) {
        if (!success) return "Execution failed.";

        List<Task> tasks = context.getOrchestrator().getTasks();
        if (tasks.isEmpty()) return "Execution completed.";

        long completedTasks = tasks.stream()
            .filter(t -> t.getStatus() == eu.kalafatic.evolution.model.orchestration.TaskStatus.DONE)
            .count();

        return "I have completed " + completedTasks + " of " + tasks.size() + " tasks.";
    }

    private String buildAccomplishments(TaskContext context) {
        List<Task> tasks = context.getOrchestrator().getTasks();
        return tasks.stream()
            .filter(t -> t.getResultSummary() != null && !t.getResultSummary().isEmpty())
            .map(Task::getResultSummary)
            .distinct()
            .sorted()
            .collect(Collectors.joining(" "));
    }
}
