package eu.kalafatic.evolution.controller.orchestration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.File;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.model.orchestration.PromptInstructions;
import eu.kalafatic.evolution.controller.tools.FileTool;
import eu.kalafatic.evolution.controller.services.BestPracticesService;

/**
 * Builds a minimal ContextPackage from a Task.
 */
public class ContextBuilder {
    private static final int MAX_FILES = 5;
    private static final int SMALL_FILE_LIMIT = 4096;

    public static ContextPackage build(Task task, TaskContext context) {
        return build(task, context, 1, null);
    }

    public static ContextPackage build(Task task, TaskContext context, int attempt, String lastFeedback) {
        ContextPackage pkg = new ContextPackage();
        pkg.setTaskContext(context);
        pkg.setGoal(task.getGoal());
        pkg.setStep(task.getName());
        pkg.setAttempt(attempt);
        pkg.setLastFeedback(lastFeedback);

        // Architecture Context (replaces large architecture.md reading)
        ArchitectureContext arch = new ArchitectureContext();
        arch.getKeyRules().add("DO NOT rewrite or replace existing components (Orchestrator, Agents, ContextBuilder, Supervisor)");
        arch.getKeyRules().add("Align responsibilities and simplify interactions");
        arch.getKeyRules().add("Preserve all working behavior");
        arch.setCurrentFocus(task.getName());
        pkg.setArchitectureContext(arch);

        Set<String> scope = selectRelevantFiles(task, context);
        pkg.getScope().addAll(scope);

        StringBuilder codeBuilder = new StringBuilder();
        StringBuilder depBuilder = new StringBuilder();
        FileTool fileTool = new FileTool();

        for (String path : scope) {
            try {
                // Sanitize path (remove [FILE:] prefix if somehow still there)
                String cleanPath = path.replaceAll("^\\[FILE:|\\]$", "");

                String content = fileTool.execute("READ " + cleanPath, context.getProjectRoot(), context);
                codeBuilder.append("// FILE: ").append(cleanPath).append("\n");
                codeBuilder.append(extractRelevantCode(content)).append("\n\n");

                depBuilder.append(extractDependencies(cleanPath, content)).append("\n");
            } catch (Exception e) {
                context.log("ContextBuilder: Could not read file " + path + ": " + e.getMessage());
            }
        }

        pkg.setCode(codeBuilder.toString());
        pkg.setDependencies(depBuilder.toString());

        List<String> constraints = new ArrayList<>();
        constraints.add("do not change public API unless required");
        constraints.add("keep compatibility with EMF model");
        constraints.add("modify only files in scope");

        // Add task-specific description as a hint/constraint if it contains "don't" or "must"
        if (task.getDescription() != null) {
            String desc = task.getDescription().toLowerCase();
            if (desc.contains("must") || desc.contains("don't") || desc.contains("do not") || desc.contains("ensure")) {
                constraints.add("Task Hint: " + task.getDescription());
            }
        }

        pkg.setConstraints(constraints);

        return pkg;
    }

    private static Set<String> selectRelevantFiles(Task task, TaskContext context) {
        Set<String> files = new HashSet<>();

        String name = task.getName() != null ? task.getName() : "";
        String desc = task.getDescription() != null ? task.getDescription() : "";
        String goal = task.getGoal() != null ? task.getGoal() : "";

        // 1. Look for [FILE:path] markers in name, description, and goal
        String combined = (name + " " + desc + " " + goal);
        Pattern fileTagPattern = Pattern.compile("\\[FILE:([^\\]]+)\\]", Pattern.CASE_INSENSITIVE);
        Matcher tagMatcher = fileTagPattern.matcher(combined);
        while (tagMatcher.find()) {
            files.add(tagMatcher.group(1));
        }

        // 2. Look for potential Java file paths
        Pattern javaPathPattern = Pattern.compile("([\\w/\\\\\\.-]+\\.java)", Pattern.CASE_INSENSITIVE);
        Matcher pathMatcher = javaPathPattern.matcher(combined);
        while (pathMatcher.find()) {
            files.add(pathMatcher.group(1));
        }

        // 3. Fallback: If no files found, try to infer from task name (e.g., "Update ContextBuilder")
        if (files.isEmpty() && task.getName() != null) {
             // Simple heuristic: last word if it looks like a class
             String[] words = name.split("\\s+");
             if (words.length > 0) {
                 String lastWord = words[words.length - 1];
                 if (Character.isUpperCase(lastWord.charAt(0)) || lastWord.endsWith(".java")) {
                     // We don't have the full path here, so this is risky without a search tool
                     // For now, let's stick to explicit paths or well-formed strings
                 }
             }
        }

        // Limit scope to avoid prompt explosion
        if (files.size() > MAX_FILES) {
            List<String> limited = new ArrayList<>(files).subList(0, MAX_FILES);
            return new HashSet<>(limited);
        }
        return files;
    }

