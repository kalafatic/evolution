package eu.kalafatic.evolution.controller.agents;

import org.json.JSONObject;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.evolution.EvaluationSignal;
import eu.kalafatic.evolution.controller.orchestration.evolution.SignalSeverity;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventBus;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;
import eu.kalafatic.evolution.controller.tools.MavenTool;
import eu.kalafatic.evolution.controller.tools.ShellTool;

/**
 * Agent specialized in Quality Assurance and Linting.
 */
public class QualityAgent extends BaseAiAgent {
    public QualityAgent() {
        super("Quality", "Quality");
        addTool(new MavenTool());
        addTool(new ShellTool());
    }

    @Override
    protected String getAgentInstructions() {
        return "You are an AI Quality Agent. You focus on code quality, compliance, and linting.\n" +
               "Use Checkstyle, Linter tools, or Maven reports to ensure the codebase follows established standards.";
    }

    public void emitQualitySignal(JSONObject evaluation, String taskDescription, TaskContext context) {
        String variantId = context.getMetadata().getOrDefault("variantId", "unknown").toString();
        boolean success = evaluation.optBoolean("success", false);
        double score = success ? 1.0 : 0.5; // Quality failures are often not binary
        SignalSeverity severity = success ? SignalSeverity.INFO : SignalSeverity.WARNING;
        String explanation = evaluation.optString("feedback", "Quality check finished.");

        EvaluationSignal signal = new EvaluationSignal(
            variantId,
            "QualityAgent",
            score,
            0.7, // confidence
            severity,
            explanation
        );

        RuntimeEventBus.getInstance().publish(new RuntimeEvent(
            RuntimeEventType.EVALUATION_SIGNAL_CREATED,
            context.getSessionId(),
            "QualityAgent",
            signal
        ).withMetadata("task", taskDescription));
    }
}
