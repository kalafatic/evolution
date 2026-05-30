package eu.kalafatic.evolution.controller.orchestration;

import eu.kalafatic.evolution.controller.orchestration.intent.IntentAnalyzer;
import eu.kalafatic.evolution.controller.orchestration.intent.IntentAnalysisResult;
import eu.kalafatic.evolution.controller.trajectory.EvaluationSignal;
import eu.kalafatic.evolution.controller.trajectory.SignalSeverity;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventBus;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;

public class IntentService {
    private final AiService aiService;

    public IntentService(AiService aiService) {
        this.aiService = aiService;
    }

    public IntentAnalysisResult analyze(String prompt, TaskContext context) throws Exception {
        IntentAnalyzer analyzer = new IntentAnalyzer(aiService);
        IntentAnalysisResult result = analyzer.analyze(prompt, context);
        context.getOrchestrationState().setIntentAnalysis(result);

        emitIntentSignal(result, context);

        return result;
    }

    private void emitIntentSignal(IntentAnalysisResult result, TaskContext context) {
        EvaluationSignal signal = new EvaluationSignal(
            "intent-analysis",
            "IntentService",
            result.getConfidenceScore(),
            1.0,
            result.getConfidenceScore() > 0.7 ? SignalSeverity.INFO : SignalSeverity.WARNING,
            "Intent confidence: " + result.getConfidenceScore()
        );

        SessionContainer session = SessionManager.getInstance().getSession(context.getSessionId());
        if (session == null) {
            throw new IllegalStateException("IntentService: session is null for sessionId: " + context.getSessionId());
        }
        RuntimeEventBus bus = session.getEventBus();

        bus.publish(new RuntimeEvent(
            RuntimeEventType.EVALUATION_SIGNAL_CREATED,
            context.getSessionId(),
            "IntentService",
            signal
        ));
    }
}
