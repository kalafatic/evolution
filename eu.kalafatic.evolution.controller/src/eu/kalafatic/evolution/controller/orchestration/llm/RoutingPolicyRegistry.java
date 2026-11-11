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
        // COMPOSITE MODEL POLICY (Multi-model composition enabled via context or model name)
        policies.add(new IRoutingPolicy() {
            @Override public boolean applies(Orchestrator orchestrator, TaskContext context) {
                if (context != null && context.getMetadata() != null) {
                    Object compFlag = context.getMetadata().get("compositeModel");
                    if (Boolean.TRUE.equals(compFlag) || "true".equalsIgnoreCase(String.valueOf(compFlag))) {
                        return true;
                    }
                    String model = (String) context.getMetadata().get("model");
                    if (model != null && model.toLowerCase().contains("composite")) {
                        return true;
                    }
                }
                if (orchestrator != null && orchestrator.getLocalModel() != null
                        && orchestrator.getLocalModel().toLowerCase().contains("composite")) {
                    return true;
                }
                return false;
            }
            @Override public String handle(LlmRouter router, Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
                CompositeModelProvider composite = new CompositeModelProvider(router.getNativeEvoProvider(), router.getLocalProvider());
                return composite.sendRequest(orchestrator, prompt, temperature, proxyUrl, context);
            }
        });

        // NATIVE EVO POLICY (Highest priority for .evo or native models)
        policies.add(new IRoutingPolicy() {
            @Override public boolean applies(Orchestrator orchestrator, TaskContext context) {
                String model = null;
                if (context != null && context.getMetadata() != null) {
                    model = (String) context.getMetadata().get("model");
                }
                if (model == null && orchestrator != null) {
                    model = orchestrator.getLocalModel();
                    if (model == null && orchestrator.getOllama() != null) {
                        model = orchestrator.getOllama().getModel();
                    }
                }
                return LlmRouter.isNativeEvoModel(model);
            }
            @Override public String handle(LlmRouter router, Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
                return router.sendNativeEvoRequest(orchestrator, prompt, temperature, proxyUrl, context);
            }
        });

        // LOCAL POLICY
        policies.add(new IRoutingPolicy() {
            @Override public boolean applies(Orchestrator orchestrator, TaskContext context) {
                if (orchestrator == null) return true; // Default fallback to LOCAL if orchestrator is not set
                return orchestrator.getAiMode() == null || orchestrator.getAiMode() == AiMode.LOCAL || orchestrator.getAiMode() == AiMode.PROXY;
            }
            @Override public String handle(LlmRouter router, Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
                return router.sendLocalRequest(orchestrator, prompt, temperature, proxyUrl, context);
            }
        });

        // HYBRID POLICY
        policies.add(new IRoutingPolicy() {
            @Override public boolean applies(Orchestrator orchestrator, TaskContext context) {
                return orchestrator != null && orchestrator.getAiMode() == AiMode.HYBRID;
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
                return orchestrator != null && orchestrator.getAiMode() == AiMode.REMOTE;
            }
            @Override public String handle(LlmRouter router, Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
                return router.sendRemoteRequest(orchestrator, prompt, temperature, proxyUrl, context);
            }
        });

        // MEDIATED & INTENT POLICY (Local fallback)
        policies.add(new IRoutingPolicy() {
            @Override public boolean applies(Orchestrator orchestrator, TaskContext context) {
                return orchestrator != null && (orchestrator.getAiMode() == AiMode.MEDIATED || orchestrator.getAiMode() == AiMode.INTENT || orchestrator.getAiMode() == AiMode.DEVELOPER);
            }
            @Override public String handle(LlmRouter router, Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
                return router.sendLocalRequest(orchestrator, prompt, temperature, proxyUrl, context);
            }
        });
    }

    public static List<IRoutingPolicy> getPolicies() {
        return policies;
    }
}
