package eu.kalafatic.evolution.controller.orchestration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import eu.kalafatic.evolution.controller.orchestration.attachments.AttachmentInjector;
import eu.kalafatic.evolution.controller.orchestration.workspace.ContextResolver;
import eu.kalafatic.evolution.controller.orchestration.workspace.WorkspaceArtifact;
import eu.kalafatic.evolution.controller.tools.FileTool;
import eu.kalafatic.evolution.model.orchestration.Task;

/**
 * Builds a deterministic ContextPackage from a Task using a staged pipeline.
 */
public class ContextBuilder {
    private static final int MAX_FILES = 5;
    private static final int SMALL_FILE_LIMIT = 4096;

    public static ContextPackage build(Task task, TaskContext context) {
        return build(task, context, 1, null);
    }

    public static ContextPackage build(Task task, TaskContext context, int attempt, String lastFeedback) {
        // Pipeline: RAW_INPUT → ANALYSIS → ENRICHMENT → FILTERING → COMPRESSION → ASSEMBLY

        // 1. RAW_INPUT & INITIALIZATION
        ContextPackage pkg = new ContextPackage();
        pkg.setGoal(task.getGoal());
        pkg.setStep(task.getName());
        pkg.setAttempt(attempt);
        pkg.setLastFeedback(lastFeedback);

        OrchestrationState state = context.getOrchestrationState();
        state.addDiagnostic("ContextBuilder: Starting pipeline for " + task.getName());

        // 2. ANALYSIS (File selection)
        Set<String> scope = selectRelevantFiles(task, context);
        pkg.getScope().addAll(scope);

        // 2b. ARCHITECTURAL EXPANSION
        expandArchitecturalContext(scope, context);
        pkg.getScope().addAll(scope); // Add expanded files back to scope
        state.addDiagnostic("ContextBuilder: Selected " + scope.size() + " files for scope after architectural expansion.");

        // 3. ENRICHMENT (Reading content, dependencies, and attachments)
        StringBuilder codeBuilder = new StringBuilder();
        StringBuilder depBuilder = new StringBuilder();
        FileTool fileTool = new FileTool();

        for (String path : scope) {
            try {
                String cleanPath = path.replaceAll("^\\[FILE:|\\]$", "");
                String content = fileTool.execute("READ " + cleanPath, context.getProjectRoot(), context);

                // 4. COMPRESSION (Extracting relevant code)
                codeBuilder.append("// FILE: ").append(cleanPath).append("\n");
                codeBuilder.append(extractRelevantCode(content)).append("\n\n");

                depBuilder.append(extractDependencies(cleanPath, content)).append("\n");
            } catch (Exception e) {
                context.log("ContextBuilder: Could not read file " + path + ": " + e.getMessage());
            }
        }

        pkg.setCode(codeBuilder.toString());
        pkg.setDependencies(depBuilder.toString());

        // 4b. SEMANTIC WORKSPACE INJECTION (Trajectory & Hypothesis Aware)
        ContextResolver resolver = new ContextResolver();

        // Enhance goal with trajectory and hypothesis metadata for better semantic retrieval
        String semanticGoal = pkg.getGoal();
        String currentPhase = context.getOrchestrationState().getCurrentPhase();
        if (currentPhase != null) {
            semanticGoal += " Phase: " + currentPhase;
        }

        List<WorkspaceArtifact> artifacts = resolver.resolveRelevantArtifacts(semanticGoal, context.getSemanticWorkspace());

        // Filter artifacts based on current trajectory stability
        double stability = context.getSemanticWorkspace().getTrajectoryMemory().getSuccessfulStrategies().size() > 0 ? 0.8 : 0.4;
        List<WorkspaceArtifact> filteredArtifacts = artifacts.stream()
                .filter(a -> a.getConfidence() > (1.0 - stability))
                .collect(Collectors.toList());

        String workspacePrompt = resolver.formatArtifactsForPrompt(filteredArtifacts);
        if (!workspacePrompt.isEmpty()) {
            pkg.setAttachmentContext((pkg.getAttachmentContext() != null ? pkg.getAttachmentContext() : "") + "\n" + workspacePrompt);
            state.addDiagnostic("ContextBuilder: Injected " + filteredArtifacts.size() + " adaptive semantic artifacts.");
        }

        // 5. FILTERING (Constraints)
        List<String> constraints = new ArrayList<>();
        constraints.add("do not change public API unless required");
        constraints.add("keep compatibility with EMF model");
        constraints.add("modify only files in scope");

        if (task.getDescription() != null) {
            String desc = task.getDescription().toLowerCase();
            if (desc.contains("must") || desc.contains("don't") || desc.contains("do not") || desc.contains("ensure")) {
                constraints.add("Task Hint: " + task.getDescription());
            }
        }
        pkg.setConstraints(constraints);

        // Enrichment: Authoritative Attachment Injection
        if (context.getInstructionFiles() != null && !context.getInstructionFiles().isEmpty()) {
            String combined = (task.getName() != null ? task.getName() : "") + " " +
                             (task.getDescription() != null ? task.getDescription() : "") + " " +
                             (task.getGoal() != null ? task.getGoal() : "");
            String attachmentContext = AttachmentInjector.inject(context.getInstructionFiles(), combined.trim(), context);
            pkg.setAttachmentContext(attachmentContext);
        }

        // 6. ASSEMBLY (Handled in buildPrompt)
        ArchitectureContext arch = new ArchitectureContext();
        arch.getKeyRules().add("Single Transition Authority: ONLY IterationManager may change system state");
        arch.getKeyRules().add("Stateless Execution: EvolutionOrchestrator is a blind executor");
        arch.setCurrentFocus(task.getName());
        pkg.setArchitectureContext(arch);

        state.addDiagnostic("ContextBuilder: Pipeline complete.");
        return pkg;
    }

