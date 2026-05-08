package eu.kalafatic.evolution.controller.orchestration.export;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.agents.GeneralAgent;
import eu.kalafatic.evolution.controller.agents.AgentFactory;
import eu.kalafatic.evolution.controller.orchestration.util.EvolutionConstants;

/**
 * Transforms a weak user request into a strong structured prompt for ChatGPT.
 */
public class PromptOptimizer {
    private final GeneralAgent promptAgent;

    public PromptOptimizer() {
        this.promptAgent = (GeneralAgent) AgentFactory.getAgent(EvolutionConstants.AGENT_GENERAL);
    }

    public String optimize(String originalRequest, String architectureSummary, TaskContext context, AiService aiService) throws Exception {
        context.log("[EXPORT] Optimizing prompt for ChatGPT...");

        StringBuilder sb = new StringBuilder();
        sb.append("You are a Prompt Engineering expert. Your goal is to convert a user request into a high-quality, structured prompt for ChatGPT.\n\n");
        sb.append("CONTEXT:\n");
        sb.append("- Original Request: ").append(originalRequest).append("\n");
        sb.append("- System Architecture: ").append(architectureSummary).append("\n\n");
        sb.append("STRICT RULES FOR THE GENERATED PROMPT:\n");
        sb.append("1. Must include a clear goal.\n");
        sb.append("2. Must include system context summary.\n");
        sb.append("3. Must include explicit constraints (e.g., 'use only provided context').\n");
        sb.append("4. Must specify expected output format (e.g., code diff, full files).\n");
        sb.append("5. The generated prompt must be direct, professional, and optimized for reasoning in frontier models.\n\n");
        sb.append("Return ONLY the optimized prompt in Markdown format. No commentary.");

        promptAgent.setAiService(aiService);
        return promptAgent.process(sb.toString(), context, null);
    }
}
