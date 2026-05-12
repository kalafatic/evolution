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
 * Specialized agent for testing and validation.
 */
public class TesterAgent extends BaseAiAgent {
    public TesterAgent() {
        super("Tester", "Tester");
        addTool(new MavenTool());
        addTool(new ShellTool());
    }

    @Override
    protected String getAgentInstructions() {
        return "You are acting as a Quality Assurance and Test Engineer Agent.\n" +
               "Generate JUnit tests, or run Maven tests and analyze the output.";
    }

    public void emitTestSignal(JSONObject evaluation, String taskDescription, TaskContext context) {
        String variantId = context.getMetadata().getOrDefault("variantId", "unknown").toString();
        boolean success = evaluation.optBoolean("success", false);
        double score = success ? 1.0 : 0.0;
        SignalSeverity severity = success ? SignalSeverity.INFO : SignalSeverity.CRITICAL;
        String explanation = evaluation.optString("feedback", "Test execution finished.");

        EvaluationSignal signal = new EvaluationSignal(
            variantId,
            "TesterAgent",
            score,
            0.9, // high confidence for tests
            severity,
            explanation
        );

        RuntimeEventBus.getInstance().publish(new RuntimeEvent(
            RuntimeEventType.EVALUATION_SIGNAL_CREATED,
            context.getSessionId(),
            "TesterAgent",
            signal
        ).withMetadata("task", taskDescription));
    }
}