    private static void expandArchitecturalContext(Set<String> scope, TaskContext context) {
        Set<String> expansions = new HashSet<>();
        for (String path : scope) {
            // Auto-expand if orchestration or runtime components are detected
            if (path.contains("IterationManager") || path.contains("Orchestrator") || path.contains("Supervisor") || path.contains("Kernel")) {
                context.log("ContextBuilder: Expanding orchestration context for " + path);
                expansions.add("eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/orchestration/OrchestrationState.java");
                expansions.add("eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/workflow/RuntimeEventBus.java");
                expansions.add("eu.kalafatic.evolution.model/src/eu/kalafatic/evolution/model/orchestration/Orchestrator.java");
            }
            if (path.contains("Agent")) {
                context.log("ContextBuilder: Expanding agent context for " + path);
                expansions.add("eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/agents/BaseAiAgent.java");
                expansions.add("eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/agents/AgentFactory.java");
            }
        }
        scope.addAll(expansions);
    }

    private static Set<String> selectRelevantFiles(Task task, TaskContext context) {
        Set<String> files = new HashSet<>();
        String combined = (task.getName() + " " + task.getDescription() + " " + task.getGoal());

        Pattern fileTagPattern = Pattern.compile("\\[FILE:([^]]+)\\]", Pattern.CASE_INSENSITIVE);
        Matcher tagMatcher = fileTagPattern.matcher(combined);
        while (tagMatcher.find()) {
            files.add(tagMatcher.group(1));
        }

        Pattern javaPathPattern = Pattern.compile("([\\w/\\\\\\.-]+\\.java)", Pattern.CASE_INSENSITIVE);
        Matcher pathMatcher = javaPathPattern.matcher(combined);
        while (pathMatcher.find()) {
            files.add(pathMatcher.group(1));
        }

        if (files.size() > MAX_FILES) {
            return new HashSet<>(new ArrayList<>(files).subList(0, MAX_FILES));
        }
        return files;
    }

    private static String extractRelevantCode(String content) {
        if (content.length() <= SMALL_FILE_LIMIT) return content;
        String[] lines = content.split("\n");
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (String line : lines) {
            count++;
            if (count < 100 || line.contains("class ") || line.contains("interface ") || line.contains("public ")) {
                sb.append(line).append("\n");
            }
            if (count > 500) break;
        }
        return sb.toString();
    }

    private static String extractDependencies(String path, String content) {
        StringBuilder deps = new StringBuilder("- File: " + path + "\n");
        Pattern importPattern = Pattern.compile("^import\\s+([\\w\\.]+);", Pattern.MULTILINE);
        Matcher matcher = importPattern.matcher(content);
        int count = 0;
        while (matcher.find() && count < 5) {
            deps.append("  - uses ").append(matcher.group(1)).append("\n");
            count++;
        }
        return deps.toString();
    }

    public static String buildPrompt(ContextPackage ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("### GOAL\n").append(ctx.getGoal()).append("\n\n");

        if (ctx.getArchitectureContext() != null) {
            sb.append("### ARCHITECTURE CONTEXT\n");
            sb.append("Focus: ").append(ctx.getArchitectureContext().getCurrentFocus()).append("\n");
            sb.append("Rules:\n");
            for (String rule : ctx.getArchitectureContext().getKeyRules()) {
                sb.append("- ").append(rule).append("\n");
            }
            sb.append("\n");
        }

        sb.append("### CURRENT STEP\n").append(ctx.getStep());
        if (ctx.getAttempt() > 1) sb.append(" (Attempt ").append(ctx.getAttempt()).append(")");
        sb.append("\n\n");

        if (ctx.getLastFeedback() != null && !ctx.getLastFeedback().isEmpty()) {
            sb.append("### PREVIOUS FEEDBACK\n").append(ctx.getLastFeedback()).append("\n\n");
        }

        sb.append("### CONSTRAINTS\n");
        for (String c : ctx.getConstraints()) sb.append("- ").append(c).append("\n");

        sb.append("\n### DEPENDENCIES\n").append(ctx.getDependencies()).append("\n");

        if (ctx.getAttachmentContext() != null && !ctx.getAttachmentContext().isEmpty()) {
            sb.append("### ATTACHMENTS\n").append(ctx.getAttachmentContext()).append("\n");
        }

        sb.append("### RELEVANT CODE\n```java\n").append(ctx.getCode()).append("\n```\n\n");
        sb.append("### INSTRUCTION\nPerform the task described in 'CURRENT STEP' and return ONLY the modified code or a diff.\n");

        return sb.toString();
    }
}
