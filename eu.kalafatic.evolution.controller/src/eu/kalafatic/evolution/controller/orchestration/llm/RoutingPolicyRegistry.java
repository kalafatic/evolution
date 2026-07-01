package eu.kalafatic.evolution.controller.orchestration.llm;

import java.util.ArrayList;
import java.util.List;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Registry for LLM routing policies.
 */
public class RoutingPolicyRegistry {
    private static final List<IRoutingPolicy> policies = new ArrayList<>();

    static {
        // LOCAL POLICY
        policies.add(new IRoutingPolicy() {
            @Override public boolean applies(Orchestrator orchestrator, TaskContext context) {
                return orchestrator.getAiMode() == AiMode.LOCAL || orchestrator.getAiMode() == AiMode.PROXY;
            }
            @Override public String handle(LlmRouter router, Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
                return router.sendLocalRequest(orchestrator, prompt, temperature, proxyUrl, context);
            }
        });

        // HYBRID POLICY
        policies.add(new IRoutingPolicy() {
            @Override public boolean applies(Orchestrator orchestrator, TaskContext context) {
                return orchestrator.getAiMode() == AiMode.HYBRID;
            }
            @Override public String handle(LlmRouter router, Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
                String augmentedPrompt = router.buildContextLocally(orchestrator, prompt, temperature, proxyUrl, context);
                String remoteResponse = router.sendRemoteRequest(orchestrator, augmentedPrompt, temperature, proxyUrl, context);
                return router.verifyResponseLocally(orchestrator, remoteResponse, temperature, proxyUrl, context);
            }
        });

        // REMOTE POLICY
        policies.add(new IRoutingPolicy() {
            @Override public boolean applies(Orchestrator orchestrator, TaskContext context) {
                return orchestrator.getAiMode() == AiMode.REMOTE;
            }
            @Override public String handle(LlmRouter router, Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
                return router.sendRemoteRequest(orchestrator, prompt, temperature, proxyUrl, context);
            }
        });
    }

    public static List<IRoutingPolicy> getPolicies() {
        return policies;
    }
}
