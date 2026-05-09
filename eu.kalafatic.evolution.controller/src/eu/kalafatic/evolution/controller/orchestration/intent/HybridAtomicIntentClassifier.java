package eu.kalafatic.evolution.controller.orchestration.intent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;

import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.parsers.JsonUtils;

/**
 * Hybrid implementation of AtomicIntentClassifier using heuristic semantic scoring
 * and optional lightweight LLM validation.
 */
public class HybridAtomicIntentClassifier implements AtomicIntentClassifier {

    private final AiService aiService;

    public HybridAtomicIntentClassifier(AiService aiService) {
        this.aiService = aiService;
    }

    @Override
    public AtomicIntentAnalysis analyze(String request, TaskContext context) {
        AtomicIntentAnalysis analysis = heuristicAnalyze(request);

        // If confidence is in the "uncertain" zone (e.g., 0.4 to 0.8), use LLM for verification
        if (analysis.getConfidence() > 0.4 && analysis.getConfidence() <= 0.8 && aiService != null && context != null) {
            context.log("[KERNEL] AtomicIntentClassifier heuristic confidence intermediate (" + analysis.getConfidence() + "). Invoking LLM validation...");
            return llmValidate(request, context, analysis);
        }

        context.log("[KERNEL] AtomicIntentClassifier score=" + String.format("%.2f", analysis.getConfidence()));
        context.log("[KERNEL] Signals=" + analysis.getSignals());
        context.log("[KERNEL] requiresPlanning=" + analysis.isRequiresPlanning());
        context.log("[KERNEL] LLM validation skipped");

        return analysis;
    }

    /**
     * Performs weighted heuristic scoring based on semantic signals.
     */
    public static AtomicIntentAnalysis heuristicAnalyze(String request) {
        AtomicIntentAnalysis analysis = new AtomicIntentAnalysis();
        if (request == null || request.trim().isEmpty()) {
            analysis.setConfidence(0.0);
            return analysis;
        }

        String lower = request.toLowerCase().trim();
        double score = 0.5; // Starting neutral score

        // POSITIVE SIGNALS
        List<String> posVerbs = Arrays.asList("create", "generate", "add", "write", "make", "save");
        List<String> posArtifacts = Arrays.asList("class", "interface", "file", "resource", "record", "enum", "entity", "controller", "service", "readme");

        for (String verb : posVerbs) {
            if (lower.startsWith(verb)) {
                score += 0.15;
                analysis.getSignals().add("deterministic_verb");
                break;
            }
        }

        for (String art : posArtifacts) {
            if (lower.contains(" " + art) || lower.contains(art + " ")) {
                score += 0.1;
                analysis.getSignals().add("artifact_terminology");
                break;
            }
        }

        // Detect potential target (simplistic: last word if it looks like an identifier)
        Pattern targetPattern = Pattern.compile("\\b([A-Z][a-zA-Z0-9_]*|[a-z0-9_-]+\\.[a-z0-9]+)\\b");
        Matcher m = targetPattern.matcher(request);
        int targetCount = 0;
        while (m.find()) {
            targetCount++;
            analysis.getExtractedTargets().add(m.group());
        }

        if (targetCount == 1) {
            String target = analysis.getExtractedTargets().get(0);
            boolean isVerb = false;
            for (String verb : posVerbs) {
                if (verb.equalsIgnoreCase(target)) {
                    isVerb = true;
                    break;
                }
            }
            if (!isVerb) {
                score += 0.15;
                analysis.getSignals().add("single_artifact");
                analysis.setTargetArtifact(target);
            } else {
                targetCount = 0;
                analysis.getExtractedTargets().clear();
            }
        }

        if (targetCount > 1) {
            score -= 0.1;
            analysis.getSignals().add("multiple_targets");
            analysis.setMultiStep(true);
        }

        // NEGATIVE SIGNALS
        List<String> negWording = Arrays.asList("refactor", "redesign", "optimize", "improve", "architecture", "workflow", "system-wide", "autonomous", "analyze project", "entire");
        for (String neg : negWording) {
            if (lower.contains(neg)) {
                score -= 0.2;
                analysis.getSignals().add("negative_signal:" + neg);
            }
        }

        if (lower.contains(" and ") || lower.contains(",") || lower.contains(";")) {
            score -= 0.15;
            analysis.getSignals().add("potential_conjunctions");
            analysis.setMultiStep(true);
        }

        // Bounded scope check (short length often implies atomic)
        if (request.length() < 60) {
            score += 0.1;
            analysis.getSignals().add("bounded_scope");
        } else if (request.length() > 150) {
            score -= 0.15;
            analysis.getSignals().add("broad_scope");
        }

        analysis.setConfidence(Math.max(0.0, Math.min(1.0, score)));
        analysis.setAtomic(analysis.getConfidence() > 0.6);
        analysis.setDeterministic(analysis.getConfidence() > 0.7);

        boolean hasTarget = analysis.getTargetArtifact() != null && !analysis.getTargetArtifact().isEmpty();
        analysis.setRequiresPlanning(analysis.getConfidence() < 0.75 || analysis.isMultiStep() || !hasTarget);

        return analysis;
    }

    private AtomicIntentAnalysis llmValidate(String request, TaskContext context, AtomicIntentAnalysis heuristic) {
        String prompt = "Determine whether this request is a SINGLE deterministic artifact generation request.\n\n" +
                "User Request: \"" + request + "\"\n\n" +
                "Return ONLY JSON:\n" +
                "{\n" +
                "  \"atomic\": true/false,\n" +
                "  \"confidence\": 0-1,\n" +
                "  \"deterministic\": true/false,\n" +
                "  \"requiresPlanning\": true/false,\n" +
                "  \"multiStep\": true/false,\n" +
                "  \"targetArtifact\": \"...\",\n" +
                "  \"artifactType\": \"...\",\n" +
                "  \"reason\": \"...\"\n" +
                "}";

        try {
            String response = aiService.sendRequest(context.getOrchestrator(), prompt, context);
            JSONObject json = JsonUtils.extractJsonObject(response);
            if (json != null) {
                AtomicIntentAnalysis llmAnalysis = new AtomicIntentAnalysis();
                llmAnalysis.setAtomic(json.optBoolean("atomic", heuristic.isAtomic()));
                llmAnalysis.setConfidence(json.optDouble("confidence", heuristic.getConfidence()));
                llmAnalysis.setDeterministic(json.optBoolean("deterministic", heuristic.isDeterministic()));
                llmAnalysis.setRequiresPlanning(json.optBoolean("requiresPlanning", heuristic.isRequiresPlanning()));
                llmAnalysis.setMultiStep(json.optBoolean("multiStep", heuristic.isMultiStep()));
                llmAnalysis.setTargetArtifact(json.optString("targetArtifact", heuristic.getTargetArtifact()));
                llmAnalysis.setArtifactType(json.optString("artifactType", heuristic.getArtifactType()));
                llmAnalysis.setReason(json.optString("reason"));
                llmAnalysis.getSignals().addAll(heuristic.getSignals());
                llmAnalysis.getSignals().add("LLM_VALIDATED");

                context.log("[KERNEL] LLM Validation result: atomic=" + llmAnalysis.isAtomic() + ", confidence=" + llmAnalysis.getConfidence());
                return llmAnalysis;
            }
        } catch (Exception e) {
            context.log("[KERNEL] LLM Validation failed: " + e.getMessage());
        }
        return heuristic;
    }
}
