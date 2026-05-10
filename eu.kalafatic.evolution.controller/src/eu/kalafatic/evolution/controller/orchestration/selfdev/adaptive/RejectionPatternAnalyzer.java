package eu.kalafatic.evolution.controller.orchestration.selfdev.adaptive;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.agents.BaseAiAgent;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationRecord;
import eu.kalafatic.evolution.controller.parsers.JsonUtils;

/**
 * Analyzes rejected variants and iterations to extract failure patterns
 * and generate guidance for the next mutation cycle.
 */
public class RejectionPatternAnalyzer extends BaseAiAgent {

    public RejectionPatternAnalyzer() {
        super("RejectionPatternAnalyzer", "RejectionAnalyzer");
    }

    @Override
    protected String getAgentInstructions() {
        return "Role: Rejection Pattern Analyzer. Goal: Analyze failed/rejected iterations and variants to extract actionable guidance.\n\n" +
               "Input will be a list of Iteration Records (including rejected variants if available).\n" +
               "Identify recurring themes in user rejections and technical failures.\n\n" +
               "OUTPUT SCHEMA (JSON):\n" +
               "{\n" +
               "  \"failurePatterns\": [\"too complex\", \"ignored constraint X\", ...],\n" +
               "  \"avoidGuidelines\": [\"Reduce abstraction layers\", \"Prioritize single-module setup\", ...],\n" +
               "  \"preferGuidelines\": [\"Direct implementation\", \"Minimal architecture\", ...],\n" +
               "  \"diversityDirectives\": [\"Try functional approach\", \"Switch framework Y to Z\", ...],\n" +
               "  \"rejectionSeverity\": 0.0-1.0\n" +
               "}";
    }

    public JSONObject analyze(List<IterationRecord> history, TaskContext context) throws Exception {
        if (history == null || history.isEmpty()) return null;

        List<IterationRecord> failures = history.stream()
                .filter(r -> "FAIL".equals(r.getResult()) || "REJECTED".equals(r.getStatus()))
                .collect(Collectors.toList());

        if (failures.isEmpty()) return null;

        StringBuilder sb = new StringBuilder();
        sb.append("Analysis of ").append(failures.size()).append(" failed/rejected iterations:\n");
        for (IterationRecord r : failures) {
            sb.append("- Iteration ").append(r.getIteration()).append(": ")
              .append(r.getStrategy()).append(" -> ")
              .append(r.getErrorMessage()).append("\n");
        }

        String prompt = buildPrompt(sb.toString(), context, null);
        String response = aiService.sendRequest(context.getOrchestrator(), prompt, context);

        return JsonUtils.extractJsonObject(response);
    }
}