    private static String extractRelevantCode(String content) {
        if (content.length() <= SMALL_FILE_LIMIT) {
            return content;
        }

        String[] lines = content.split("\n");
        StringBuilder sb = new StringBuilder();
        int count = 0;
        boolean inInterestingBlock = false;

        for (String line : lines) {
            count++;
            // Include class/interface/enum definitions and first 100 lines
            if (count < 100 || line.contains("class ") || line.contains("interface ") || line.contains("enum ")) {
                sb.append(line).append("\n");
                continue;
            }

            // Include public methods (very simple heuristic)
            if (line.trim().startsWith("public ") && line.contains("(")) {
                sb.append(line).append("\n");
                inInterestingBlock = true;
                continue;
            }

            if (inInterestingBlock) {
                sb.append(line).append("\n");
                if (line.contains("}")) {
                    inInterestingBlock = false;
                }
            }

            if (count > 500) break; // Hard limit for safety
        }

        if (count > 100) {
            sb.append("\n// ... [TRUNCATED] ...\n");
        }

        return sb.toString();
    }

    private static String extractDependencies(String path, String content) {
        StringBuilder deps = new StringBuilder("- File: " + path + "\n");
        Pattern importPattern = Pattern.compile("^import\\s+([\\w\\.]+);", Pattern.MULTILINE);
        Matcher matcher = importPattern.matcher(content);
        int count = 0;
        while (matcher.find() && count < 10) {
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
        if (ctx.getAttempt() > 1) {
            sb.append(" (Attempt ").append(ctx.getAttempt()).append(")");
        }
        sb.append("\n\n");

        if (ctx.getLastFeedback() != null && !ctx.getLastFeedback().isEmpty()) {
            sb.append("### PREVIOUS FEEDBACK\n").append(ctx.getLastFeedback()).append("\n\n");
        }

        sb.append("### CONSTRAINTS\n");
        for (String c : ctx.getConstraints()) {
            sb.append("- ").append(c).append("\n");
        }

        sb.append("\n### DEPENDENCIES\n").append(ctx.getDependencies()).append("\n");

        sb.append("### RELEVANT CODE\n");
        sb.append("```java\n").append(ctx.getCode()).append("\n```\n\n");

        // Best Practices Injection (Kernel-centralized enrichment)
        if (ctx.getTaskContext() != null && ctx.getTaskContext().getOrchestrator() != null && ctx.getTaskContext().getOrchestrator().getAiChat() != null) {
            PromptInstructions pi = ctx.getTaskContext().getOrchestrator().getAiChat().getPromptInstructions();
            if (pi != null && (pi.isIterativeMode() || pi.isSelfIterativeMode())) {
                BestPracticesService bp = new BestPracticesService(ctx.getTaskContext().getOrchestrator(), ctx.getTaskContext().getProjectRoot() != null ? ctx.getTaskContext().getProjectRoot() : new File("."));
                sb.append("### BEST PRACTICES\n");
                if (pi.isIterativeMode()) {
                    sb.append("--- ITERATIVE LOOP CONTEXT ---\n");
                    sb.append(bp.getSpecialContext("iterative_loop.md")).append("\n");
                }
                if (pi.isSelfIterativeMode()) {
                    sb.append("--- SELF DEVELOPMENT CONTEXT ---\n");
                    sb.append(bp.getSpecialContext("self_development.md")).append("\n");
                }
                sb.append("\n");
            }
        }

        sb.append("### INSTRUCTION\n");
        sb.append("Based on the context above, perform the task described in 'CURRENT STEP'.\n");
        sb.append("Return ONLY the modified code or a diff that can be applied to the relevant files.\n");
        sb.append("Keep the implementation minimal and robust.\n");

        return sb.toString();
    }

    /**
     * Builds a prompt for strategic agents (Analytic, Planner) including best practices.
     */
    public static String buildStrategicPrompt(String role, String instructions, String footer, String request, TaskContext context, String lastFeedback) {
        StringBuilder sb = new StringBuilder();
        sb.append("Role: ").append(role).append("\n");
        if (context.getProjectRoot() != null) {
            sb.append("PROJECT ROOT: ").append(context.getProjectRoot().getAbsolutePath()).append("\n\n");
        }

        sb.append("INSTRUCTIONS:\n").append(instructions).append("\n\n");

        // Best Practices Injection
        if (context.getOrchestrator() != null && context.getOrchestrator().getAiChat() != null) {
            PromptInstructions pi = context.getOrchestrator().getAiChat().getPromptInstructions();
            if (pi != null && (pi.isIterativeMode() || pi.isSelfIterativeMode())) {
                BestPracticesService bp = new BestPracticesService(context.getOrchestrator(), context.getProjectRoot() != null ? context.getProjectRoot() : new File("."));
                sb.append("### BEST PRACTICES\n");
                if (pi.isIterativeMode()) {
                    sb.append("--- ITERATIVE LOOP CONTEXT ---\n");
                    sb.append(bp.getSpecialContext("iterative_loop.md")).append("\n");
                }
                if (pi.isSelfIterativeMode()) {
                    sb.append("--- SELF DEVELOPMENT CONTEXT ---\n");
                    sb.append(bp.getSpecialContext("self_development.md")).append("\n");
                }
                sb.append("\n");
            }
        }

        if (lastFeedback != null && !lastFeedback.isEmpty()) {
            sb.append("### PREVIOUS FEEDBACK (FAILURE RECOVERY)\n").append(lastFeedback).append("\n\n");
        }

        sb.append("CURRENT TASK:\n").append(request).append("\n\n");

        if (footer != null && !footer.isEmpty()) {
            sb.append("FINAL DIRECTIVE:\n").append(footer);
        }

        return sb.toString();
    }
}
