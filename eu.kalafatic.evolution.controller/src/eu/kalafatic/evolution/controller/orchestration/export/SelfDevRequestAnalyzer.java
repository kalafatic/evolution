package eu.kalafatic.evolution.controller.orchestration.export;

import org.json.JSONObject;
import eu.kalafatic.evolution.controller.agents.AnalyticAgent;
import eu.kalafatic.evolution.controller.agents.AgentFactory;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.util.EvolutionConstants;

/**
 * Wraps AnalyticAgent to extract intent and scope specifically for manual export.
 */
public class SelfDevRequestAnalyzer {
    private final AnalyticAgent analyticAgent;

    public SelfDevRequestAnalyzer() {
        this.analyticAgent = (AnalyticAgent) AgentFactory.getAgent(EvolutionConstants.AGENT_ANALYTIC);
    }

    public JSONObject analyze(String prompt, TaskContext context) throws Exception {
        context.log("[EXPORT] Analyzing request for manual self-dev: " + prompt);
        JSONObject analysis = analyticAgent.analyze(prompt, context);
        analysis.put("isExportRequest", true);
        return analysis;
    }
}
